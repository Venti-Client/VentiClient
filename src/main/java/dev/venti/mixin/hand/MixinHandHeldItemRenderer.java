//? if >=26.3 {
/*package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.hand.SwingAnimationModule;
import dev.venti.modules.hand.ViewmodelArmModule;
import dev.venti.modules.hand.ViewmodelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// First-person hand hooks shared by 3D Hand, Hand Pose and Swing Animation. 26.3 renamed ItemInHandRenderer to
// FirstPersonHandsAndItemsRenderer and draws from extracted render state; Hand Aura is in MixinHandAuraGameRenderer.
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinHandHeldItemRenderer {
   private static final String SUBMIT_ARM_WITH_ITEM =
      "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";
   // Both the bare arm and the map hands draw through this method.
   private static final String RENDER_PLAYER_HAND =
      "renderPlayerHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;)V";

   // ---- 3D Hand: stretch the bare arm up to the shoulder.
   @WrapOperation(
      method = RENDER_PLAYER_HAND,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )
   )
   private void venti$longRightArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.pushPose();
         ViewmodelArmModule.shoulder(pose, true, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }

   @WrapOperation(
      method = RENDER_PLAYER_HAND,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )
   )
   private void venti$longLeftArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.pushPose();
         ViewmodelArmModule.shoulder(pose, false, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }

   // ---- 3D Hand: draw the arm gripping the held item (26.3 submits the extracted item state directly).
   @WrapOperation(
      method = SUBMIT_ARM_WITH_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
      )
   )
   private void venti$handModelItem(
      ItemStackRenderState item,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      int overlay,
      int outline,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true) ItemStack stack
   ) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (ViewmodelArmModule.active != null && player != null) {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         ItemDisplayContext context = arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         ViewmodelArmModule.renderArm(poseStack, player, stack, context, collector, light, mainHand);
      }

      original.call(item, poseStack, collector, light, overlay, outline);
   }

   // ---- Hand Pose: offset, rotate and scale the whole hand right after the method's push().
   @Inject(
      method = SUBMIT_ARM_WITH_ITEM,
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
   )
   private void venti$handsPose(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTicks,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack stack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      if (ViewmodelModule.live != null && playerState.avatarRenderState != null) {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm mainArm = playerState.avatarRenderState.mainArm;
         HumanoidArm arm = mainHand ? mainArm : mainArm.getOpposite();
         ViewmodelModule.shape(poseStack, mainHand, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }

   // ---- Swing Animation: replace the vanilla swing transform.
   @WrapOperation(
      method = SUBMIT_ARM_WITH_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )
   )
   private void venti$swingStyle(
      FirstPersonHandsAndItemsRenderer renderer,
      float progress,
      PoseStack poseStack,
      int side,
      HumanoidArm arm,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == InteractionHand.MAIN_HAND)) {
         module.pose(poseStack, progress, side);
      } else {
         original.call(renderer, progress, poseStack, side, arm);
      }
   }
}
*///?} else {
package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.modules.hand.HandGlowModule;
import dev.venti.modules.hand.SwingAnimationModule;
import dev.venti.modules.hand.ViewmodelArmModule;
import dev.venti.modules.hand.ViewmodelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person hand hooks shared by 3D Hand, Hand Aura, Hand Pose and Swing Animation. */
@Mixin(ItemInHandRenderer.class)
public abstract class MixinHandHeldItemRenderer {
   //? if >=26.2 {
   private static final String RENDER_ARM_WITH_ITEM =
      "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";
   //?} else {
   /*private static final String RENDER_ARM_WITH_ITEM =
      "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";
   *///?}
   private static final String RENDER_PLAYER_ARM =
      "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V";
   private static final String RENDER_MAP_HAND =
      "renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/entity/HumanoidArm;)V";

   // ---- Hand Aura: capture the hand pass into its own target, then composite it back with a glow.
   // (26.x draws the hand pass in GameRenderer: MixinHandAuraGameRenderer.)
   //? if <26.2 {
   /*@WrapOperation(
      method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V")
   )
   private void venti$handAura(FeatureRenderDispatcher features, Operation<Void> original) {
      if (HandGlowModule.active != null && HandGlowModule.beginCapture()) {
         boolean drawn = false;
         try {
            original.call(features);
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            drawn = true;
         } finally {
            HandGlowModule.endCapture(drawn);
         }
      } else {
         original.call(features);
      }
   }
   *///?}

   // ---- 3D Hand: stretch the bare arm up to the shoulder.
   @WrapOperation(
      method = {RENDER_PLAYER_ARM, RENDER_MAP_HAND},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )
   )
   private void venti$longRightArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.pushPose();
         ViewmodelArmModule.shoulder(pose, true, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }

   @WrapOperation(
      method = {RENDER_PLAYER_ARM, RENDER_MAP_HAND},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
      )
   )
   private void venti$longLeftArm(
      AvatarRenderer<?> renderer, PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original
   ) {
      original.call(renderer, pose, collector, light, skin, sleeve);
      if (ViewmodelArmModule.active != null) {
         pose.pushPose();
         ViewmodelArmModule.shoulder(pose, false, renderer);
         original.call(renderer, pose, collector, light, skin, sleeve);
         pose.popPose();
      }
   }

   // ---- 3D Hand: draw the arm gripping the held item.
   @WrapOperation(
      method = RENDER_ARM_WITH_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void venti$handModelItem(
      ItemInHandRenderer renderer,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      Operation<Void> original,
      @Local(argsOnly = true) InteractionHand hand
   ) {
      if (ViewmodelArmModule.active != null) {
         ViewmodelArmModule.renderArm(poseStack, entity, stack, context, collector, light, hand == InteractionHand.MAIN_HAND);
      }

      original.call(renderer, entity, stack, context, poseStack, collector, light);
   }

   // ---- Hand Pose: offset, rotate and scale the whole hand right after the method's push().
   @Inject(
      method = RENDER_ARM_WITH_ITEM,
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
   )
   private void venti$handsPose(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swing,
      ItemStack stack,
      float equip,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      if (ViewmodelModule.live != null) {
         boolean mainHand = hand == InteractionHand.MAIN_HAND;
         HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
         ViewmodelModule.shape(poseStack, mainHand, arm == HumanoidArm.RIGHT ? 1 : -1);
      }
   }

   // ---- Swing Animation: replace the vanilla swing transform.
   @WrapOperation(
      method = RENDER_ARM_WITH_ITEM,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )
   )
   private void venti$swingStyle(
      ItemInHandRenderer renderer, float progress, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original, @Local(argsOnly = true) InteractionHand hand
   ) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && module.covers(hand == InteractionHand.MAIN_HAND)) {
         module.pose(poseStack, progress, side);
      } else {
         original.call(renderer, progress, poseStack, side, arm);
      }
   }
}
//?}
