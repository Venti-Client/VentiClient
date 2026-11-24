//? if >=26.3 {
/*package dev.venti.mixin;

import dev.venti.imgui.ImGuiManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ImGui input on 26.3 (SDL3): keys and typed text reach ImGui before Minecraft. A key ImGui takes while the
// Venti GUI is open does not reach Minecraft, as with the GLFW key callback before. Listed by VentiMixinPlugin.
@Mixin(KeyboardHandler.class)
public abstract class ImGuiKeyboardMixin {
   @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"), cancellable = true)
   private void venti$imguiKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle() && ImGuiManager.getInstance().onKey(action, event)) {
         ci.cancel();
      }
   }

   @Inject(method = "textInput(JLjava/lang/String;)V", at = @At("HEAD"))
   private void venti$imguiText(long handle, String text, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         ImGuiManager.getInstance().onText(text);
      }
   }
}
*///?}
