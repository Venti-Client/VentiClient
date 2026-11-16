package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.FreelookModule;
import dev.venti.modules.view.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom scroll and lowered sensitivity; Freelook camera turning. */
@Mixin(Mouse.class)
public abstract class ViewMouseMixin {
   @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
   private void venti$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (vertical != 0.0 && window == mc.getWindow().getHandle() && mc.currentScreen == null && mc.getOverlay() == null && ZoomModule.scroll(vertical)) {
         ci.cancel();
      }
   }

   @WrapOperation(
      method = "updateMouse(D)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
   )
   private void venti$turn(ClientPlayerEntity player, double yaw, double pitch, Operation<Void> original) {
      double scale = ZoomModule.turnScale();
      yaw *= scale;
      pitch *= scale;
      FreelookModule freelook = FreelookModule.looking();
      if (freelook != null && freelook.cameraMode()) {
         freelook.turnCamera(yaw, pitch);
      } else {
         original.call(player, yaw, pitch);
      }
   }
}
