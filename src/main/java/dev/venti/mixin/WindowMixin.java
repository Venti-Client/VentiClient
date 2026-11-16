package dev.venti.mixin;

import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.noinvleak.StreamOverlay;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ImGui draws straight onto the default framebuffer, last thing before the frame is shown. */
@Mixin(Window.class)
public abstract class WindowMixin {
   @Inject(method = "swapBuffers", at = @At("HEAD"))
   private void venti$beforeSwap(CallbackInfo ci) {
      StreamOverlay.endFrame();
      ImGuiManager.getInstance().renderIfActive();
   }
}
