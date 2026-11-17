package dev.venti.modules.hand;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ColorHelper;

/** Hand Aura: glowing outline around your first-person hand and held item (ported from Anubis by 4ldenz). */
public final class HandGlowModule extends Module {
   public static volatile HandGlowModule active;
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int WHITE = -1;
   private static final float SHEEN_LIFT = 0.55F;
   private static final long BREATH_NANOS = 3200000000L;
   private static final long SHEEN_NANOS = 4400000000L;
   private final ModeSetting colorMode = this.add("color", new ModeSetting("Color", COLOR_THEME, COLOR_THEME, COLOR_MANUAL));
   private final ColorSetting glowColor = this.add("glow-color", new ColorSetting("Glow Color", -13338378));
   private final NumberSetting aura = this.add("aura", new NumberSetting("Aura", 65.0F, 0.0F, 100.0F).step(5.0F));
   private final NumberSetting reach = this.add("reach", new NumberSetting("Reach", 8.0F, 2.0F, 20.0F).step(1.0F));
   private final NumberSetting rim = this.add("rim", new NumberSetting("Rim", 40.0F, 0.0F, 100.0F).step(5.0F));
   private final NumberSetting sheen = this.add("sheen", new NumberSetting("Sheen", 25.0F, 0.0F, 100.0F).step(5.0F));
   private final ColorSetting sheenColor = this.add("sheen-color", new ColorSetting("Sheen Color", -3153665));
   private final BooleanSetting breathe = this.add("breathe", new BooleanSetting("Breathe", true));

   public HandGlowModule() {
      super("hand-glow", "Hand Glow", "Adds a glowing outline to your hand and held item.", ModuleCategory.VISUALS);
      this.glowColor.visibleWhen(this::manual);
      this.reach.visibleWhen(() -> this.aura.get() > 0.0F);
      this.sheenColor.visibleWhen(() -> this.sheen.get() > 0.0F && this.manual());
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      if (RenderSystem.isOnRenderThread()) {
         HandGlowPass.release();
      } else {
         MinecraftClient.getInstance().execute(HandGlowModule::releaseIfOff);
      }
   }

   private static void releaseIfOff() {
      if (active == null) {
         HandGlowPass.release();
      }
   }

   public static boolean beginCapture() {
      HandGlowModule module = active;
      return module != null && module.shows() && HandGlowPass.begin();
   }

   public static void endCapture(boolean drawn) {
      if (HandGlowPass.end() && drawn) {
         HandGlowModule module = active;
         float scale = Math.max(1.0F, MinecraftClient.getInstance().getFramebuffer().textureHeight / 1080.0F);
         if (module == null) {
            HandGlowPass.compose(WHITE, 0.0F, 2.0F, 0.0F, 2.0F * scale, WHITE, 0.0F, 0.0F);
         } else {
            long now = System.nanoTime();
            float phase = (float)(now % SHEEN_NANOS / 4.4E9);
            int glow = module.glowArgb();
            HandGlowPass.compose(
               glow,
               module.auraStrength(now),
               module.reach.get() * scale,
               module.rim.get() / 100.0F,
               2.0F * scale,
               module.sheenArgb(glow),
               module.sheen.get() / 100.0F,
               phase
            );
         }
      }
   }

   private boolean manual() {
      return this.colorMode.is(COLOR_MANUAL);
   }

   private int glowArgb() {
      return this.manual() ? this.glowColor.argb() : HandAccent.argb();
   }

   private int sheenArgb(int glow) {
      return this.manual() ? this.sheenColor.argb() : ColorHelper.lerp(SHEEN_LIFT, glow, WHITE);
   }

   private boolean shows() {
      return this.aura.get() > 0.0F || this.rim.get() > 0.0F;
   }

   private float auraStrength(long now) {
      float strength = this.aura.get() / 100.0F;
      if (!this.breathe.get()) {
         return strength;
      }

      double cycle = now % BREATH_NANOS / 3.2E9;
      float swell = (float)Math.sin(cycle * Math.PI);
      return strength * (0.6F + 0.4F * swell * swell);
   }
}
