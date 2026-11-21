package dev.venti.mixin.view;

import dev.venti.modules.view.SprintModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sprint: report the sprint key as held, exactly like vanilla's Toggle Sprint does. */
@Mixin(KeyboardInput.class)
public abstract class ViewKeyboardInputMixin {
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void venti$holdSprint(CallbackInfo ci) {
      ClientInput input = (ClientInput)(Object)this;
      Minecraft mc = Minecraft.getInstance();
      Input keys = input.keyPresses;
      if (SprintModule.holding() && mc.player != null && mc.player.input == input && !keys.sprint()) {
         input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift(), true);
      }
   }
}
