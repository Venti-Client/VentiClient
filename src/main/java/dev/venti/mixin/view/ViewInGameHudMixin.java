package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.CustomCrosshairModule;
import dev.venti.modules.view.NameProtectModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom Crosshair; No Render (pumpkin/powder snow, portal and nausea overlays); Name Protect (titles, action bar, sidebar). */
@Mixin(InGameHud.class)
public abstract class ViewInGameHudMixin {
   @Inject(
      method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$customCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomCrosshairModule module = CustomCrosshairModule.active;
      if (module != null && module.render(context)) {
         ci.cancel();
      }
   }

   @Inject(method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideTextureOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
      if (NoRenderModule.hidePumpkinPowderSnow()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hidePortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderNauseaOverlay(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideNauseaOverlay(DrawContext context, float distortionStrength, CallbackInfo ci) {
      if (NoRenderModule.hideNauseaPortal()) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "setTitle(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
   private Text venti$protectTitle(Text title) {
      return NameProtectModule.protect(title);
   }

   @ModifyVariable(method = "setSubtitle(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
   private Text venti$protectSubtitle(Text subtitle) {
      return NameProtectModule.protect(subtitle);
   }

   @ModifyVariable(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), argsOnly = true)
   private Text venti$protectActionBar(Text message) {
      return NameProtectModule.protect(message);
   }

   @ModifyExpressionValue(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getDisplayName()Lnet/minecraft/text/Text;")
   )
   private Text venti$protectSidebarTitle(Text original) {
      return NameProtectModule.protect(original);
   }

   @ModifyExpressionValue(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At(value = "FIELD", opcode = 180, target = "Lnet/minecraft/client/gui/hud/InGameHud$SidebarEntry;name:Lnet/minecraft/text/Text;")
   )
   private Text venti$protectSidebarLine(Text original) {
      return NameProtectModule.protect(original);
   }
}
