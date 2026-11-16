package dev.venti.mixin.view;

import dev.venti.module.Modules;
import dev.venti.modules.view.NametagState;
import dev.venti.modules.view.NametagsModule;
import dev.venti.modules.view.SkinChangeModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skin Change (swap the skin in the render state) and Nametags.
 *
 * <p>Nametags hooks vanilla's own label path: it captures data only when vanilla set {@code displayName} (vanilla's
 * nametag decision), and replaces the label drawing only for those players. Nothing is drawn for players vanilla would
 * not label, and the restyled tag is depth-tested and hidden when blocks are in the way.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class ViewPlayerRendererMixin {
   @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void venti$updateState(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
      SkinChangeModule skins = Modules.get(SkinChangeModule.class);
      if (skins != null && skins.isEnabled()) {
         state.skinTextures = skins.skinFor(state.id, state.skinTextures);
      }

      if (entity instanceof AbstractClientPlayerEntity player) {
         ((NametagState)state).venti$setNametag(NametagsModule.capture(player, state, tickProgress));
      }
   }

   @Inject(
      method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$nametag(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
      if (!NametagsModule.active() || state.displayName == null) {
         return;
      }

      NametagsModule.Tag tag = ((NametagState)state).venti$nametag();
      if (tag == null) {
         return;
      }

      // From here the module owns this label: vanilla's see-through pass and the below-name scoreboard line are not drawn.
      ci.cancel();
      if (tag.visible()) {
         NametagsModule.render(state, tag, matrices, queue, cameraState);
      }
   }
}
