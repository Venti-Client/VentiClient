package dev.venti.mixin.character;

import dev.venti.modules.character.CharacterLayer;
import dev.venti.modules.character.AvatarModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.network.ClientPlayerLikeEntity;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
*///?}
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
//? if >=1.21.9 {
public abstract class MixinCharacterAvatarRenderer<T extends PlayerLikeEntity & ClientPlayerLikeEntity> extends LivingEntityRenderer<T, PlayerEntityRenderState, PlayerEntityModel> {
//?} else {
/*public abstract class MixinCharacterAvatarRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
*///?}
   protected MixinCharacterAvatarRenderer(Context context, PlayerEntityModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void venti$addCharacterLayer(Context context, boolean slim, CallbackInfo ci) {
      this.addFeature(new CharacterLayer(this));
   }

   //? if >=1.21.9 {
   @Inject(
      method = {"updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void venti$extractCharacter(PlayerLikeEntity entity, PlayerEntityRenderState state, float partialTick, CallbackInfo ci) {
   //?} else {
   /*@Inject(
      method = {"updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void venti$extractCharacter(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float partialTick, CallbackInfo ci) {
   *///?}
      AvatarModule.extract(entity, state);
   }

   @Inject(
      method = {"renderArm"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$characterFirstPersonHand(
      //? if >=1.21.9 {
      MatrixStack poseStack, OrderedRenderCommandQueue collector, int light, Identifier skin, ModelPart arm, boolean sleeve, CallbackInfo ci
      //?} else {
      /*MatrixStack poseStack, VertexConsumerProvider collector, int light, Identifier skin, ModelPart arm, boolean sleeve, CallbackInfo ci
      *///?}
   ) {
      if (CharacterLayer.firstPersonHand((PlayerEntityModel)this.getModel(), poseStack, collector, light, arm)) {
         ci.cancel();
      }
   }
}
