package dev.venti.mixin.hud;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the local player's cooldown groups for the HUD cooldown tray. */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
   @Accessor("entries")
   Map<Identifier, ?> venti$Cooldowns();

   @Accessor("tick")
   int venti$TickCount();
}
