package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
//? if >=1.21.9 {
import net.minecraft.client.render.block.entity.TrialSpawnerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.MobSpawnerBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
*///?}
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: skip the spinning mob inside spawners (the shared helper is used by mob spawners and trial spawners). */
//? if >=1.21.9 {
@Mixin(TrialSpawnerBlockEntityRenderer.class)
//?} else {
/*@Mixin(MobSpawnerBlockEntityRenderer.class) // before 1.21.9 the shared helper lives on the mob spawner renderer
*///?}
public abstract class ViewNoRenderTrialSpawnerMixin {
   @Inject(
      //? if >=1.21.9 {
      method = "updateSpawnerRenderState(Lnet/minecraft/client/render/block/entity/state/MobSpawnerBlockEntityRenderState;FLnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/EntityRenderManager;DD)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideSpawnerMob(
      MobSpawnerBlockEntityRenderState state, float tickProgress, Entity displayEntity, EntityRenderManager manager, double rotation, double lastRotation, CallbackInfo ci
   ) {
      //?} else {
      /*method = "render(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/EntityRenderDispatcher;DD)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideSpawnerMob(
      float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Entity displayEntity, EntityRenderDispatcher dispatcher,
      double lastRotation, double rotation, CallbackInfo ci
   ) {
      *///?}
      if (NoRenderModule.hideSpawnerMob()) {
         ci.cancel();
      }
   }
}
