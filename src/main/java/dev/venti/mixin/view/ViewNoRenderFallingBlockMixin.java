package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntityRenderer.class)
public abstract class ViewNoRenderFallingBlockMixin {
   @Inject(
      method = "shouldRender(Lnet/minecraft/entity/FallingBlockEntity;Lnet/minecraft/client/render/Frustum;DDD)Z",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$hideFallingBlock(FallingBlockEntity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideFallingBlocks()) {
         cir.setReturnValue(false);
      }
   }
}
