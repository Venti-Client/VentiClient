package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.FreelookModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Freelook: the camera uses the freelook angles instead of the entity's. Only the two calls that pass the entity's
 * yaw/pitch are wrapped; the mirrored front view and the sleeping view keep deriving from them like vanilla.
 */
@Mixin(Camera.class)
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
