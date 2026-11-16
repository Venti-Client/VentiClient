package dev.venti.modules.hand;

import dev.venti.config.ActionSetting;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Hand Pose: moves, rotates and resizes your first-person hands, gliding between poses (ported from Anubis by 4ldenz). */
public final class HandPoseModule extends Module {
   public static volatile HandPoseModule live;
   private static final String MAIN = "Main Hand";
   private static final String OFF = "Offhand";
   private static final float PIVOT_X = 0.56F;
   private static final float PIVOT_Y = -0.52F;
   private static final float PIVOT_Z = -0.72F;
   private static final float[] REST = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
   private static final int X = 0;
   private static final int Y = 1;
   private static final int Z = 2;
   private static final int PITCH = 3;
   private static final int YAW = 4;
   private static final int ROLL = 5;
   private static final int SCALE = 6;
   private static final float SETTLED = 0.001F;
   private static final float MAX_STEP = 0.1F;
   private static final float SLOWEST_RATE = 3.0F;
   private static final float FASTEST_RATE = 30.0F;
   private final BooleanSetting link = this.add("link-hands", new BooleanSetting("Link Hands", false));
   private final ModeSetting editing = this.add("editing", new ModeSetting("Editing", MAIN, MAIN, OFF));
   private final NumberSetting[] main = this.axes("main", true);
   private final NumberSetting[] off = this.axes("off", false);
   private final NumberSetting glide = this.add("glide", new NumberSetting("Glide", 0.4F, 0.0F, 1.0F).step(0.05F));
   private final HandState mainState = new HandState();
   private final HandState offState = new HandState();

   public HandPoseModule() {
      super("viewmodel", "Viewmodel", "Moves, rotates and resizes your first-person hands.", ModuleCategory.VISUALS);
      this.editing.visibleWhen(() -> !this.link.get());
      this.add("reset-hand", new ActionSetting("Reset Hand", this::resetEdited));
   }

   private NumberSetting[] axes(String prefix, boolean mainHand) {
      NumberSetting[] axes = new NumberSetting[]{
         this.add(prefix + "-x", new NumberSetting("Offset X", REST[X], -1.0F, 1.0F).step(0.01F)),
         this.add(prefix + "-y", new NumberSetting("Offset Y", REST[Y], -1.0F, 1.0F).step(0.01F)),
         this.add(prefix + "-z", new NumberSetting("Offset Z", REST[Z], -1.0F, 1.0F).step(0.01F)),
         this.add(prefix + "-pitch", new NumberSetting("Pitch", REST[PITCH], -180.0F, 180.0F).step(1.0F)),
         this.add(prefix + "-yaw", new NumberSetting("Yaw", REST[YAW], -180.0F, 180.0F).step(1.0F)),
         this.add(prefix + "-roll", new NumberSetting("Roll", REST[ROLL], -180.0F, 180.0F).step(1.0F)),
         this.add(prefix + "-scale", new NumberSetting("Scale", REST[SCALE], 0.3F, 2.0F).step(0.05F))
      };

      for (NumberSetting axis : axes) {
         axis.visibleWhen(mainHand ? () -> this.link.get() || this.editing.is(MAIN) : () -> !this.link.get() && this.editing.is(OFF));
      }

      return axes;
   }

   @Override
   protected void onEnable() {
      this.mainState.stamp = 0L;
      this.offState.stamp = 0L;
      live = this;
   }

   // No onDisable: the hands glide back to rest and shapeHand() clears `live` once both have settled.

   private void resetEdited() {
      NumberSetting[] axes = !this.link.get() && !this.editing.is(MAIN) ? this.off : this.main;
      for (int i = 0; i < axes.length; i++) {
         axes[i].set(REST[i]);
      }
   }

   public static void shape(MatrixStack pose, boolean mainHand, int side) {
      HandPoseModule module = live;
      if (module != null) {
         module.shapeHand(pose, mainHand, side);
      }
   }

   private void shapeHand(MatrixStack pose, boolean mainHand, int side) {
      boolean on = this.isEnabled();
      HandState state = mainHand ? this.mainState : this.offState;
      state.step(!mainHand && !this.link.get() ? this.off : this.main, on, this.follow());
      if (!on && this.mainState.rested && this.offState.rested) {
         live = null;
      } else if (!state.rested) {
         float[] v = state.value;
         pose.translate(side * (PIVOT_X + v[X]), PIVOT_Y + v[Y], PIVOT_Z + v[Z]);
         pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * v[YAW]));
         pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(v[PITCH]));
         pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * v[ROLL]));
         pose.scale(v[SCALE], v[SCALE], v[SCALE]);
         pose.translate(-side * PIVOT_X, -PIVOT_Y, -PIVOT_Z);
      }
   }

   private float follow() {
      float g = this.glide.get();
      return g <= 0.0F ? -1.0F : FASTEST_RATE + (SLOWEST_RATE - FASTEST_RATE) * g;
   }

   private static final class HandState {
      final float[] value = REST.clone();
      long stamp;
      boolean rested = true;

      void step(NumberSetting[] target, boolean on, float rate) {
         long now = System.nanoTime();
         float dt = this.stamp == 0L ? 0.0F : Math.min(MAX_STEP, (float)(now - this.stamp) / 1.0E9F);
         this.stamp = now;
         float k = rate < 0.0F ? 1.0F : 1.0F - (float)Math.exp(-rate * dt);
         boolean settled = true;

         for (int i = 0; i < this.value.length; i++) {
            float goal = on ? target[i].get() : REST[i];
            float next = this.value[i] + (goal - this.value[i]) * k;
            if (Math.abs(goal - next) < SETTLED) {
               next = goal;
            }

            this.value[i] = next;
            if (next != REST[i]) {
               settled = false;
            }
         }

         this.rested = settled;
      }
   }
}
