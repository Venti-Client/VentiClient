package dev.venti.mixin.world;

import dev.venti.modules.world.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinJumpCirclesLevelRenderer {
   @Inject(
      method = {"renderBlockDamage"},
      at = {@At("HEAD")}
   )
   private void venti$jumpCircles(MatrixStack poseStack, Immediate crumblingBuffers, WorldRenderState levelRenderState, CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.renderLevel(levelRenderState.cameraRenderState);
      }
   }
}
