package dev.venti.modules.view;

import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class CustomFovModule extends Module {
   public static volatile CustomFovModule active;
   private static float screenEffectScale = 1.0F;
   private final NumberSetting fov = this.add("fov", new NumberSetting("FOV", 110.0F, 30.0F, 160.0F).step(1.0F));
   private int culledFov = Integer.MIN_VALUE;

   public CustomFovModule() {
      super("custom-fov", "Custom Fov", "Changes your field of view (FOV) beyond the vanilla slider.", ModuleCategory.VISUALS);
      this.fov.onChange(this::onFovChanged);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.culledFov = Math.round(this.fov.get());
      scheduleTerrainUpdate();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.culledFov = Integer.MIN_VALUE;
      screenEffectScale = 1.0F;
      scheduleTerrainUpdate();
   }

   public float fov() {
      return Math.round(this.fov.get());
   }

   /** Remembers how much the hand/overlay FOV differs from vanilla, so screen overlays can be scaled back. */
   public static void hudFovApplied(float vanilla, float applied) {
      screenEffectScale = vanilla == applied ? 1.0F : (float)(Math.tan(Math.toRadians(applied) * 0.5) / Math.tan(Math.toRadians(vanilla) * 0.5));
   }

   public static float screenEffectScale() {
      return active == null ? 1.0F : screenEffectScale;
   }

   private void onFovChanged() {
      if (active == this) {
         int value = Math.round(this.fov.get());
         if (value != this.culledFov) {
            this.culledFov = value;
            scheduleTerrainUpdate();
         }
      }
   }

   private static void scheduleTerrainUpdate() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         if (!mc.isSameThread()) {
            mc.execute(CustomFovModule::scheduleTerrainUpdate);
         } else if (mc.level != null && mc.levelRenderer != null) {
            //? if >=26.2 {
            mc.levelRenderer.sectionOcclusionGraph().invalidate();
            mc.levelRenderer.cloudRenderer().markForRebuild();
            //?} else {
            /*mc.levelRenderer.needsUpdate();
            *///?}
         }
      }
   }
}
