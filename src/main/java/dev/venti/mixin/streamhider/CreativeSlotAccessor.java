package dev.venti.mixin.streamhider;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The creative screen wraps inventory slots; No Inv Leak needs the wrapped slot's real inventory index. */
@Mixin(targets = "net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen$CreativeSlot")
public interface CreativeSlotAccessor {
   @Accessor("slot")
   Slot venti$noInvLeakTarget();
}
