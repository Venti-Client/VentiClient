package dev.venti.mixin.world;

import dev.venti.modules.world.WorldHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pops (entity status 35) for Particles and Totem Spirit, seen on the client thread. */
@Mixin(ClientPacketListener.class)
public abstract class MixinWorldTotemPops {
   @Inject(method = "handleEntityEvent", at = @At("HEAD"))
   private void venti$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (packet.getEventId() == 35 && mc.isSameThread() && mc.level != null && packet.getEntity(mc.level) instanceof LivingEntity living) {
         WorldHooks.totemPop(living);
      }
   }
}
