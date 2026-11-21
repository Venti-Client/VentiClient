package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.block.entity.TrialSpawnerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: skip the spinning mob inside spawners (the shared helper is used by mob spawners and trial spawners). */
@Mixin(TrialSpawnerBlockEntityRenderer.class)
public abstract class ViewNoRenderTrialSpawnerMixin {
   @Inject(
      method = "updateSpawnerRenderState(Lnet/minecraft/client/render/block/entity/state/MobSpawnerBlockEntityRenderState;FLnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/EntityRenderManager;DD)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideSpawnerMob(
      MobSpawnerBlockEntityRenderState state, float tickProgress, Entity displayEntity, EntityRenderManager manager, double rotation, double lastRotation, CallbackInfo ci
   ) {
      if (NoRenderModule.hideSpawnerMob()) {
         ci.cancel();
      }
   }
}
