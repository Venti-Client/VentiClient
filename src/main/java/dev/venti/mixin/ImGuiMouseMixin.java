//? if >=26.3 {
/*package dev.venti.mixin;

import dev.venti.imgui.ImGuiManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ImGui input on 26.3 (SDL3): mouse movement, buttons and wheel go to ImGui as well as to Minecraft, as the GLFW
// backend's chained callbacks did before. Listed by VentiMixinPlugin.
@Mixin(MouseHandler.class)
public abstract class ImGuiMouseMixin {
   @Inject(method = "onMove(JDDDD)V", at = @At("HEAD"))
   private void venti$imguiMove(long handle, double x, double y, double dx, double dy, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         ImGuiManager.getInstance().onMouseMove(x, y);
      }
   }

   @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"))
   private void venti$imguiButton(long handle, MouseButtonInfo button, int action, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         ImGuiManager.getInstance().onMouseButton(button, action);
      }
   }

   @Inject(method = "onScroll(JDD)V", at = @At("HEAD"))
   private void venti$imguiScroll(long handle, double x, double y, CallbackInfo ci) {
      if (handle == Minecraft.getInstance().getWindow().handle()) {
         ImGuiManager.getInstance().onScroll(x, y);
      }
   }
}
*///?}
