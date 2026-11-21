package dev.venti.mixin.hud;

import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance")
public interface ToastEntryAccessor {
   @Accessor("toast")
   Toast venti$Toast();

   @Accessor("firstSlotIndex")
   int venti$FirstSlot();

   @Accessor("visibility")
   Toast.Visibility venti$Visibility();

   @Accessor("visiblePortion")
   float venti$VisiblePortion();
}
