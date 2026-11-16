package dev.venti.mixin.hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ItemCooldownManager.Entry is package-private, so its start and end ticks are read through this accessor. */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface CooldownEntryAccessor {
   @Accessor("startTick")
   int venti$StartTime();

   @Accessor("endTick")
   int venti$EndTime();
}
