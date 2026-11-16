package dev.venti.mixin.world;

import dev.venti.modules.world.WorldHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pops (entity status 35) for Particles and Totem Spirit, seen on the client thread. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinWorldTotemPops {
   @Inject(method = "onEntityStatus", at = @At("HEAD"))
   private void venti$totemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (packet.getStatus() == 35 && mc.isOnThread() && mc.world != null && packet.getEntity(mc.world) instanceof LivingEntity living) {
         WorldHooks.totemPop(living);
      }
   }
}
