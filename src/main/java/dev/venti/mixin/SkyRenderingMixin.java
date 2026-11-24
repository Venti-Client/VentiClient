//? if >=26.3 {
/*package dev.venti.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.venti.compat.SkyPass;
import dev.venti.modules.sky.CustomSkyModule;
import dev.venti.modules.spotify.sky.SkyLyrics;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.3 draws the whole sky in one render pass; Venti's sky drawing suspends it (SkyPass) and draws its own passes.
@Mixin(SkyRenderer.class)
public abstract class SkyRenderingMixin {
   @WrapOperation(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
      )
   )
   private RenderPass venti$suspendableSkyPass(
      CommandEncoder encoder,
      Supplier<String> label,
      GpuTextureView color,
      Optional<Vector4fc> clearColor,
      GpuTextureView depth,
      OptionalDouble clearDepth,
      Operation<RenderPass> original
   ) {
      return SkyPass.open(encoder, label, color, clearColor, depth, clearDepth);
   }

   // Custom Sky draws its picture in place of the vanilla sky dome.
   @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
   private void venti$customSky(RenderPass pass, Vector3fc color, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null) {
         SkyPass.suspend();
         if (sky.render(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false))) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
   private void venti$noDarkBelowHorizon(RenderPass pass, CallbackInfo ci) {
      if (CustomSkyModule.active() != null) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
   private void venti$noSunset(RenderPass pass, PoseStack matrices, float angle, Vector4fc color, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunset()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
   private void venti$noSun(RenderPass pass, float alpha, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
   private void venti$noMoon(RenderPass pass, MoonPhase phase, float alpha, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideSunMoon()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
   private void venti$noStars(RenderPass pass, float brightness, PoseStack matrices, CallbackInfo ci) {
      CustomSkyModule sky = CustomSkyModule.active();
      if (sky != null && sky.hideStars()) {
         ci.cancel();
      }
   }

   // Sky lyrics are painted onto the sky right after the sun, moon and stars.
   @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
   private void venti$afterCelestials(
      RenderPass pass,
      PoseStack matrices,
      float sunAngle,
      float moonAngle,
      float starAngle,
      MoonPhase moonPhase,
      float rainBrightness,
      float starBrightness,
      CallbackInfo ci
   ) {
      SkyPass.suspend();
      SkyLyrics.render();
   }
}
*///?} else {
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
//?}
