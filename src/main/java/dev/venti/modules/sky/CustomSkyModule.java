package dev.venti.modules.sky;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.StringSetting;
import dev.venti.gui.Theme;
import dev.venti.gui.image.PictureFrames;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import dev.venti.modules.spotify.sky.SkyCanvas;
import dev.venti.modules.spotify.sky.SkyTexture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/** Replaces the overworld sky with a picture or animated GIF wrapped around the whole sky. */
public final class CustomSkyModule extends Module {
   private static final int MAX_SIDE = 2048;
   private static final int MAX_FRAMES = 120;
   private final StringSetting image = this.add("image", new StringSetting("Sky Image", "").files(Theme.IMAGE_TYPES));
   private final ModeSetting mapping = this.add("mapping", new ModeSetting("Mapping", "Dome", "Dome", "Full Sphere"));
   private final NumberSetting repeat = this.add("repeat", new NumberSetting("Repeat Around", 1.0F, 1.0F, 8.0F).step(1.0F));
   private final NumberSetting spin = this.add("spin", new NumberSetting("Spin Speed", 0.0F, 0.0F, 10.0F).step(0.1F));
   private final NumberSetting opacity = this.add("opacity", new NumberSetting("Opacity", 1.0F, 0.1F, 1.0F).step(0.05F));
   private final BooleanSetting darkenAtNight = this.add("darken-at-night", new BooleanSetting("Darken At Night", true));
   private final BooleanSetting hideSunMoon = this.add("hide-sun-moon", new BooleanSetting("Hide Sun And Moon", false));
   private final BooleanSetting hideStars = this.add("hide-stars", new BooleanSetting("Hide Stars", true));
   private final BooleanSetting hideSunset = this.add("hide-sunset", new BooleanSetting("Hide Sunset Glow", true));
   private final BooleanSetting gifSpeedSync = this.add("gif-speed", new BooleanSetting("Play GIF", true));
   private final List<SkyTexture> frames = new ArrayList<>();
   private final List<Integer> delays = new ArrayList<>();
   private SkyCanvas canvas;
   private String loaded = "";
   private CompletableFuture<List<PictureFrames.Frame>> loading;
   private long animationStart;
   private boolean failed;

   public CustomSkyModule() {
      super("custom-sky", "Custom Sky", "Sets the sky to any picture or animated GIF you choose.", ModuleCategory.RENDER);
      this.repeat.visibleWhen(() -> !this.image.get().isBlank());
      this.image.onChange(() -> this.failed = false);
   }

   public static CustomSkyModule active() {
      CustomSkyModule module = Modules.get(CustomSkyModule.class);
      return module != null && module.isEnabled() && !module.image.get().isBlank() ? module : null;
   }

   public boolean hideSunMoon() {
      return this.hideSunMoon.get();
   }

   public boolean hideStars() {
      return this.hideStars.get();
   }

   public boolean hideSunset() {
      return this.hideSunset.get();
   }

   @Override
   protected void onDisable() {
      this.release();
   }

   /** Called in place of the vanilla sky dome; draws nothing (and returns false) until the picture is ready. */
   public boolean render(float tickDelta) {
      SkyTexture texture = this.currentFrame();
      if (texture == null) {
         return false;
      } else {
         if (this.canvas == null) {
            this.canvas = new SkyCanvas();
         }

         float seconds = (System.nanoTime() % 3_600_000_000_000L) / 1.0E9F;
         float offset = this.spin.get() * seconds / 360.0F % 1.0F;
         float bottom = this.mapping.is("Dome") ? 0.95F : 2.22F;
         float light = this.darkenAtNight.get() ? daylight(Minecraft.getInstance().level, tickDelta) : 1.0F;
         int shade = Math.round(light * 255.0F);
         int color = 0xFF000000 | shade << 16 | shade << 8 | shade;
         this.canvas.sphere(texture, Math.round(this.repeat.get()), offset, bottom, color);
         this.canvas.flush(this.opacity.get());
         return true;
      }
   }

   private SkyTexture currentFrame() {
      String wanted = this.image.get();
      if (!wanted.equals(this.loaded)) {
         this.release();
         this.loaded = wanted;
         if (!wanted.isBlank() && !this.failed) {
            this.loading = CompletableFuture.supplyAsync(() -> {
               try {
                  return PictureFrames.load(wanted, MAX_SIDE, MAX_FRAMES);
               } catch (Exception e) {
                  throw new RuntimeException(e);
               }
            });
         }
      }

      if (this.loading != null && this.loading.isDone()) {
         try {
            List<PictureFrames.Frame> decoded = this.loading.join();
            for (int i = 0; i < decoded.size(); i++) {
               PictureFrames.Frame frame = decoded.get(i);
               this.frames.add(SkyTexture.upload("custom_sky_" + i, frame.width(), frame.height(), frame.argb()));
               this.delays.add(frame.delayMs());
            }

            this.animationStart = System.currentTimeMillis();
         } catch (RuntimeException e) {
            this.failed = true;
            LOGGER.warn("[Venti] Custom Sky could not load {}", this.loaded, e);
            this.chat(Component.literal("couldn't load " + this.loaded).withStyle(ChatFormatting.RED));
         }

         this.loading = null;
      }

      if (this.frames.isEmpty()) {
         return null;
      } else if (this.frames.size() == 1 || !this.gifSpeedSync.get()) {
         return this.frames.get(0);
      } else {
         int total = 0;
         for (int delay : this.delays) {
            total += delay;
         }

         long at = (System.currentTimeMillis() - this.animationStart) % Math.max(1, total);
         for (int i = 0; i < this.frames.size(); i++) {
            at -= this.delays.get(i);
            if (at < 0L) {
               return this.frames.get(i);
            }
         }

         return this.frames.get(this.frames.size() - 1);
      }
   }

   private void release() {
      this.frames.forEach(SkyTexture::close);
      this.frames.clear();
      this.delays.clear();
      this.loaded = "";
      this.loading = null;
   }

   /** 1 at midday, down to 0.25 at midnight, following the vanilla day cycle. */
   private static float daylight(ClientLevel world, float tickDelta) {
      if (world == null) {
         return 1.0F;
      } else {
         double day = ((world.getDayTime() % 24000L) + tickDelta) / 24000.0;
         float sun = (float)Math.cos((day - 0.25) * Math.PI * 2.0);
         return Math.max(0.25F, Math.min(1.0F, 0.6F + sun * 0.6F));
      }
   }
}
