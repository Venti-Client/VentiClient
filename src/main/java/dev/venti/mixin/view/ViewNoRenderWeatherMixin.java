package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
//? if <26.2 {
/*import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.level.ParticleStatus;
*///?}
import net.minecraft.client.renderer.WeatherEffectRenderer;
//? if >=26.2 {
import net.minecraft.client.renderer.state.level.WeatherRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.WeatherRenderState;
*///?}
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: hide rain/snow and their splashes and sounds. */
//? if >=26.2 {
@Mixin(WeatherEffectRenderer.class)
public abstract class ViewNoRenderWeatherMixin {
   @Inject(
      method = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideWeather(Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }
   // 26.x ticks rain splashes and sounds in ClientLevel.tickWeatherEffects (ViewNoRenderRainTickMixin).
}
//?} else {
/*@Mixin(WeatherEffectRenderer.class)
public abstract class ViewNoRenderWeatherMixin {
   @Inject(
      method = "render(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/WeatherRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideWeather(MultiBufferSource vertexConsumers, Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "tickRainParticles(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/Camera;ILnet/minecraft/server/level/ParticleStatus;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideRainSplashes(ClientLevel world, Camera camera, int ticks, ParticleStatus particlesMode, int weatherRadius, CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }
}
*///?}
