package dev.venti.mixin.hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ItemCooldownManager.Entry is package-private, so its start and end ticks are read through this accessor. */
@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface CooldownEntryAccessor {
   @Accessor("startTime")
   int venti$StartTime();

   @Accessor("endTime")
   int venti$EndTime();
}
