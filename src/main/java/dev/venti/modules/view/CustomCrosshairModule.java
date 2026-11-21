package dev.venti.modules.view;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.Setting;
import dev.venti.gui.Theme;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Draws a configurable crosshair in place of the vanilla one (vanilla's own visibility rules still apply). */
public final class CustomCrosshairModule extends Module {
   public static volatile CustomCrosshairModule active;
   private static final int OUTLINE = 0x8C000000;
   private static final int TRACK = 0xCC161616;
   private static final float DIAGONAL = 0.70710677F;
   private final ModeSetting style = this.add("style", new ModeSetting("Style", "Cross", "Cross", "T", "X", "Dot", "Circle"));
   private final NumberSetting size = this.add("size", new NumberSetting("Size", 6.0F, 1.0F, 20.0F).step(1.0F));
   private final NumberSetting gap = this.add("gap", new NumberSetting("Gap", 2.0F, 0.0F, 16.0F).step(1.0F));
   private final NumberSetting thickness = this.add("thickness", new NumberSetting("Thickness", 2.0F, 1.0F, 5.0F).step(1.0F));
   private final BooleanSetting centerDot = this.add("center-dot", new BooleanSetting("Center Dot", false));
   private final BooleanSetting outline = this.add("outline", new BooleanSetting("Outline", true));
   private final ModeSetting colorMode = this.add("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));
   private final ColorSetting color = this.add("color", new ColorSetting("Custom Color", 0xFFF5F5F7));
   private final BooleanSetting targetTint = this.add("target-tint", new BooleanSetting("Target Tint", false));
   private final ColorSetting targetColor = this.add("target-color", new ColorSetting("Target Color", 0xFFFF4560));
   private final BooleanSetting dynamicGap = this.add("dynamic-gap", new BooleanSetting("Dynamic Gap", false));
   private final NumberSetting spread = this.add("spread", new NumberSetting("Spread", 4.0F, 1.0F, 12.0F).step(1.0F));
   private float tint;
   private float extraGap;
   private float barAlpha;
   private float barFill;
   private long lastFrame;

   public CustomCrosshairModule() {
      super("custom-crosshair", "Custom Crosshair", "Draws a configurable crosshair in place of the vanilla one.", ModuleCategory.VISUALS);
      this.gap.visibleWhen(this::hasArms);
      this.centerDot.visibleWhen(() -> !this.style.is("Dot"));
      this.color.visibleWhen(() -> this.colorMode.is("Custom"));
      this.targetColor.visibleWhen(this.targetTint::get);
      this.spread.visibleWhen(() -> this.dynamicGap.get() && !this.style.is("Dot"));
   }

   @Override
   protected void onEnable() {
      this.tint = 0.0F;
      this.extraGap = 0.0F;
      this.barAlpha = 0.0F;
      this.barFill = 1.0F;
      this.lastFrame = 0L;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   /** Called in place of vanilla's crosshair; returns false to let vanilla draw (e.g. when the debug 3D crosshair is on). */
   public boolean render(GuiGraphics graphics) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
         return false;
      } else if (player != null && isVisible(mc)) {
         long now = System.nanoTime();
         float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (now - this.lastFrame) / 1.0E9F);
         this.lastFrame = now;
         this.updateMotion(mc, player, dt);
         Geometry g = this.geometry(graphics.guiWidth() / 2, graphics.guiHeight() / 2);
         int fill = this.currentColor();
         float fillAlpha = (fill >>> 24 & 0xFF) / 255.0F;
         int edge = this.outline.get() ? 1 : 0;
         if (edge > 0) {
            this.drawShape(graphics, g, withAlpha(OUTLINE, fillAlpha), edge);
         }

         this.drawShape(graphics, g, fill, 0);
         this.drawCooldown(graphics, mc, player, g, fill, edge, dt);
         return true;
      } else {
         return true;
      }
   }

   private static boolean isVisible(Minecraft mc) {
      if (mc.level == null || mc.gameMode == null || mc.options.hideGui) {
         return false;
      } else if (!mc.options.getCameraType().isFirstPerson()) {
         return false;
      } else {
         return mc.gameMode.getPlayerMode() != GameType.SPECTATOR || spectatorCanInteract(mc);
      }
   }

   private static boolean spectatorCanInteract(Minecraft mc) {
      HitResult hit = mc.hitResult;
      if (hit instanceof EntityHitResult entityHit) {
         return entityHit.getEntity() instanceof MenuProvider;
      } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
         BlockPos pos = blockHit.getBlockPos();
         return mc.level.getBlockState(pos).getMenuProvider(mc.level, pos) != null;
      } else {
         return false;
      }
   }

   private void updateMotion(Minecraft mc, LocalPlayer player, float dt) {
      boolean onTarget = this.targetTint.get() && mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive();
      this.tint = approach(this.tint, onTarget ? 1.0F : 0.0F, 14.0F, dt);
      float wanted = 0.0F;
      if (this.dynamicGap.get()) {
         double moved = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);
         float moving = (float)Math.min(1.0, moved / 0.28);
         float swing = 1.0F - player.getAttackStrengthScale(0.0F);
         wanted = this.spread.get() * Math.max(moving, swing);
      }

      this.extraGap = approach(this.extraGap, wanted, 12.0F, dt);
   }

   private static float approach(float current, float target, float rate, float dt) {
      float next = current + (target - current) * (1.0F - (float)Math.exp(-rate * dt));
      return Math.abs(target - next) < 0.001F ? target : next;
   }

   private int currentColor() {
      int base = this.colorMode.is("Accent") ? accent() : this.color.argb();
      return this.tint <= 0.0F ? base : lerpColor(base, this.targetColor.argb(), this.tint);
   }

   private static int accent() {
      Theme theme = Theme.get();
      if (theme != null) {
         for (Setting<?> setting : theme.settings().all()) {
            if ("accent".equals(setting.key()) && setting instanceof ColorSetting color) {
               return color.argb();
            }
         }
      }

      return 0xFFF0435A;
   }

   private static int lerpColor(int from, int to, float t) {
      int a = Math.round((from >>> 24 & 0xFF) + ((to >>> 24 & 0xFF) - (from >>> 24 & 0xFF)) * t);
      int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
      int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
      int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int withAlpha(int argb, float factor) {
      int alpha = Math.round((argb >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, factor)));
      return alpha << 24 | argb & 0xFFFFFF;
   }

   private boolean hasArms() {
      return this.style.is("Cross") || this.style.is("T") || this.style.is("X");
   }

   private Geometry geometry(int cx, int cy) {
      int thick = Math.max(1, Math.round(this.thickness.get()));
      int length = Math.max(1, Math.round(this.size.get()));
      float baseGap = this.hasArms() ? this.gap.get() : 0.0F;
      int open = Math.round(baseGap + this.extraGap);
      float maxExtra = this.dynamicGap.get() && !this.style.is("Dot") ? this.spread.get() : 0.0F;
      int below = this.reachBelow(length, Math.round(baseGap + maxExtra), thick);
      return new Geometry(cx, cy, length, open, thick, below);
   }

   private int reachBelow(int length, int open, int thick) {
      int below = switch (this.style.get()) {
         case "Dot" -> (length + 1) / 2;
         case "Circle" -> length + open + (thick + 1) / 2;
         case "X" -> Math.round((open + length) * DIAGONAL) + (thick + 1) / 2;
         default -> open + length + (thick + 1) / 2;
      };
      return this.centerDot.get() ? Math.max(below, (thick + 1) / 2) : below;
   }

   private boolean armsJoined(Geometry g) {
      return (this.style.is("Cross") || this.style.is("T")) && g.open() < 1;
   }

   private void drawShape(GuiGraphics dc, Geometry g, int color, int grow) {
      switch (this.style.get()) {
         case "Dot" -> disc(dc, g.cx(), g.cy(), Math.max(1.0F, g.size() * 0.5F) + grow, color);
         case "Circle" -> ring(dc, g.cx(), g.cy(), g.size() + g.open(), g.thick() * 0.5F + grow, color);
         case "X" -> drawDiagonals(dc, g, color, grow);
         default -> drawArms(dc, g, color, grow, !this.style.is("T"));
      }

      if (this.centerDot.get() && !this.style.is("Dot") && !this.armsJoined(g)) {
         int x0 = g.cx() - g.thick() / 2;
         int y0 = g.cy() - g.thick() / 2;
         rect(dc, x0, y0, x0 + g.thick(), y0 + g.thick(), color, grow);
      }
   }

   private static void drawArms(GuiGraphics dc, Geometry g, int color, int grow, boolean top) {
      int x0 = g.cx() - g.thick() / 2;
      int x1 = x0 + g.thick();
      int y0 = g.cy() - g.thick() / 2;
      int y1 = y0 + g.thick();
      int near = g.open();
      int far = g.open() + g.size();
      if (near < 1) {
         rect(dc, x0 - far, y0, x1 + far, y1, color, grow);
         dc.fill(x0 - grow, y1 + grow, x1 + grow, y1 + far + grow, color);
         if (top) {
            dc.fill(x0 - grow, y0 - far - grow, x1 + grow, y0 - grow, color);
         }
      } else {
         rect(dc, x0 - far, y0, x0 - near, y1, color, grow);
         rect(dc, x1 + near, y0, x1 + far, y1, color, grow);
         rect(dc, x0, y1 + near, x1, y1 + far, color, grow);
         if (top) {
            rect(dc, x0, y0 - far, x1, y0 - near, color, grow);
         }
      }
   }

   private static void drawDiagonals(GuiGraphics dc, Geometry g, int color, int grow) {
      float near = Math.max(0.0F, g.open() - grow) * DIAGONAL;
      float far = (g.open() + g.size() + grow) * DIAGONAL;
      int half = g.thick() / 2 + grow;
      int span = g.thick() + grow * 2;

      // Collect the covered pixels first and draw each once, so translucent colours do not stack.
      java.util.HashSet<Long> pixels = new java.util.HashSet<>();
      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sy = -1; sy <= 1; sy += 2) {
            for (float d = near; d <= far; d += 0.5F) {
               int px = Math.round(g.cx() + sx * d) - half;
               int py = Math.round(g.cy() + sy * d) - half;
               for (int ox = 0; ox < span; ox++) {
                  for (int oy = 0; oy < span; oy++) {
                     pixels.add((long)(px + ox) << 32 | (py + oy) & 0xFFFFFFFFL);
                  }
               }
            }
         }
      }

      for (long pixel : pixels) {
         int x = (int)(pixel >> 32);
         int y = (int)pixel;
         dc.fill(x, y, x + 1, y + 1, color);
      }
   }

   private static void disc(GuiGraphics dc, int cx, int cy, float radius, int color) {
      int r = (int)Math.ceil(radius);
      for (int y = -r; y < r; y++) {
         int start = Integer.MIN_VALUE;
         for (int x = -r; x <= r; x++) {
            boolean in = x < r && inside(x + 0.5F, y + 0.5F, 0.0F, radius);
            if (in && start == Integer.MIN_VALUE) {
               start = x;
            } else if (!in && start != Integer.MIN_VALUE) {
               dc.fill(cx + start, cy + y, cx + x, cy + y + 1, color);
               start = Integer.MIN_VALUE;
            }
         }
      }
   }

   private static void ring(GuiGraphics dc, int cx, int cy, float radius, float halfWidth, int color) {
      float inner = Math.max(0.0F, radius - halfWidth);
      float outer = radius + halfWidth;
      int r = (int)Math.ceil(outer);
      for (int y = -r; y < r; y++) {
         int start = Integer.MIN_VALUE;
         for (int x = -r; x <= r; x++) {
            boolean in = x < r && inside(x + 0.5F, y + 0.5F, inner, outer);
            if (in && start == Integer.MIN_VALUE) {
               start = x;
            } else if (!in && start != Integer.MIN_VALUE) {
               dc.fill(cx + start, cy + y, cx + x, cy + y + 1, color);
               start = Integer.MIN_VALUE;
            }
         }
      }
   }

   private static boolean inside(float x, float y, float inner, float outer) {
      float d = x * x + y * y;
      return d <= outer * outer && d >= inner * inner;
   }

   private static void rect(GuiGraphics dc, int x0, int y0, int x1, int y1, int color, int grow) {
      dc.fill(x0 - grow, y0 - grow, x1 + grow, y1 + grow, color);
   }

   private void drawCooldown(GuiGraphics dc, Minecraft mc, LocalPlayer player, Geometry g, int fill, int edge, float dt) {
      boolean wanted = mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR;
      float charge = player.getAttackStrengthScale(0.0F);
      boolean ready = wanted && readyOnTarget(mc, player, charge);
      this.barFill = ready ? 1.0F : Math.min(1.0F, charge);
      this.barAlpha = approach(this.barAlpha, !wanted || !ready && !(charge < 1.0F) ? 0.0F : 1.0F, 18.0F, dt);
      if (!(this.barAlpha < 0.01F)) {
         int w = 16;
         int h = 2;
         int x0 = g.cx() - w / 2;
         int y0 = g.cy() + g.below() + edge + 4;
         float alpha = this.barAlpha * (fill >>> 24 & 0xFF) / 255.0F;
         if (edge > 0) {
            dc.fill(x0 - edge, y0 - edge, x0 + w + edge, y0 + h + edge, withAlpha(OUTLINE, alpha));
         }

         dc.fill(x0, y0, x0 + w, y0 + h, withAlpha(TRACK, alpha));
         int filled = Math.round(w * this.barFill);
         if (filled >= 1) {
            dc.fill(x0, y0, x0 + filled, y0 + h, withAlpha(fill | 0xFF000000, alpha));
         }
      }
   }

   private static boolean readyOnTarget(Minecraft mc, LocalPlayer player, float charge) {
      if (charge < 1.0F || !(mc.crosshairPickEntity instanceof LivingEntity target && target.isAlive())) {
         return false;
      } else if (player.getCurrentItemAttackStrengthDelay() <= 5.0F) {
         return false;
      } else {
         AttackRange range = player.getActiveItem().get(DataComponents.ATTACK_RANGE);
         return range == null || mc.hitResult == null || range.isInRange(player, mc.hitResult.getLocation());
      }
   }

   private record Geometry(int cx, int cy, int size, int open, int thick, int below) {
   }
}
