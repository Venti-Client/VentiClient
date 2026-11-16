package dev.venti.mixin.character;

import dev.venti.modules.character.CharacterLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityModel.class})
public abstract class MixinCharacterPlayerModel {
   //? if >=1.21.9 {
   @Inject(
      method = {"setArmAngle(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$characterHand(PlayerEntityRenderState state, Arm arm, MatrixStack poseStack, CallbackInfo ci) {
   //?} else {
   /*private PlayerEntityRenderState venti$posedState;

   // Before 1.21.9 setArmAngle gets no render state; use the one the model was last posed with (same frame, same player).
   @Inject(
      method = {"setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V"},
      at = {@At("HEAD")}
   )
   private void venti$rememberState(PlayerEntityRenderState state, CallbackInfo ci) {
      this.venti$posedState = state;
   }

   @Inject(
      method = {"setArmAngle(Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$characterHand(Arm arm, MatrixStack poseStack, CallbackInfo ci) {
      PlayerEntityRenderState state = this.venti$posedState;
      if (state == null) {
         return;
      }
   *///?}
      if (CharacterLayer.translateToHand((PlayerEntityModel)(Object)this, state, arm, poseStack)) {
         ci.cancel();
      }
   }
}
