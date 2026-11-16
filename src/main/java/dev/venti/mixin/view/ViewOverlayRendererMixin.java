package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render (fire overlay); Custom Fov keeps screen overlays the same size as with the vanilla FOV. */
@Mixin(InGameOverlayRenderer.class)
public abstract class ViewOverlayRendererMixin {
   @ModifyExpressionValue(
      method = "renderOverlays(ZFLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
      at = @At(value = "NEW", target = "()Lnet/minecraft/client/util/math/MatrixStack;")
   )
   private MatrixStack venti$undoCustomFov(MatrixStack matrices) {
      float scale = CustomFovModule.screenEffectScale();
      if (scale != 1.0F) {
         matrices.peek().getPositionMatrix().scale(scale, scale, 1.0F);
      }

      return matrices;
   }

   @Inject(
      method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
      if (NoRenderModule.hideFireOverlay()) {
         ci.cancel();
      }
   }
}
