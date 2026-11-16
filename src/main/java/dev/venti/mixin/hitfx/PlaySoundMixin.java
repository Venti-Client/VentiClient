package dev.venti.mixin.hitfx;

import dev.venti.modules.hitfx.HitFxModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Hit FX see (and swap out) server hit sounds. Runs after the main-thread hop, so only once per packet. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PlaySoundMixin {
   @Inject(
      method = "onPlaySound",
      at = @At(
         value = "INVOKE",
         //? if >=1.21.9 {
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         //?} else {
         /*target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
         *///?}
         shift = At.Shift.AFTER
      ),
      cancellable = true
   )
   private void venti$hitFx(PlaySoundS2CPacket packet, CallbackInfo ci) {
      if (HitFxModule.onServerSound(packet)) {
         ci.cancel();
      }
   }
}
