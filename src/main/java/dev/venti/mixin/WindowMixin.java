package dev.venti.mixin;

import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.streamhider.StreamOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.3 {
/*import com.mojang.renderpearl.frontend.FrontendGpuSurface;
import dev.venti.compat.Mc;
*///?} elif >=26.2 {
import com.mojang.blaze3d.systems.GpuSurface;
import dev.venti.compat.Mc;
//?} else {
/*import com.mojang.blaze3d.platform.Window;
*///?}

/** ImGui draws straight onto the default framebuffer, last thing before the frame is shown. */
//? if >=26.3 {
/*// 26.3 presents through FrontendGpuSurface (GpuSurface became an interface); the frame is already blitted to the
// window when present() runs. ImGui and the overlay draw with OpenGL, so only on the OpenGL backend.
@Mixin(FrontendGpuSurface.class)
public abstract class WindowMixin {
   @Inject(method = "present", at = @At("HEAD"))
   private void venti$beforeSwap(CallbackInfo ci) {
      if (Mc.openGl()) {
         StreamOverlay.endFrame();
         ImGuiManager.getInstance().renderIfActive();
      }
   }
}
*///?} elif >=26.2 {
// 26.x presents through GpuSurface; ImGui and the overlay draw with OpenGL, so only on the OpenGL backend.
@Mixin(GpuSurface.class)
public abstract class WindowMixin {
   @Inject(method = "present", at = @At("HEAD"))
   private void venti$beforeSwap(CallbackInfo ci) {
      if (Mc.openGl()) {
         StreamOverlay.endFrame();
         ImGuiManager.getInstance().renderIfActive();
      }
   }
}
//?} else {
/*@Mixin(Window.class)
public abstract class WindowMixin {
   @Inject(method = "updateDisplay", at = @At("HEAD"))
   private void venti$beforeSwap(CallbackInfo ci) {
      StreamOverlay.endFrame();
      ImGuiManager.getInstance().renderIfActive();
   }
}
*///?}
