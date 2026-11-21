package dev.venti.mixin.hud;

import java.util.List;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the module list slide below toasts in the top-right corner. */
@Mixin(ToastManager.class)
public interface ToastManagerAccessor {
   @Accessor("visibleToasts")
   List<?> venti$VisibleToasts();
}
