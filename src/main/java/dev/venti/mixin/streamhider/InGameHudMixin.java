package dev.venti.mixin.streamhider;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.venti.modules.streamhider.StreamHiderModule;
import dev.venti.modules.streamhider.StreamOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar items, the held item name popup and the selected slot frame. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
   @Unique
   private static final Identifier VENTI$SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
   @Shadow
   private int heldItemTooltipFade;
   @Shadow
   private ItemStack currentStack;

   @ModifyVariable(
      method = "renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V",
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack venti$noInvLeakHotbar(
      ItemStack stack,
      @Local(argsOnly = true) DrawContext context,
      @Local(argsOnly = true, ordinal = 0) int x,
      @Local(argsOnly = true, ordinal = 1) int y,
      @Local(argsOnly = true, ordinal = 2) int seed
   ) {
      return StreamHiderModule.hotbar(stack, StreamHiderModule.screenX(context, x, y, x), StreamHiderModule.screenY(context, x, y, y), seed);
   }

   @Inject(method = "renderHeldItemTooltip(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
   private void venti$noInvLeakItemName(DrawContext context, CallbackInfo ci) {
      if (StreamHiderModule.hidesItemName()) {
         if (StreamHiderModule.overlaying() && this.heldItemTooltipFade > 0 && !this.currentStack.isEmpty()) {
            int alpha = Math.min(255, (int)(this.heldItemTooltipFade * 256.0F / 10.0F));
            int y = context.getScaledWindowHeight() - 59;
            if (!MinecraftClient.getInstance().interactionManager.hasStatusBars()) {
               y += 14;
            }

            StreamOverlay.itemName(this.currentStack, alpha, y);
         }

         ci.cancel();
      }
   }

   @WrapWithCondition(
      method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"
      )
   )
   private boolean venti$noInvLeakSelection(DrawContext context, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
      if (StreamHiderModule.hidesSelectedSlot() && VENTI$SELECTION.equals(sprite)) {
         if (StreamHiderModule.overlaying()) {
            StreamOverlay.selection(x, y);
         }

         return false;
      }

      return true;
   }
}
