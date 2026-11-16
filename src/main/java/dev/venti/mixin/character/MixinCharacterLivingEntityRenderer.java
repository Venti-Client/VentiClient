package dev.venti.mixin.character;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
*///?}
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntityRenderer.class})
public abstract class MixinCharacterLivingEntityRenderer {
   @Inject(
      method = {"getRenderLayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$hideModelUnderCharacter(
      LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderLayer> cir
   ) {
      if (CharacterModule.hidesModel(state)) {
         cir.setReturnValue(null);
      }
   }

   //? if >=1.21.9 {
   @WrapOperation(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"
      )}
   )
   private void venti$hideLayersUnderCharacter(
      FeatureRenderer<?, ?> layer, MatrixStack poseStack, OrderedRenderCommandQueue collector, int light, EntityRenderState state, float yRot, float xRot, Operation<Void> original
   ) {
   //?} else {
   /*@WrapOperation(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"
      )}
   )
   private void venti$hideLayersUnderCharacter(
      FeatureRenderer<?, ?> layer, MatrixStack poseStack, VertexConsumerProvider collector, int light, EntityRenderState state, float yRot, float xRot, Operation<Void> original
   ) {
   *///?}
      if (!CharacterModule.hidesLayer(layer, state)) {
         original.call(new Object[]{layer, poseStack, collector, light, state, yRot, xRot});
      }
   }
}
