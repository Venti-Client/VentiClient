package dev.venti.mixin;

import dev.venti.modules.sky.CustomSkyModule;
import dev.venti.modules.spotify.sky.SkyLyrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.util.math.MatrixStack;
//? if <1.21.9
//import net.minecraft.client.render.VertexConsumerProvider;
//? if >=1.21.11
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {
   /** Custom Sky draws its picture in place of the vanilla sky dome. */
   @Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true)
   //? if >=1.21.11 {
   private void venti$customSky(int color, CallbackInfo ci) {
   //?} else {
   /*private void venti$customSky(float red, float green, float blue, CallbackInfo ci) {
   *///?}
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.render(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false))) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
   private void venti$noDarkBelowHorizon(CallbackInfo ci) {
      if (CustomSkyModule.active() != null) {
         ci.cancel();
      }
   }

   @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
   //? if >=1.21.9 {
   private void venti$noSunset(MatrixStack matrices, float angle, int color, CallbackInfo ci) {
   //?} else {
   /*private void venti$noSunset(MatrixStack matrices, VertexConsumerProvider.Immediate buffers, float angle, int color, CallbackInfo ci) {
   *///?}
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunset()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
   //? if >=1.21.9 {
   private void venti$noSun(float alpha, MatrixStack matrices, CallbackInfo ci) {
   //?} else {
   /*private void venti$noSun(float alpha, VertexConsumerProvider buffers, MatrixStack matrices, CallbackInfo ci) {
   *///?}
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
   //? if >=1.21.11 {
   private void venti$noMoon(MoonPhase phase, float alpha, MatrixStack matrices, CallbackInfo ci) {
   //?} elif >=1.21.9 {
   /*private void venti$noMoon(int phase, float alpha, MatrixStack matrices, CallbackInfo ci) {
   *///?} else {
   /*private void venti$noMoon(int phase, float alpha, VertexConsumerProvider buffers, MatrixStack matrices, CallbackInfo ci) {
   *///?}
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
   private void venti$noStars(float brightness, MatrixStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideStars()) {
         ci.cancel();
      }
   }

   /** Sky lyrics are painted onto the sky right after the sun, moon and stars. */
   @Inject(method = "renderCelestialBodies", at = @At("TAIL"))
   private void venti$afterCelestials(
      //? if >=1.21.11 {
      MatrixStack matrices, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      //?} elif >=1.21.9 {
      /*MatrixStack matrices, float skyAngle, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      *///?} else {
      /*MatrixStack matrices, VertexConsumerProvider.Immediate buffers, float skyAngle, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      *///?}
   ) {
      SkyLyrics.render();
   }
}
