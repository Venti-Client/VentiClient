package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Name Protect: rebuild aliases when the player list changes; protect the death screen message. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ViewNameProtectNetworkMixin {
   @Inject(method = "onPlayerList(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;)V", at = @At("TAIL"))
   private void venti$playerListChanged(PlayerListS2CPacket packet, CallbackInfo ci) {
      NameProtectModule.onPlayerInfoApplied(packet);
   }

   @ModifyExpressionValue(
      method = "onDeathMessage(Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;message()Lnet/minecraft/text/Text;")
   )
   private Text venti$protectDeathMessage(Text message) {
      return NameProtectModule.protect(message);
   }
}
