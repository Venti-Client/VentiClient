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
   @Inject(
      method = {"setArmAngle(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$characterHand(PlayerEntityRenderState state, Arm arm, MatrixStack poseStack, CallbackInfo ci) {
      if (CharacterLayer.translateToHand((PlayerEntityModel)(Object)this, state, arm, poseStack)) {
         ci.cancel();
      }
   }
}
