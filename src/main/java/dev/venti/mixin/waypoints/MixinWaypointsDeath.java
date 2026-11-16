package dev.venti.mixin.waypoints;

import dev.venti.modules.waypoints.WaypointsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server sends this packet to a player when that player dies (it opens the death screen). On the network thread
 * the handler throws to hop onto the client thread, so RETURN is only reached on the client thread.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinWaypointsDeath {
   @Inject(method = "onDeathMessage", at = @At("RETURN"))
   private void venti$waypointsDeath(DeathMessageS2CPacket packet, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.isOnThread() && mc.player != null && packet.playerId() == mc.player.getId()) {
         WaypointsModule module = WaypointsModule.get();
         if (module != null) {
            module.onPlayerDeath();
         }
      }
   }
}
