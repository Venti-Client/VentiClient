package dev.venti.mixin.noinvleak;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.noinvleak.NoInvLeakModule;
import dev.venti.modules.noinvleak.StreamOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Your own slots in every container screen, the cursor stack, drag previews and tooltips. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
   @Shadow
   protected Slot focusedSlot;
   @Shadow
   protected int x;
   @Shadow
   protected int y;

   @Shadow
   public abstract ScreenHandler getScreenHandler();

   @WrapOperation(
      //? if >=1.21.11 {
      method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
      //?} else {
      /*method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
      *///?}
      at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;")
   )
   private ItemStack venti$noInvLeakSlot(Slot slot, Operation<ItemStack> original, @Local(argsOnly = true) DrawContext context) {
      ItemStack stack = original.call(slot);
      if (!NoInvLeakModule.ownSlot(slot)) {
         return stack;
      }

      int sx = NoInvLeakModule.screenX(context, slot.x, slot.y, this.x + slot.x);
      int sy = NoInvLeakModule.screenY(context, slot.x, slot.y, this.y + slot.y);
      return NoInvLeakModule.container(stack, slot, sx, sy, slot == this.focusedSlot);
   }

   @WrapOperation(
      //? if >=1.21.11 {
      method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
      //?} else {
      /*method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
      *///?}
      at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copyWithCount(I)Lnet/minecraft/item/ItemStack;")
   )
   private ItemStack venti$noInvLeakDragPreview(ItemStack stack, int count, Operation<ItemStack> original) {
      return NoInvLeakModule.dragged(original.call(stack, count));
   }

   @WrapOperation(
      //? if >=1.21.11 {
      method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
      //?} else {
      /*method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
      *///?}
      at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getBackgroundSprite()Lnet/minecraft/util/Identifier;")
   )
   private Identifier venti$noInvLeakSilhouette(Slot slot, Operation<Identifier> original) {
      Identifier icon = original.call(slot);
      return icon != null && NoInvLeakModule.overlaying() && NoInvLeakModule.ownSlot(slot) && slot.hasStack() ? null : icon;
   }

   @ModifyVariable(
      method = "drawItem(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack venti$noInvLeakCarried(ItemStack stack, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
      return NoInvLeakModule.carried(stack, x, y);
   }

   @Inject(method = "drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"), cancellable = true)
   private void venti$noInvLeakTooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (NoInvLeakModule.hidesTooltips() && NoInvLeakModule.ownSlot(this.focusedSlot)) {
         if (NoInvLeakModule.overlaying() && this.focusedSlot.hasStack() && this.getScreenHandler().getCursorStack().isEmpty()) {
            StreamOverlay.tooltip(this.focusedSlot.getStack(), mouseX, mouseY);
         }

         ci.cancel();
      }
   }
}
