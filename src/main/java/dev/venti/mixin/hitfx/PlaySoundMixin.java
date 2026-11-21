package dev.venti.mixin.hitfx;

import dev.venti.modules.hitfx.HitFxModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Hit FX see (and swap out) server hit sounds. Runs after the main-thread hop, so only once per packet. */
@Mixin(ClientPacketListener.class)
public abstract class PlaySoundMixin {
   @Inject(
      method = "handleSoundEvent",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
         shift = At.Shift.AFTER
      ),
      cancellable = true
   )
   private void venti$hitFx(ClientboundSoundPacket packet, CallbackInfo ci) {
      if (HitFxModule.onServerSound(packet)) {
         ci.cancel();
      }
   }
}
