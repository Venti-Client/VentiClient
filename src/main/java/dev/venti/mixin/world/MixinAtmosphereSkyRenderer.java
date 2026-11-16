package dev.venti.mixin.world;

import dev.venti.modules.world.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.state.SkyRenderState;
//? if >=1.21.11
import net.minecraft.world.MoonPhase;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.SkyRendering;
//? if >=1.21.11 {
import net.minecraft.world.dimension.DimensionType.Skybox;
//?} else {
/*import net.minecraft.client.render.DimensionEffects.SkyType;
import net.minecraft.util.math.Vec3d;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRendering.class})
public abstract class MixinAtmosphereSkyRenderer {
   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   //? if >=1.21.11 {
   private void venti$readSky(ClientWorld level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && state.skybox == Skybox.OVERWORLD) {
   //?} else {
   /*private void venti$readSky(ClientWorld level, float partialTick, Vec3d cameraPos, SkyRenderState state, CallbackInfo ci) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && state.skyType == SkyType.NORMAL) {
   *///?}
         atmosphere.extract(level, partialTick, state);
      }
   }

   @Inject(
      method = {"renderTopSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   //? if >=1.21.11 {
   private void venti$dome(int color, CallbackInfo ci) {
   //?} else {
   /*private void venti$dome(float red, float green, float blue, CallbackInfo ci) {
   *///?}
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.skyDisc()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderTopSky"},
      at = {@At("TAIL")}
   )
   //? if >=1.21.11 {
   private void venti$afterDome(int color, CallbackInfo ci) {
   //?} else {
   /*private void venti$afterDome(float red, float green, float blue, CallbackInfo ci) {
   *///?}
      AtmosphereModule atmosphere = AtmosphereModule.get();
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
      AtmosphereModule atmosphere = AtmosphereModule.get();
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
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.stars(pose)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         //? if >=1.21.11 {
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V"
         //?} else {
         /*target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(IFLnet/minecraft/client/util/math/MatrixStack;)V"
         *///?}
      )}
   )
   private void venti$beforeMoon(
      //? if >=1.21.11 {
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      //?} else {
      /*MatrixStack pose, float skyAngle, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      *///?}
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.beforeMoon(pose);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         //? if >=1.21.11 {
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V",
         //?} else {
         /*target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(IFLnet/minecraft/client/util/math/MatrixStack;)V",
         *///?}
         shift = Shift.AFTER
      )}
   )
   private void venti$afterMoon(
      //? if >=1.21.11 {
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      //?} else {
      /*MatrixStack pose, float skyAngle, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      *///?}
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null) {
         atmosphere.afterMoon(pose);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At("TAIL")}
   )
   private void venti$afterCelestials(
      //? if >=1.21.11 {
      MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      //?} else {
      /*MatrixStack pose, float skyAngle, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
      *///?}
   ) {
      AtmosphereModule atmosphere = AtmosphereModule.get();
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
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.endSky()) {
         ci.cancel();
      }
   }
}
