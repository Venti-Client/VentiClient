package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
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
      method = "renderPrecipitation(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/state/WeatherRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideWeather(VertexConsumerProvider vertexConsumers, Vec3d cameraPos, WeatherRenderState state, CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "addParticlesAndSound(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/render/Camera;ILnet/minecraft/particle/ParticlesMode;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideRainSplashes(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, int weatherRadius, CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }
}
