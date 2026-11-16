package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
//? if >=1.21.9 {
import net.minecraft.client.render.state.WeatherRenderState;
//?} else {
/*import net.minecraft.world.World;
*///?}
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: hide rain/snow and their splashes and sounds. */
@Mixin(WeatherRendering.class)
public abstract class ViewNoRenderWeatherMixin {
   @Inject(
      //? if >=1.21.9 {
      method = "renderPrecipitation(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/state/WeatherRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideWeather(VertexConsumerProvider vertexConsumers, Vec3d cameraPos, WeatherRenderState state, CallbackInfo ci) {
      //?} else {
      /*method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideWeather(World world, VertexConsumerProvider vertexConsumers, int ticks, float tickProgress, Vec3d cameraPos, CallbackInfo ci) {
      *///?}
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }

   @Inject(
      //? if >=1.21.11 {
      method = "addParticlesAndSound(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/render/Camera;ILnet/minecraft/particle/ParticlesMode;I)V",
      //?} else {
      /*method = "addParticlesAndSound(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/render/Camera;ILnet/minecraft/particle/ParticlesMode;)V",
      *///?}
      at = @At("HEAD"),
      cancellable = true
   )
   //? if >=1.21.11 {
   private void venti$hideRainSplashes(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, int weatherRadius, CallbackInfo ci) {
   //?} else {
   /*private void venti$hideRainSplashes(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, CallbackInfo ci) {
   *///?}
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }
}
