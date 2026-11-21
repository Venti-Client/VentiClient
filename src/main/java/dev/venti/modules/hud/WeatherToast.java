package dev.venti.modules.hud;

import com.mojang.blaze3d.platform.Window;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** The advancement-style card toast from Anubis (ChallengeToast), trimmed to what the weather notifier needs. */
final class WeatherToast implements Toast {
   private static final int BACKGROUND = -266921698;
   private static final int TITLE_COLOR = -986891;
   private static final int MESSAGE_COLOR = -5656902;
   private static final int TIMER_TRACK = 654311423;
   private static final int TIMER_FILL = -1711276033;
   private static final int TEXT_X = 30;
   private static final int TEXT_PAD = 7;
   private static final long DISPLAY_MILLIS = 5000L;
   private final ItemStack icon;
   private final boolean sound;
   private final Component title;
   private final Component message;
   private final int width;
   private final int height;
   private final List<FormattedCharSequence> lines;
   private boolean hiding;
   private Visibility wanted = Visibility.HIDE;

   WeatherToast(ItemStack icon, Component title, Component message, boolean sound) {
      this.icon = icon.copy();
      this.title = title;
      this.message = message;
      this.sound = sound;
      Font font = Minecraft.getInstance().font;
      int text = Math.max(font.width(this.title), font.width(this.message));
      this.width = Mth.clamp(TEXT_X + text + TEXT_PAD, 160, 240);
      List<FormattedCharSequence> split = this.message.getString().isEmpty() ? List.of() : font.split(this.message, this.width - TEXT_X - TEXT_PAD);
      this.lines = split.size() > 5 ? List.copyOf(split.subList(0, 5)) : split;
      this.height = Math.max(32, 18 + this.lines.size() * 10 + 4);
   }

   @Override
   public SoundEvent getSoundEvent() {
      return this.sound ? SoundEvents.NOTE_BLOCK_BELL.value() : null;
   }

   @Override
   public Visibility getWantedVisibility() {
      return this.wanted;
   }

   @Override
   public int width() {
      return this.width;
   }

   @Override
   public int height() {
      return this.height;
   }

   /** Moves below the HUD info bar when the two would overlap at the top of the screen. */
   @Override
   public float yPos(int firstSlotIndex) {
      float y = firstSlotIndex * 32.0F;
      float[] bar = HudRenderer.infoBarBox();
      if (bar == null) {
         return y;
      } else {
         Window window = Minecraft.getInstance().getWindow();
         float scale = window.getGuiScale();
         float left = this.xPos(window.getGuiScaledWidth(), 1.0F);
         float right = left + this.width;
         float barLeft = bar[0] / scale;
         float barTop = bar[1] / scale;
         float barRight = bar[2] / scale;
         float barBottom = bar[3] / scale;
         return !(barTop > 32.0F) && !(right <= barLeft) && !(left >= barRight) ? y + barBottom + 2.0F : y;
      }
   }

   @Override
   public void update(ToastManager toasts, long visibleFor) {
      if (!this.hiding) {
         this.hiding = visibleFor >= DISPLAY_MILLIS * toasts.getNotificationDisplayTimeMultiplier();
         this.wanted = this.hiding ? Visibility.HIDE : Visibility.SHOW;
      }
   }

   @Override
   public void render(GuiGraphics graphics, Font font, long visibleFor) {
      int right = this.width - 3;
      int bottom = this.height - 1;
      rounded(graphics, 0, 1, right, bottom, BACKGROUND);
      double duration = DISPLAY_MILLIS * Minecraft.getInstance().getToastManager().getNotificationDisplayTimeMultiplier();
      float left = 1.0F - (float)Mth.clamp(visibleFor / duration, 0.0, 1.0);
      int x0 = 4;
      int x1 = right - 4;
      int y = bottom - 2;
      graphics.fill(x0, y, x1, y + 1, TIMER_TRACK);
      graphics.fill(x0, y, x0 + Math.round((x1 - x0) * left), y + 1, TIMER_FILL);
      graphics.drawString(font, this.title, TEXT_X, this.lines.isEmpty() ? 12 : 7, TITLE_COLOR, true);

      for (int i = 0; i < this.lines.size(); i++) {
         graphics.drawString(font, this.lines.get(i), TEXT_X, 18 + i * 10, MESSAGE_COLOR, true);
      }

      graphics.renderFakeItem(this.icon, 8, 8);
   }

   private static void rounded(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
      graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, color);
      graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, color);
      graphics.fill(x0, y0 + 2, x1, y1 - 2, color);
      graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, color);
      graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, color);
   }
}
