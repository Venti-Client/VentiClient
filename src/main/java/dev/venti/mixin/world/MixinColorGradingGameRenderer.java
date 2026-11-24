package dev.venti.mixin.world;

import dev.venti.modules.world.ColorGradingModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinColorGradingGameRenderer {
   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"
      )}
   )
   //? if >=26.3 {
   /*private void venti$colorGrading(CallbackInfo ci) {
      if (ColorGradingModule.active != null) {
         ColorGradingModule.afterWorld(((GameRenderer)(Object)this).gameRenderState().shouldRenderLevel);
      }
   }
   *///?} else {
   private void venti$colorGrading(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
      if (ColorGradingModule.active != null) {
         ColorGradingModule.afterWorld(renderLevel);
      }
   }
   //?}
}
