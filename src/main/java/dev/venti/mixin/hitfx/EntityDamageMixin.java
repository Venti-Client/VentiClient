package dev.venti.mixin.hitfx;

import dev.venti.modules.hitfx.HitFxModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells Hit FX when the server reports damage, so it can react to you getting hit. */
@Mixin(ClientPacketListener.class)
public abstract class EntityDamageMixin {
   // TAIL runs on the client thread: the handler first re-queues itself there.
   @Inject(method = "handleDamageEvent", at = @At("TAIL"))
   private void venti$onDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
      HitFxModule.onEntityDamage(packet);
   }
}
