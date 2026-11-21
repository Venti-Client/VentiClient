package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Name Protect: rebuild aliases when the player list changes; protect the death screen message. */
@Mixin(ClientPacketListener.class)
public abstract class ViewNameProtectNetworkMixin {
   @Inject(method = "handlePlayerInfoUpdate(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;)V", at = @At("TAIL"))
   private void venti$playerListChanged(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
      NameProtectModule.onPlayerInfoApplied(packet);
   }

   @ModifyExpressionValue(
      method = "handlePlayerCombatKill(Lnet/minecraft/network/protocol/game/ClientboundPlayerCombatKillPacket;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerCombatKillPacket;message()Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectDeathMessage(Component message) {
      return NameProtectModule.protect(message);
   }
}
