package dev.venti.mixin.waypoints;

import dev.venti.modules.waypoints.WaypointsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server sends this packet to a player when that player dies (it opens the death screen). On the network thread
 * the handler throws to hop onto the client thread, so RETURN is only reached on the client thread.
 */
@Mixin(ClientPacketListener.class)
public abstract class MixinWaypointsDeath {
   @Inject(method = "handlePlayerCombatKill", at = @At("RETURN"))
   private void venti$waypointsDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.isSameThread() && mc.player != null && packet.playerId() == mc.player.getId()) {
         WaypointsModule module = WaypointsModule.get();
         if (module != null) {
            module.onPlayerDeath();
         }
      }
   }
}
