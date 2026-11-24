package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockRenderer.class)
public abstract class ViewNoRenderFallingBlockMixin {
   @Inject(
      //? if >=26.3 {
      /*method = "shouldRender(Lnet/minecraft/world/entity/item/FallingBlockEntity;Lnet/minecraft/client/renderer/culling/Frustum;DDDF)Z",
      *///?} else {
      method = "shouldRender(Lnet/minecraft/world/entity/item/FallingBlockEntity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
      //?}
      at = @At("HEAD"),
      cancellable = true
   )
   //? if >=26.3 {
   /*private void venti$hideFallingBlock(
      FallingBlockEntity entity, Frustum frustum, double x, double y, double z, float partialTicks, CallbackInfoReturnable<Boolean> cir
   ) {
   *///?} else {
   private void venti$hideFallingBlock(FallingBlockEntity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
   //?}
      if (NoRenderModule.hideFallingBlocks()) {
         cir.setReturnValue(false);
      }
   }
}
