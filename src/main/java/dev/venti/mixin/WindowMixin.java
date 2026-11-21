package dev.venti.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.streamhider.StreamOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ImGui draws straight onto the default framebuffer, last thing before the frame is shown. */
@Mixin(Window.class)
public abstract class WindowMixin {
   @Inject(method = "updateDisplay", at = @At("HEAD"))
   private void venti$beforeSwap(CallbackInfo ci) {
      StreamOverlay.endFrame();
      ImGuiManager.getInstance().renderIfActive();
   }
}
