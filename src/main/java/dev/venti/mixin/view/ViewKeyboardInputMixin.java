package dev.venti.mixin.view;

import dev.venti.modules.view.SprintModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sprint: report the sprint key as held, exactly like vanilla's Toggle Sprint does. */
@Mixin(KeyboardInput.class)
public abstract class ViewKeyboardInputMixin {
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void venti$holdSprint(CallbackInfo ci) {
      Input input = (Input)(Object)this;
      MinecraftClient mc = MinecraftClient.getInstance();
      PlayerInput keys = input.playerInput;
      if (SprintModule.holding() && mc.player != null && mc.player.input == input && !keys.sprint()) {
         input.playerInput = new PlayerInput(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.sneak(), true);
      }
   }
}
