package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}

/** No Render (fire overlay); Custom Fov keeps screen overlays the same size as with the vanilla FOV. */
//? if >=26.2 {
@Mixin(ScreenEffectRenderer.class)
public abstract class ViewOverlayRendererMixin {
   @ModifyExpressionValue(
      //? if >=26.3 {
      /*method = "submit(FLnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Z)V",
      *///?} else {
      method = "submit(ZZFLnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
      //?}
      at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
   )
   private PoseStack venti$undoCustomFov(PoseStack matrices) {
      float scale = CustomFovModule.screenEffectScale();
      if (scale != 1.0F) {
         matrices.last().pose().scale(scale, scale, 1.0F);
      }

      return matrices;
   }

   @Inject(
      method = "submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideFireOverlay(PoseStack matrices, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (NoRenderModule.hideFireOverlay()) {
         ci.cancel();
      }
   }
}
//?} else {
/*@Mixin(ScreenEffectRenderer.class)
public abstract class ViewOverlayRendererMixin {
   @ModifyExpressionValue(
      method = "renderScreenEffect(ZFLnet/minecraft/client/renderer/SubmitNodeCollector;)V",
      at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;")
   )
   private PoseStack venti$undoCustomFov(PoseStack matrices) {
      float scale = CustomFovModule.screenEffectScale();
      if (scale != 1.0F) {
         matrices.last().pose().scale(scale, scale, 1.0F);
      }

      return matrices;
   }

   @Inject(
      method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideFireOverlay(PoseStack matrices, MultiBufferSource vertexConsumers, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (NoRenderModule.hideFireOverlay()) {
         ci.cancel();
      }
   }
}
*///?}
