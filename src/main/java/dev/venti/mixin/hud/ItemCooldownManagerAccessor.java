package dev.venti.mixin.hud;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the local player's cooldown groups for the HUD cooldown tray. */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownManagerAccessor {
   @Accessor("cooldowns")
   Map<Identifier, ?> venti$Cooldowns();

   @Accessor("tickCount")
   int venti$TickCount();
}
