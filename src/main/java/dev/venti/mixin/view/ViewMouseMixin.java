package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.FreelookModule;
import dev.venti.modules.view.ZoomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom scroll and lowered sensitivity; Freelook camera turning. */
@Mixin(MouseHandler.class)
public abstract class ViewMouseMixin {
   @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
   private void venti$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (vertical != 0.0 && window == mc.getWindow().handle() && mc.screen == null && mc.getOverlay() == null && ZoomModule.scroll(vertical)) {
         ci.cancel();
      }
   }

   @WrapOperation(
      method = "turnPlayer(D)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
   )
   private void venti$turn(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
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
