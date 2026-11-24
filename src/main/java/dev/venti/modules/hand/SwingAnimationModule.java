package dev.venti.modules.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.util.Arrays;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Swing Animation: replaces the first-person swing with one of several styles (ported from Anubis by 4ldenz). */
public final class SwingAnimationModule extends Module {
   public static volatile SwingAnimationModule active;
   private static final String KHOPESH = "Slash";
   private static final String ANKH = "Chop";
   private static final String SICKLE = "Hook";
   private static final String SCARAB = "Spin";
   private static final String HORUS = "Double Tap";
   private static final String SPHINX = "Push";
   private static final long COMBAT_WINDOW_MS = 3000L;
   private final ModeSetting style = this.add("style", new ModeSetting("Style", KHOPESH, KHOPESH, ANKH, SICKLE, SCARAB, HORUS, SPHINX));
   private final NumberSetting tempo = this.add("tempo", new NumberSetting("Tempo", 1.0F, 0.4F, 2.5F).step(0.05F));
   private final NumberSetting intensity = this.add("intensity", new NumberSetting("Intensity", 1.0F, 0.2F, 2.0F).step(0.05F));
   private final BooleanSetting offhand = this.add("offhand", new BooleanSetting("Offhand Too", true));
   private final BooleanSetting combatOnly = this.add("combat-only", new BooleanSetting("Combat Only", false));
   private int styleIndex;
   private boolean styled;
   // Replaces Anubis's CombatTarget.lastHit(): the entity the local player last attacked, and when.
   private static Entity lastHit;
   private static long lastHitAt;

   public SwingAnimationModule() {
      super("swing-animation", "Swing Animation", "Changes your first-person swing animation.", ModuleCategory.VISUALS);
      this.style.onChange(this::cacheStyle);
      this.cacheStyle();
      AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
         if (world.isClientSide() && player == Minecraft.getInstance().player) {
            lastHit = entity;
            lastHitAt = System.currentTimeMillis();
         }

         return InteractionResult.PASS;
      });
   }

   @Override
   protected void onEnable() {
      this.latch();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   private void cacheStyle() {
      this.styleIndex = Math.max(0, Arrays.asList(this.style.modes()).indexOf(this.style.get()));
   }

   public void latch() {
      this.styled = !this.combatOnly.get() || inCombat();
   }

   private static boolean inCombat() {
      Entity hit = lastHit;
      if (hit != null && System.currentTimeMillis() - lastHitAt <= COMBAT_WINDOW_MS && hit.isAlive() && !hit.isRemoved()) {
         return true;
      }

      Entity looked = Minecraft.getInstance().crosshairPickEntity;
      return (looked instanceof Player || looked instanceof Enemy) && looked instanceof LivingEntity && looked.isAlive();
   }

   public boolean covers(boolean mainHand) {
      return (mainHand || this.offhand.get()) && this.styled;
   }

   public int swingTicks(int ticks) {
      return !this.styled ? ticks : Math.max(1, Math.round(ticks / this.tempo.get()));
   }

   //? if >=26.3 {
   /*// 26.3: the swing duration for a swing that is about to start, with the style it will latch.
   public int latchedSwingTicks(int ticks) {
      boolean styled = !this.combatOnly.get() || inCombat();
      return !styled ? ticks : Math.max(1, Math.round(ticks / this.tempo.get()));
   }

   *///?}
   public void pose(PoseStack pose, float progress, int side) {
      if (progress > 0.0F && progress < 1.0F) {
         float k = this.intensity.get();
         switch (this.styleIndex) {
            case 1 -> ankh(pose, progress, side, k);
            case 2 -> sickle(pose, progress, side, k);
            case 3 -> scarab(pose, progress, side, k);
            case 4 -> horus(pose, progress, side, k);
            case 5 -> sphinx(pose, progress, side, k);
            default -> khopesh(pose, progress, side, k);
         }
      }
   }

   private static void khopesh(PoseStack pose, float t, int side, float k) {
      float a = crest(t, 0.3F) * k;
      pose.translate(side * -0.3F * a, 0.05F * Mth.sin((float)Math.PI * t) * k, -0.1F * a);
      pose.mulPose(Axis.YP.rotationDegrees(side * 64.0F * a));
      pose.mulPose(Axis.ZP.rotationDegrees(side * 48.0F * a));
      pose.mulPose(Axis.XP.rotationDegrees(-14.0F * a));
   }

   private static void ankh(PoseStack pose, float t, int side, float k) {
      float raise = t < 0.28F ? Mth.sin((float)Math.PI * t / 0.28F) * k : 0.0F;
      float strike = t <= 0.18F ? 0.0F : crest((t - 0.18F) / 0.82F, 0.28F) * k;
      pose.translate(side * -0.06F * strike, 0.16F * raise - 0.1F * strike, 0.06F * raise - 0.24F * strike);
      pose.mulPose(Axis.XP.rotationDegrees(30.0F * raise - 105.0F * strike));
      pose.mulPose(Axis.ZP.rotationDegrees(side * 10.0F * strike));
   }

   private static void sickle(PoseStack pose, float t, int side, float k) {
      float a = crest(t, 0.42F);
      float hook = a * a * k;
      a *= k;
      pose.translate(side * -0.1F * a, -0.24F * a, -0.06F * a);
      pose.mulPose(Axis.ZP.rotationDegrees(side * -100.0F * a));
      pose.mulPose(Axis.YP.rotationDegrees(side * 30.0F * hook));
      pose.mulPose(Axis.XP.rotationDegrees(-25.0F * a));
   }

   private static void scarab(PoseStack pose, float t, int side, float k) {
      float turn = (float)(Math.PI * 2) * t;
      float cos = Mth.cos(turn);
      float sin = Mth.sin(turn);
      float body = Mth.sin((float)Math.PI * t) * k;
      float r = 0.17F * k;
      pose.translate(side * r * 0.6F * (cos - 1.0F), r * sin, -0.12F * body);
      pose.mulPose(Axis.ZP.rotationDegrees(side * 40.0F * sin * k));
      pose.mulPose(Axis.XP.rotationDegrees(-40.0F * body));
   }

   private static void horus(PoseStack pose, float t, int side, float k) {
      float beat = Mth.sin((float)(Math.PI * 2) * t);
      float w = beat * beat * (t < 0.5F ? 1.0F : 0.65F) * k;
      pose.translate(side * -0.08F * w, 0.06F * w, -0.1F * w);
      pose.mulPose(Axis.YP.rotationDegrees(side * 26.0F * w));
      pose.mulPose(Axis.XP.rotationDegrees(-50.0F * w));
      pose.mulPose(Axis.ZP.rotationDegrees(side * -22.0F * w));
   }

   private static void sphinx(PoseStack pose, float t, int side, float k) {
      float a = (0.5F - 0.5F * Mth.cos((float)(Math.PI * 2) * t)) * k;
      pose.translate(0.0F, -0.06F * a, -0.1F * a);
      pose.mulPose(Axis.XP.rotationDegrees(-24.0F * a));
      pose.mulPose(Axis.YP.rotationDegrees(side * 10.0F * a));
   }

   private static float crest(float t, float peak) {
      if (t <= 0.0F || t >= 1.0F) {
         return 0.0F;
      } else if (t < peak) {
         float u = 1.0F - t / peak;
         return 1.0F - u * u * u;
      } else {
         float u = (t - peak) / (1.0F - peak);
         return 1.0F - u * u * (3.0F - 2.0F * u);
      }
   }
}
