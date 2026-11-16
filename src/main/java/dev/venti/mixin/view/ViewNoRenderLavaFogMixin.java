package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
//? if >=1.21.11 {
import net.minecraft.client.render.Camera;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
*///?}
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
      //? if >=1.21.11 {
      method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V",
      //?} else {
      /*method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V",
      *///?}
      at = @At("TAIL")
   )
   //? if >=1.21.11 {
   private void venti$clearLavaFog(FogData fog, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
   //?} else {
   /*private void venti$clearLavaFog(
      FogData fog, Entity cameraEntity, BlockPos cameraPos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci
   ) {
   *///?}
      if (NoRenderModule.hideLavaFog()) {
         fog.environmentalStart = -8.0F;
         fog.environmentalEnd = viewDistance;
         fog.skyEnd = viewDistance;
         fog.cloudEnd = viewDistance;
      }
   }
}
