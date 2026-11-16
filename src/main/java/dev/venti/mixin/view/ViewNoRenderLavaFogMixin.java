package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: push the lava fog out to the render distance. */
@Mixin(LavaFogModifier.class)
public abstract class ViewNoRenderLavaFogMixin {
   @Inject(
      method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("TAIL")
   )
   private void venti$clearLavaFog(FogData fog, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (NoRenderModule.hideLavaFog()) {
         fog.environmentalStart = -8.0F;
         fog.environmentalEnd = viewDistance;
         fog.skyEnd = viewDistance;
         fog.cloudEnd = viewDistance;
      }
   }
}
