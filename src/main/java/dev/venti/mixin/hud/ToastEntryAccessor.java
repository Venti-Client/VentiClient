package dev.venti.mixin.hud;

import net.minecraft.client.toast.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.toast.ToastManager$Entry")
public interface ToastEntryAccessor {
   @Accessor("instance")
   Toast venti$Toast();

   @Accessor("topIndex")
   int venti$FirstSlot();

   @Accessor("visibility")
   Toast.Visibility venti$Visibility();

   @Accessor("visibleWidthPortion")
   float venti$VisiblePortion();
}
