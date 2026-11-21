package dev.venti.mixin.streamhider;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.venti.compat.Gfx;
import dev.venti.modules.streamhider.StreamHiderModule;
import dev.venti.modules.streamhider.StreamOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
*///?}

/** Hotbar items, the held item name popup and the selected slot frame. */
//? if >=26.2 {
@Mixin(Hud.class)
public abstract class InGameHudMixin {
   @Unique
   private static final Identifier VENTI$SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
   @Shadow
   private int toolHighlightTimer;
   @Shadow
   private ItemStack lastToolHighlight;

   @ModifyVariable(
      method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack venti$noInvLeakHotbar(
      ItemStack stack,
      @Local(argsOnly = true) GuiGraphicsExtractor context,
      @Local(argsOnly = true, ordinal = 0) int x,
      @Local(argsOnly = true, ordinal = 1) int y,
      @Local(argsOnly = true, ordinal = 2) int seed
   ) {
      return StreamHiderModule.hotbar(stack, StreamHiderModule.screenX(new Gfx(context), x, y, x), StreamHiderModule.screenY(new Gfx(context), x, y, y), seed);
   }

   @Inject(method = "extractSelectedItemName(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), cancellable = true)
   private void venti$noInvLeakItemName(GuiGraphicsExtractor context, CallbackInfo ci) {
      if (StreamHiderModule.hidesItemName()) {
         if (StreamHiderModule.overlaying() && this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            int alpha = Math.min(255, (int)(this.toolHighlightTimer * 256.0F / 10.0F));
            int y = context.guiHeight() - 59;
            if (!Minecraft.getInstance().gameMode.canHurtPlayer()) {
               y += 14;
            }

            StreamOverlay.itemName(this.lastToolHighlight, alpha, y);
         }

         ci.cancel();
      }
   }

   @WrapWithCondition(
      method = "extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
      )
   )
   private boolean venti$noInvLeakSelection(GuiGraphicsExtractor context, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
      if (StreamHiderModule.hidesSelectedSlot() && VENTI$SELECTION.equals(sprite)) {
         if (StreamHiderModule.overlaying()) {
            StreamOverlay.selection(x, y);
         }

         return false;
      }

      return true;
   }
}
//?} else {
/*@Mixin(Gui.class)
public abstract class InGameHudMixin {
   @Unique
   private static final Identifier VENTI$SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
   @Shadow
   private int toolHighlightTimer;
   @Shadow
   private ItemStack lastToolHighlight;

   @ModifyVariable(
      method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack venti$noInvLeakHotbar(
      ItemStack stack,
      @Local(argsOnly = true) GuiGraphics context,
      @Local(argsOnly = true, ordinal = 0) int x,
      @Local(argsOnly = true, ordinal = 1) int y,
      @Local(argsOnly = true, ordinal = 2) int seed
   ) {
      return StreamHiderModule.hotbar(stack, StreamHiderModule.screenX(new Gfx(context), x, y, x), StreamHiderModule.screenY(new Gfx(context), x, y, y), seed);
   }

   @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
   private void venti$noInvLeakItemName(GuiGraphics context, CallbackInfo ci) {
      if (StreamHiderModule.hidesItemName()) {
         if (StreamHiderModule.overlaying() && this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            int alpha = Math.min(255, (int)(this.toolHighlightTimer * 256.0F / 10.0F));
            int y = context.guiHeight() - 59;
            if (!Minecraft.getInstance().gameMode.canHurtPlayer()) {
               y += 14;
            }

            StreamOverlay.itemName(this.lastToolHighlight, alpha, y);
         }

         ci.cancel();
      }
   }

   @WrapWithCondition(
      method = "renderItemHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
      )
   )
   private boolean venti$noInvLeakSelection(GuiGraphics context, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
      if (StreamHiderModule.hidesSelectedSlot() && VENTI$SELECTION.equals(sprite)) {
         if (StreamHiderModule.overlaying()) {
            StreamOverlay.selection(x, y);
         }

         return false;
      }

      return true;
   }
}
*///?}
