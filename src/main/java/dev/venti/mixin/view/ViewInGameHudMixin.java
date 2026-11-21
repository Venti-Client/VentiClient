package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.compat.Gfx;
import dev.venti.modules.view.CustomCrosshairModule;
import dev.venti.modules.view.NameProtectModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
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

/** Custom Crosshair; No Render (pumpkin/powder snow, portal and nausea overlays); Name Protect (titles, action bar, sidebar). */
//? if >=26.2 {
@Mixin(Hud.class)
public abstract class ViewInGameHudMixin {
   @Inject(
      method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$customCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      CustomCrosshairModule module = CustomCrosshairModule.active;
      if (module != null && module.render(new Gfx(context))) {
         ci.cancel();
      }
   }

   @Inject(method = "extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideTextureOverlay(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo ci) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractPortalOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hidePortalOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideNauseaOverlay(GuiGraphicsExtractor context, float distortionStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "setTitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectTitle(Component title) {
      return NameProtectModule.protect(title);
   }

   @ModifyVariable(method = "setSubtitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectSubtitle(Component subtitle) {
      return NameProtectModule.protect(subtitle);
   }

   @ModifyVariable(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectActionBar(Component message) {
      return NameProtectModule.protect(message);
   }

   @ModifyExpressionValue(
      method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectSidebarTitle(Component original) {
      return NameProtectModule.protect(original);
   }

   @ModifyExpressionValue(
      method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "FIELD", opcode = 180, target = "Lnet/minecraft/client/gui/Hud$1DisplayEntry;name:Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectSidebarLine(Component original) {
      return NameProtectModule.protect(original);
   }
}
//?} else {
/*@Mixin(Gui.class)
public abstract class ViewInGameHudMixin {
   @Inject(
      method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$customCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
      CustomCrosshairModule module = CustomCrosshairModule.active;
      if (module != null && module.render(new Gfx(context))) {
         ci.cancel();
      }
   }

   @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideTextureOverlay(GuiGraphics context, Identifier texture, float opacity, CallbackInfo ci) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hidePortalOverlay(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideNauseaOverlay(GuiGraphics context, float distortionStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "setTitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectTitle(Component title) {
      return NameProtectModule.protect(title);
   }

   @ModifyVariable(method = "setSubtitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectSubtitle(Component subtitle) {
      return NameProtectModule.protect(subtitle);
   }

   @ModifyVariable(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), argsOnly = true)
   private Component venti$protectActionBar(Component message) {
      return NameProtectModule.protect(message);
   }

   @ModifyExpressionValue(
      method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectSidebarTitle(Component original) {
      return NameProtectModule.protect(original);
   }

   @ModifyExpressionValue(
      method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "FIELD", opcode = 180, target = "Lnet/minecraft/client/gui/Gui$1DisplayEntry;name:Lnet/minecraft/network/chat/Component;")
   )
   private Component venti$protectSidebarLine(Component original) {
      return NameProtectModule.protect(original);
   }
}
*///?}
