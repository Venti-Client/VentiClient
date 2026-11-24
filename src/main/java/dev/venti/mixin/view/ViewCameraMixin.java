package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.FreelookModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.2 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.NoRenderModule;
import dev.venti.modules.view.ZoomModule;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}

/**
 * Freelook: the camera uses the freelook angles instead of the entity's. Only the two calls that pass the entity's
 * yaw/pitch are wrapped; the mirrored front view and the sleeping view keep deriving from them like vanilla.
 */
//? if >=26.2 {
@Mixin(Camera.class)
public abstract class ViewCameraMixin {
   @Shadow
   private boolean isPanoramicMode;

   @WrapOperation(
      method = "alignWithEntity(F)V",
      at = {
         @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
         @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1)
      }
   )
   private void venti$freelookRotation(Camera camera, float yaw, float pitch, Operation<Void> original) {
      FreelookModule freelook = FreelookModule.looking();
      if (freelook != null) {
         original.call(camera, freelook.cameraYaw(), freelook.cameraPitch());
      } else {
         original.call(camera, yaw, pitch);
      }
   }

   // Custom Fov and Zoom. 26.x computes the world FOV (calculateFov) and the hand FOV (calculateHudFov) in Camera;
   // up to 1.21.11 both came from GameRenderer.getFov (see ViewGameRendererMixin).
   @ModifyReturnValue(method = "calculateFov(F)F", at = @At("RETURN"))
   private float venti$fov(float original) {
      if (this.isPanoramicMode) {
         return original;
      }

      CustomFovModule module = CustomFovModule.active;
      return ZoomModule.fov(module != null ? module.fov() : original);
   }

   @ModifyReturnValue(method = "calculateHudFov(F)F", at = @At("RETURN"))
   private float venti$hudFov(float original) {
      if (this.isPanoramicMode) {
         return original;
      }

      CustomFovModule module = CustomFovModule.active;
      float fov = module != null ? module.fov() : original;
      CustomFovModule.hudFovApplied(original, fov);
      return fov;
   }

   // No Render: keep the sky under blindness/darkness (LevelRenderer.doesMobEffectBlockSky up to 1.21.11).
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   //? if >=26.3 {
   /*private void venti$keepSky(CameraRenderState cameraState, net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
   *///?} else {
   private void venti$keepSky(CameraRenderState cameraState, float partialTicks, CallbackInfo ci) {
   //?}
      if (NoRenderModule.hideBlindnessDarkness()) {
         cameraState.entityRenderState.doesMobEffectBlockSky = false;
      }
   }
}
//?} else {
/*@Mixin(Camera.class)
public abstract class ViewCameraMixin {
   @WrapOperation(
      method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
      at = {
         @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
         @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1)
      }
   )
   private void venti$freelookRotation(Camera camera, float yaw, float pitch, Operation<Void> original) {
      FreelookModule freelook = FreelookModule.looking();
      if (freelook != null) {
         original.call(camera, freelook.cameraYaw(), freelook.cameraPitch());
      } else {
         original.call(camera, yaw, pitch);
      }
   }
}
*///?}
