package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.hand.HandGlowModule;
import dev.venti.modules.hand.ViewmodelArmModule;
import dev.venti.modules.hand.ViewmodelModule;
import dev.venti.modules.hand.SwingAnimationModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person hand hooks shared by 3D Hand, Hand Aura, Hand Pose and Swing Animation. */
@Mixin(HeldItemRenderer.class)
public abstract class MixinHandHeldItemRenderer {
   private static final String RENDER_FIRST_PERSON_ITEM =
      "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";
   private static final String RENDER_ARM_HOLDING_ITEM =
      "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V";
   private static final String RENDER_ARM =
      "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V";

   // ---- Hand Aura: capture the hand pass into its own target, then composite it back with a glow.
   @WrapOperation(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V")
   )
   private void venti$handAura(RenderDispatcher features, Operation<Void> original) {
      if (HandGlowModule.active != null && HandGlowModule.beginCapture()) {
         boolean drawn = false;
         try {
            original.call(features);
            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
            drawn = true;
         } finally {
            HandGlowModule.endCapture(drawn);
         }
      } else {
         original.call(features);
      }
   }

   // ---- 3D Hand: stretch the bare arm up to the shoulder.
   @WrapOperation(
      method = {RENDER_ARM_HOLDING_ITEM, RENDER_ARM},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longRightArm(
      PlayerEntityRenderer<?> renderer, MatrixStack pose, OrderedRenderCommandQueue collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.push();
         ViewmodelArmModule.shoulder(pose, true, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.pop();
      }
   }

   @WrapOperation(
      method = {RENDER_ARM_HOLDING_ITEM, RENDER_ARM},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longLeftArm(
      PlayerEntityRenderer<?> renderer, MatrixStack pose, OrderedRenderCommandQueue collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.push();
         ViewmodelArmModule.shoulder(pose, false, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.pop();
      }
   }

   // ---- 3D Hand: draw the arm gripping the held item.
   @WrapOperation(
      method = RENDER_FIRST_PERSON_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
      )
   )
   private void venti$handModelItem(
      HeldItemRenderer renderer,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector,
      int light,
      Operation<Void> original,
      @Local(argsOnly = true) Hand hand
   ) {
      if (ViewmodelArmModule.active != null) {
         ViewmodelArmModule.renderArm(poseStack, entity, stack, context, collector, light, hand == Hand.MAIN_HAND);
      }

      original.call(renderer, entity, stack, context, poseStack, collector, light);
   }

   // ---- Hand Pose: offset, rotate and scale the whole hand right after the method's push().
   @Inject(
      method = RENDER_FIRST_PERSON_ITEM,
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER)
   )
   private void venti$handsPose(
      AbstractClientPlayerEntity player,
      float partialTick,
      float pitch,
      Hand hand,
      float swing,
      ItemStack stack,
      float equip,
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector,
      int light,
      CallbackInfo ci
   ) {
      if (ViewmodelModule.live != null) {
         boolean mainHand = hand == Hand.MAIN_HAND;
         Arm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         ViewmodelModule.shape(poseStack, mainHand, arm == Arm.RIGHT ? 1 : -1);
      }
   }

   // ---- Swing Animation: replace the vanilla swing transform.
   @WrapOperation(
      method = RENDER_FIRST_PERSON_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
      )
   )
   private void venti$swingStyle(
      HeldItemRenderer renderer, float progress, MatrixStack poseStack, int side, Arm arm, Operation<Void> original, @Local(argsOnly = true) Hand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == Hand.MAIN_HAND)) {
         module.pose(poseStack, progress, side);
      } else {
         original.call(renderer, progress, poseStack, side, arm);
      }
   }
}
