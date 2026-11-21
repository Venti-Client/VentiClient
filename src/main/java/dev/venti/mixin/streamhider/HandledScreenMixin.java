package dev.venti.mixin.streamhider;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.streamhider.StreamHiderModule;
import dev.venti.modules.streamhider.StreamOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Your own slots in every container screen, the cursor stack, drag previews and tooltips. */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
   @Shadow
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;

   @Shadow
   public abstract AbstractContainerMenu getMenu();

   @WrapOperation(
      method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack venti$noInvLeakSlot(Slot slot, Operation<ItemStack> original, @Local(argsOnly = true) GuiGraphics context) {
      ItemStack stack = original.call(slot);
      if (!StreamHiderModule.ownSlot(slot)) {
         return stack;
      }

      int sx = StreamHiderModule.screenX(context, slot.x, slot.y, this.leftPos + slot.x);
      int sy = StreamHiderModule.screenY(context, slot.x, slot.y, this.topPos + slot.y);
      return StreamHiderModule.container(stack, slot, sx, sy, slot == this.hoveredSlot);
   }

   @WrapOperation(
      method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack venti$noInvLeakDragPreview(ItemStack stack, int count, Operation<ItemStack> original) {
      return StreamHiderModule.dragged(original.call(stack, count));
   }

   @WrapOperation(
      method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;II)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getNoItemIcon()Lnet/minecraft/resources/Identifier;")
   )
   private Identifier venti$noInvLeakSilhouette(Slot slot, Operation<Identifier> original) {
      Identifier icon = original.call(slot);
      return icon != null && StreamHiderModule.overlaying() && StreamHiderModule.ownSlot(slot) && slot.hasItem() ? null : icon;
   }

   @ModifyVariable(
      method = "renderFloatingItem(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack venti$noInvLeakCarried(ItemStack stack, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
      return StreamHiderModule.carried(stack, x, y);
   }

   @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
   private void venti$noInvLeakTooltip(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
      if (StreamHiderModule.hidesTooltips() && StreamHiderModule.ownSlot(this.hoveredSlot)) {
         if (StreamHiderModule.overlaying() && this.hoveredSlot.hasItem() && this.getMenu().getCarried().isEmpty()) {
            StreamOverlay.tooltip(this.hoveredSlot.getItem(), mouseX, mouseY);
         }

         ci.cancel();
      }
   }
}
