package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Name Protect: boss bar titles. */
@Mixin(BossHealthOverlay.class)
public abstract class ViewNameProtectBossBarMixin {
   @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectBossName(Component original) {
      return NameProtectModule.protect(original);
   }
}
