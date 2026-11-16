package dev.venti.modules.hand;

import dev.venti.mixin.hand.AccessorHandItemLayer;
import dev.venti.mixin.hand.AccessorHandItemRenderState;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
*///?}
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
//? if >=1.21.9 {
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
//?} else {
/*import net.minecraft.client.util.SkinTextures;
*///?}
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Places a first-person arm so its fist closes on the held item's handle (ported from Anubis). */
final class FirstPersonArm {
   static final Vector3f FOREARM = new Vector3f(0.75F, -0.45F, 0.5F).normalize();
   static final float ELBOW = 0.6F;
   private static final Vector3f ARM_UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final float HANDLE_INSET = 0.1875F;
   private static final float BLOCK_BELOW = 0.17F;
   private static final Vector3f BLOCK_SHIFT = new Vector3f(0.09F, 0.0F, 0.06F);
   private static final ItemRenderState MODEL = new ItemRenderState();

   private FirstPersonArm() {
   }

   static Arm arm() {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity player = mc.player;
      if (player == null || player.isInvisible()) {
         return null;
      }

      //? if >=1.21.9 {
      EntityRenderManager dispatcher = mc.getEntityRenderDispatcher();
      PlayerEntityRenderer<AbstractClientPlayerEntity> renderer = dispatcher.getPlayerRenderer(player);
      SkinTextures skin = player.getSkin();
      return new Arm(
         renderer,
         skin.body().texturePath(),
         player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE),
         player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE),
         skin.model() == PlayerSkinType.SLIM
      );
      //?} else {
      /*EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
      PlayerEntityRenderer renderer = (PlayerEntityRenderer)dispatcher.getRenderer(player);
      SkinTextures skin = player.getSkinTextures();
      return new Arm(
         renderer,
         skin.texture(),
         player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE),
         player.isPartVisible(PlayerModelPart.LEFT_SLEEVE),
         skin.model() == SkinTextures.Model.SLIM
      );
      *///?}
   }

   static void hold(
      //? if >=1.21.9 {
      MatrixStack pose, Arm arm, ItemStack stack, ItemDisplayContext context, LivingEntity holder, OrderedRenderCommandQueue collector, int light, float dx, float dy, float dz
      //?} else {
      /*MatrixStack pose, Arm arm, ItemStack stack, ItemDisplayContext context, LivingEntity holder, VertexConsumerProvider collector, int light, float dx, float dy, float dz
      *///?}
   ) {
      Vector3f grip = grip(stack, context, holder);
      if (grip != null) {
         Vector3f elbow = new Vector3f(grip).fma(ELBOW, FOREARM);
         grip.add(dx, dy, dz);
         boolean left = context.isLeftHand();
         pose.push();
         if (left) {
            pose.scale(-1.0F, 1.0F, 1.0F);
         }

         place(pose, grip, elbow, arm.slim(), posed -> arm.draw(posed, left, collector, light));
         pose.pop();
      }
   }

   static Vector3f grip(ItemStack stack, ItemDisplayContext context, LivingEntity holder) {
      if (stack.isEmpty()
         || stack.isOf(Items.BOW)
         || stack.isOf(Items.CROSSBOW)
         || stack.isOf(Items.SHIELD)
         || stack.isOf(Items.TRIDENT)
         || stack.isOf(Items.SPYGLASS)) {
         return null;
      }

      //? if >=1.21.9 {
      MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(MODEL, stack, context, holder.getEntityWorld(), holder, holder.getId() + context.ordinal());
      //?} else {
      /*MinecraftClient.getInstance().getItemModelManager().clearAndUpdate(MODEL, stack, context, holder.getWorld(), holder, holder.getId() + context.ordinal());
      *///?}
      AccessorHandItemRenderState state = (AccessorHandItemRenderState)MODEL;
      if (state.venti$handLayerCount() <= 0) {
         return null;
      }

      AccessorHandItemLayer layer = (AccessorHandItemLayer)state.venti$handLayers()[0];
      Transformation transform = layer.venti$handTransform();
      MatrixStack.Entry display = new MatrixStack.Entry();
      if (transform != null) {
         transform.apply(context.isLeftHand(), display);
      }

      Matrix4f m = display.getPositionMatrix();
      if (!MODEL.isSideLit()) {
         Supplier<Vector3fc[]> extents = layer.venti$handExtents();
         float[] handle = handle(extents != null ? extents.get() : null);
         Vector3f grip = m.transformPosition(handle[0] - 0.5F, handle[1] - 0.5F, 0.0F, new Vector3f());
         if (context.isLeftHand()) {
            grip.x = -grip.x;
         }

         return grip;
      } else if (!(stack.getItem() instanceof BlockItem)) {
         return null;
      } else {
         Vector3f center = m.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
         if (context.isLeftHand()) {
            center.x = -center.x;
         }

         return blockGrip(center, transform != null ? transform.scale().y() : 1.0F);
      }
   }

   static Vector3f blockGrip(Vector3f center, float scale) {
      return new Vector3f(center).add(BLOCK_SHIFT).sub(0.0F, 0.5F * scale + BLOCK_BELOW, 0.0F);
   }

   static float[] handle(Vector3fc[] extents) {
      float lowest = Float.MAX_VALUE;
      if (extents != null) {
         for (Vector3fc p : extents) {
            if (!squareCorner(p)) {
               lowest = Math.min(lowest, p.x() + p.y());
            }
         }
      }

      if (lowest == Float.MAX_VALUE) {
         return new float[]{HANDLE_INSET, HANDLE_INSET};
      }

      float x = 0.0F;
      float y = 0.0F;
      int tied = 0;
      for (Vector3fc p : extents) {
         if (!squareCorner(p) && !(p.x() + p.y() > lowest + 1.0E-4F)) {
            x += p.x();
            y += p.y();
            tied++;
         }
      }

      return new float[]{x / tied + HANDLE_INSET, y / tied + HANDLE_INSET};
   }

   private static boolean squareCorner(Vector3fc p) {
      return (p.x() < 1.0E-4F || p.x() > 0.9999F) && (p.y() < 1.0E-4F || p.y() > 0.9999F);
   }

   static void place(MatrixStack pose, Vector3f hand, Vector3f elbow, boolean slim, Consumer<MatrixStack> draw) {
      Vector3f along = new Vector3f(hand).sub(elbow).normalize();
      Vector3f front = new Vector3f(ARM_UP).fma(-ARM_UP.dot(along), along).normalize();
      Vector3f side = new Vector3f(along).cross(front);
      pose.push();
      pose.translate(hand.x, hand.y, hand.z);
      pose.multiply(new Quaternionf().setFromNormalized(new Matrix3f(side, along, front)));
      drawPart(pose, slim, draw);
      pose.translate(0.0F, -0.625F, 0.0F);
      pose.scale(-1.0F, -3.0F, 1.0F);
      pose.translate(0.0F, 0.625F, 0.0F);
      drawPart(pose, slim, draw);
      pose.pop();
   }

   private static void drawPart(MatrixStack pose, boolean slim, Consumer<MatrixStack> draw) {
      pose.push();
      pose.translate(slim ? 0.03125F : 0.0625F, -0.5F, 0.0F);
      pose.multiply(RotationAxis.POSITIVE_Z.rotation(-0.1F));
      pose.translate(0.3125F, -0.125F, 0.0F);
      draw.accept(pose);
      pose.pop();
   }

   static void shoulder(MatrixStack pose, boolean right, boolean slim) {
      float pivotX = right ? -0.3125F : 0.3125F;
      float pivotY = 0.125F;
      float tilt = right ? 0.1F : -0.1F;
      float middle = (right ? -1.0F : 1.0F) * (slim ? 0.5F : 1.0F) / 16.0F;
      float top = -0.125F;
      pose.translate(pivotX, pivotY, 0.0F);
      pose.multiply(RotationAxis.POSITIVE_Z.rotation(tilt));
      pose.translate(middle, top, 0.0F);
      pose.scale(-1.0F, -3.0F, 1.0F);
      pose.translate(-middle, -top, 0.0F);
      pose.multiply(RotationAxis.POSITIVE_Z.rotation(-tilt));
      pose.translate(-pivotX, -pivotY, 0.0F);
   }

   //? if >=1.21.9 {
   record Arm(PlayerEntityRenderer<AbstractClientPlayerEntity> renderer, Identifier texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(MatrixStack pose, boolean left, OrderedRenderCommandQueue collector, int light) {
   //?} else {
   /*record Arm(PlayerEntityRenderer renderer, Identifier texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(MatrixStack pose, boolean left, VertexConsumerProvider collector, int light) {
   *///?}
         if (left) {
            pose.scale(-1.0F, 1.0F, 1.0F);
            this.renderer.renderLeftArm(pose, collector, light, this.texture, this.leftSleeve);
         } else {
            this.renderer.renderRightArm(pose, collector, light, this.texture, this.rightSleeve);
         }
      }
   }
}
