package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render (fire overlay); Custom Fov keeps screen overlays the same size as with the vanilla FOV. */
@Mixin(ScreenEffectRenderer.class)
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
