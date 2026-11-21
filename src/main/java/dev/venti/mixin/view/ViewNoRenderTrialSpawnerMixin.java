package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.renderer.blockentity.TrialSpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Render: skip the spinning mob inside spawners (the shared helper is used by mob spawners and trial spawners). */
@Mixin(TrialSpawnerRenderer.class)
public abstract class ViewNoRenderTrialSpawnerMixin {
   @Inject(
      method = "extractSpawnerData(Lnet/minecraft/client/renderer/blockentity/state/SpawnerRenderState;FLnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;DD)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void venti$hideSpawnerMob(
      SpawnerRenderState state, float tickProgress, Entity displayEntity, EntityRenderDispatcher manager, double rotation, double lastRotation, CallbackInfo ci
   ) {
      if (NoRenderModule.hideSpawnerMob()) {
         ci.cancel();
      }
   }
}
