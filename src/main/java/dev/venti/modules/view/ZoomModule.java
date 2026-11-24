package dev.venti.modules.view;

import dev.venti.compat.Mc;
import dev.venti.config.BooleanSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

/** Hold the zoom key (default C) to zoom in; the scroll wheel adjusts the zoom while held. */
public final class ZoomModule extends Module {
   private static final float MIN_ZOOM = 1.1F;
   private static final float MAX_ZOOM = 50.0F;
   private static ZoomModule instance;
   private static float target = 1.0F;
   private static float current = 1.0F;
   private static long lastNanos;
   private final NumberSetting zoom = this.add("zoom", new NumberSetting("Zoom", 4.0F, 1.5F, 20.0F).step(0.5F));
   private final NumberSetting smoothness = this.add("smoothness", new NumberSetting("Smoothness", 60.0F, 0.0F, 100.0F).step(5.0F));
   private final BooleanSetting scroll = this.add("scroll", new BooleanSetting("Scroll", true));
   private final BooleanSetting lowerSensitivity = this.add("lower-sensitivity", new BooleanSetting("Lower Sensitivity", true));
   private final BooleanSetting cinematic = this.add("cinematic", new BooleanSetting("Cinematic Camera", false));
   private final KeyMapping key;
   private boolean zooming;
   private boolean smoothBefore;
   private boolean smoothPushed;

   public ZoomModule() {
      super("zoom", "Zoom", "Hold the zoom key (Controls > Venti) to zoom in.", ModuleCategory.VISUALS, true);
      this.key = ViewKeys.register("key.venti.zoom", InputConstants.KEY_C);
      instance = this;
   }

   @Override
   protected void onDisable() {
      this.stopZoom();
   }

   private void startZoom() {
      this.zooming = true;
      target = this.zoom.get();
      if (this.cinematic.get()) {
         Minecraft mc = Minecraft.getInstance();
         this.smoothBefore = mc.options.smoothCamera;
         mc.options.smoothCamera = true;
         this.smoothPushed = true;
      }
   }

   private void stopZoom() {
      this.zooming = false;
      target = 1.0F;
      if (this.smoothPushed) {
         Minecraft.getInstance().options.smoothCamera = this.smoothBefore;
         this.smoothPushed = false;
      }
   }

   /** Polled every frame from the FOV hook so the zoom reacts without waiting for a tick. */
   private static void pollKey() {
      ZoomModule module = instance;
      if (module != null) {
         Minecraft mc = Minecraft.getInstance();
         boolean held = module.isEnabled() && Mc.screen() == null && module.key.isDown();
         if (held && !module.zooming) {
            module.startZoom();
         } else if (!held && module.zooming) {
            module.stopZoom();
         }
      }
   }

   public static float fov(float fov) {
      pollKey();
      float factor = advance();
      return factor > 1.0001F ? fov / factor : fov;
   }

   public static double turnScale() {
      ZoomModule module = instance;
      return module != null && module.lowerSensitivity.get() && !(current <= 1.0001F) ? 1.0 / current : 1.0;
   }

   public static boolean scroll(double amount) {
      ZoomModule module = instance;
      if (module != null && module.isEnabled() && module.zooming && module.scroll.get() && amount != 0.0) {
         float next = target * (amount > 0.0 ? 1.2F : 0.8333333F);
         target = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, next));
         return true;
      } else {
         return false;
      }
   }

   private static float advance() {
      long now = System.nanoTime();
      float seconds = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
      lastNanos = now;
      ZoomModule module = instance;
      float smooth = module == null ? 0.0F : module.smoothness.get() / 100.0F;
      if (!(smooth <= 0.0F) && current != target) {
         float rate = 30.0F + -25.0F * smooth;
         float blend = 1.0F - (float)Math.exp(-seconds * rate);
         double from = Math.log(current);
         double to = Math.log(target);
         current = (float)Math.exp(from + (to - from) * blend);
         if (Math.abs(current - target) < 0.002F) {
            current = target;
         }

         return current;
      } else {
         current = target;
         return current;
      }
   }
}
