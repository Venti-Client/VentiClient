//? if >=26.3 {
/*package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No Render: hide the totem animation. 26.3 starts it in LocalPlayer (GameRenderer before). Listed by VentiMixinPlugin.
@Mixin(LocalPlayer.class)
public abstract class ViewItemActivationMixin {
   @Inject(method = "displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideTotemAnimation(ItemStack stack, CallbackInfo ci) {
      if (NoRenderModule.hideTotemAnimation()) {
         ci.cancel();
      }
   }
}
*///?}
