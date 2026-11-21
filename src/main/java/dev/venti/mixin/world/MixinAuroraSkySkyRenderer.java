package dev.venti.mixin.world;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRenderer.class})
public abstract class MixinAuroraSkySkyRenderer {
   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void venti$readSky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && state.skybox == Skybox.OVERWORLD) {
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$dome(int color, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.skyDisc()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("TAIL")}
   )
   private void venti$afterDome(int color, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.afterSkyDisc();
      }
   }

   @Inject(
      method = {"renderSunriseAndSunset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$noSunriseFan(PoseStack pose, float sunAngle, int color, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.replacesSunrise()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderStars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$stars(float starBrightness, PoseStack pose, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.stars(pose)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V"
      )}
   )
   private void venti$beforeMoon(
      PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.beforeMoon(pose);
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SkyRenderer;renderMoon(Lnet/minecraft/world/level/MoonPhase;FLcom/mojang/blaze3d/vertex/PoseStack;)V",
         shift = Shift.AFTER
      )}
   )
   private void venti$afterMoon(
      PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.afterMoon(pose);
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At("TAIL")}
   )
   private void venti$afterCelestials(
      PoseStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.afterCelestials();
      }
   }

   @Inject(
      method = {"renderEndSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$endSky(CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.endSky()) {
         ci.cancel();
      }
   }
}
