package dev.venti.mixin.view;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.module.Modules;
import dev.venti.modules.view.NametagState;
import dev.venti.modules.view.NametagsModule;
import dev.venti.modules.view.SkinChangeModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Avatar;
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
@Mixin(AvatarRenderer.class)
public abstract class ViewPlayerRendererMixin {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void venti$updateState(Avatar entity, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      SkinChangeModule skins = Modules.get(SkinChangeModule.class);
      if (skins != null && skins.isEnabled()) {
         state.skin = skins.skinFor(state.id, state.skin);
      }

      if (entity instanceof AbstractClientPlayer player) {
         ((NametagState)state).venti$setNametag(NametagsModule.capture(player, state, tickProgress));
      }
   }

   @Inject(
      method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void venti$nametag(AvatarRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
      if (!NametagsModule.active() || state.nameTag == null) {
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
