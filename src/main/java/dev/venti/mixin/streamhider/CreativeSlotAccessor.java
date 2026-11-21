package dev.venti.mixin.streamhider;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The creative screen wraps inventory slots; No Inv Leak needs the wrapped slot's real inventory index. */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeSlotAccessor {
   @Accessor("target")
   Slot venti$noInvLeakTarget();
}
