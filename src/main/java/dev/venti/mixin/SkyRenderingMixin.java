package dev.venti.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.sky.CustomSkyModule;
import dev.venti.modules.spotify.sky.SkyLyrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRenderingMixin {
   /** Custom Sky draws its picture in place of the vanilla sky dome. */
   @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
   private void venti$customSky(int color, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.render(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))) {
         ci.cancel();
      }
   }

   @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
   private void venti$noDarkBelowHorizon(CallbackInfo ci) {
      if (CustomSkyModule.active() != null) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
   private void venti$noSunset(PoseStack matrices, float angle, int color, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunset()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
   private void venti$noSun(float alpha, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
   private void venti$noMoon(MoonPhase phase, float alpha, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
   private void venti$noStars(float brightness, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideStars()) {
         ci.cancel();
      }
   }

   /** Sky lyrics are painted onto the sky right after the sun, moon and stars. */
   @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
   private void venti$afterCelestials(
      PoseStack matrices, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      SkyLyrics.render();
   }
}
