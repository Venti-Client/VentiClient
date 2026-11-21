package dev.venti.mixin.world;

import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.world.MoonPhase;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRendering.class})
public abstract class MixinAuroraSkySkyRenderer {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void venti$readSky(ClientWorld level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && state.skybox == Skybox.OVERWORLD) {
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(
      method = {"renderTopSky"},
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
      method = {"renderTopSky"},
      at = {@At("TAIL")}
   )
   private void venti$afterDome(int color, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.afterSkyDisc();
      }
   }

   @Inject(
      method = {"renderGlowingSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$noSunriseFan(MatrixStack pose, float sunAngle, int color, CallbackInfo ci) {
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
   private void venti$stars(float starBrightness, MatrixStack pose, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.stars(pose)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V"
      )}
   )
   private void venti$beforeMoon(
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.beforeMoon(pose);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V",
         shift = Shift.AFTER
      )}
   )
   private void venti$afterMoon(
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null) {
         atmosphere.afterMoon(pose);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At("TAIL")}
   )
   private void venti$afterCelestials(
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
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
