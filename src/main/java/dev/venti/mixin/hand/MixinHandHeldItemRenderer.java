package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.hand.HandAuraModule;
import dev.venti.modules.hand.HandModelModule;
import dev.venti.modules.hand.HandPoseModule;
import dev.venti.modules.hand.SwingAnimationModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
*///?}
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
//? if <1.21.11
//import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person hand hooks shared by 3D Hand, Hand Aura, Hand Pose and Swing Animation. */
@Mixin(HeldItemRenderer.class)
public abstract class MixinHandHeldItemRenderer {
   //? if >=1.21.9 {
   private static final String RENDER_FIRST_PERSON_ITEM =
      "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";
   private static final String RENDER_ARM_HOLDING_ITEM =
      "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V";
   private static final String RENDER_ARM =
      "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V";
   //?} else {
   /*private static final String RENDER_FIRST_PERSON_ITEM =
      "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
   private static final String RENDER_ARM_HOLDING_ITEM =
      "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V";
   private static final String RENDER_ARM =
      "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Arm;)V";
   *///?}

   // ---- Hand Aura: capture the hand pass into its own target, then composite it back with a glow.
   //? if >=1.21.9 {
   @WrapOperation(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V")
   )
   private void venti$handAura(RenderDispatcher features, Operation<Void> original) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean drawn = false;
         try {
            original.call(features);
            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
            drawn = true;
         } finally {
            HandAuraModule.endCapture(drawn);
         }
      } else {
         original.call(features);
      }
   }
   //?} else {
   /*// Before 1.21.9 the hands draw straight into the entity buffers while renderItem runs (and it flushes them at its end),
   // so the capture wraps the whole method.
   @WrapMethod(
      method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
   )
   private void venti$handAura(
      float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate buffers, ClientPlayerEntity player, int light, Operation<Void> original
   ) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean drawn = false;
         try {
            buffers.draw();
            original.call(tickProgress, matrices, buffers, player, light);
            buffers.draw();
            drawn = true;
         } finally {
            HandAuraModule.endCapture(drawn);
         }
      } else {
         original.call(tickProgress, matrices, buffers, player, light);
      }
   }
   *///?}

   // ---- 3D Hand: stretch the bare arm up to the shoulder.
   @WrapOperation(
      method = {RENDER_ARM_HOLDING_ITEM, RENDER_ARM},
      at = @At(
         value = "INVOKE",
         //? if >=1.21.9 {
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longRightArm(
      PlayerEntityRenderer<?> renderer, MatrixStack pose, OrderedRenderCommandQueue collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
         //?} else {
         /*target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longRightArm(
      PlayerEntityRenderer renderer, MatrixStack pose, VertexConsumerProvider collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
         *///?}
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (HandModelModule.active != null) {
         pose.push();
         HandModelModule.shoulder(pose, true, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.pop();
      }
   }

   @WrapOperation(
      method = {RENDER_ARM_HOLDING_ITEM, RENDER_ARM},
      at = @At(
         value = "INVOKE",
         //? if >=1.21.9 {
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longLeftArm(
      PlayerEntityRenderer<?> renderer, MatrixStack pose, OrderedRenderCommandQueue collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
         //?} else {
         /*target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;Z)V"
      )
   )
   private void venti$longLeftArm(
      PlayerEntityRenderer renderer, MatrixStack pose, VertexConsumerProvider collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
         *///?}
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (HandModelModule.active != null) {
         pose.push();
         HandModelModule.shoulder(pose, false, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.pop();
      }
   }

   // ---- 3D Hand: draw the arm gripping the held item.
   @WrapOperation(
      method = RENDER_FIRST_PERSON_ITEM,
      at = @At(
         value = "INVOKE",
         //? if >=1.21.9 {
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
         //?} else {
         /*target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
         *///?}
      )
   )
   private void venti$handModelItem(
      HeldItemRenderer renderer,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      //? if >=1.21.9 {
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector,
      int light,
      Operation<Void> original,
      //?} else {
      /*MatrixStack poseStack,
      VertexConsumerProvider collector,
      int light,
      Operation<Void> original,
      *///?}
      @Local(argsOnly = true) Hand hand
   ) {
      if (HandModelModule.active != null) {
         HandModelModule.renderArm(poseStack, entity, stack, context, collector, light, hand == Hand.MAIN_HAND);
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
      //? if >=1.21.9 {
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector,
      int light,
      CallbackInfo ci
      //?} else {
      /*MatrixStack poseStack,
      VertexConsumerProvider collector,
      int light,
      CallbackInfo ci
      *///?}
   ) {
      if (HandPoseModule.live != null) {
         boolean mainHand = hand == Hand.MAIN_HAND;
         Arm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         HandPoseModule.shape(poseStack, mainHand, arm == Arm.RIGHT ? 1 : -1);
      }
   }

   // ---- Swing Animation: replace the vanilla swing transform.
   @WrapOperation(
      method = RENDER_FIRST_PERSON_ITEM,
      at = @At(
         value = "INVOKE",
         //? if >=1.21.11 {
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
         //?} else {
         /*target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
         *///?}
      )
   )
   //? if >=1.21.11 {
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
   //?} else {
   /*private void venti$swingStyle(
      HeldItemRenderer renderer, float progress, float equipProgress, MatrixStack poseStack, int side, Arm arm, Operation<Void> original,
      @Local(argsOnly = true) Hand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == Hand.MAIN_HAND)) {
         // Before 1.21.11 swingArm also applies the equip offset, so keep that part.
         this.applyEquipOffset(poseStack, arm, equipProgress);
         module.pose(poseStack, progress, side);
      } else {
         original.call(renderer, progress, equipProgress, poseStack, side, arm);
      }
   }

   @Shadow
   private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
      throw new AssertionError();
   }
   *///?}
}
