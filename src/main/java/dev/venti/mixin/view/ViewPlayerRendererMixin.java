package dev.venti.mixin.view;

import dev.venti.module.Modules;
import dev.venti.modules.view.NametagState;
import dev.venti.modules.view.NametagsModule;
import dev.venti.modules.view.SkinChangeModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
*///?}
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//? if >=1.21.9 {
import net.minecraft.client.render.state.CameraRenderState;
//?} else {
/*
*///?}
import net.minecraft.client.util.math.MatrixStack;
//? if >=1.21.9 {
import net.minecraft.entity.PlayerLikeEntity;
//?} else {
/*
*///?}
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
      //? if >=1.21.9 {
      method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void venti$updateState(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
      //?} else {
      /*method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void venti$updateState(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
      *///?}
      SkinChangeModule skins = Modules.get(SkinChangeModule.class);
      if (skins != null && skins.isEnabled()) {
         state.skinTextures = skins.skinFor(state.id, state.skinTextures);
      }

      if (entity instanceof AbstractClientPlayerEntity player) {
         ((NametagState)state).venti$setNametag(NametagsModule.capture(player, state, tickProgress));
      }
   }

   @Inject(
      //? if >=1.21.9 {
      method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$nametag(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
      //?} else {
      /*method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$nametag(PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider queue, int light, CallbackInfo ci) {
      *///?}
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
         //? if >=1.21.9 {
         NametagsModule.render(state, tag, matrices, queue, cameraState);
         //?} else {
         /*NametagsModule.render(state, tag, matrices, queue, light);
         *///?}
      }
   }
}
