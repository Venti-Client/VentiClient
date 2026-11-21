package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: push the lava fog out to the render distance. */
@Mixin(LavaFogEnvironment.class)
public abstract class ViewNoRenderLavaFogMixin {
   @Inject(
      method = "setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V",
      at = @At("TAIL")
   )
   private void venti$clearLavaFog(FogData fog, Camera camera, ClientLevel world, float viewDistance, DeltaTracker tickCounter, CallbackInfo ci) {
      if (NoRenderModule.hideLavaFog()) {
         fog.environmentalStart = -8.0F;
         fog.environmentalEnd = viewDistance;
         fog.skyEnd = viewDistance;
         fog.cloudEnd = viewDistance;
      }
   }
}
