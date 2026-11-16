package dev.venti.modules.hud;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

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
   private final Text title;
   private final Text message;
   private final int width;
   private final int height;
   private final List<OrderedText> lines;
   private boolean hiding;
   private Visibility wanted = Visibility.HIDE;

   WeatherToast(ItemStack icon, Text title, Text message, boolean sound) {
      this.icon = icon.copy();
      this.title = title;
      this.message = message;
      this.sound = sound;
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      int text = Math.max(font.getWidth(this.title), font.getWidth(this.message));
      this.width = MathHelper.clamp(TEXT_X + text + TEXT_PAD, 160, 240);
      List<OrderedText> split = this.message.getString().isEmpty() ? List.of() : font.wrapLines(this.message, this.width - TEXT_X - TEXT_PAD);
      this.lines = split.size() > 5 ? List.copyOf(split.subList(0, 5)) : split;
      this.height = Math.max(32, 18 + this.lines.size() * 10 + 4);
   }

   @Override
   public SoundEvent getSoundEvent() {
      return this.sound ? SoundEvents.BLOCK_NOTE_BLOCK_BELL.value() : null;
   }

   @Override
   public Visibility getVisibility() {
      return this.wanted;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   /** Moves below the HUD info bar when the two would overlap at the top of the screen. */
   @Override
   public float getYPos(int firstSlotIndex) {
      float y = firstSlotIndex * 32.0F;
      float[] bar = HudRenderer.infoBarBox();
      if (bar == null) {
         return y;
      } else {
         Window window = MinecraftClient.getInstance().getWindow();
         float scale = window.getScaleFactor();
         float left = this.getXPos(window.getScaledWidth(), 1.0F);
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
   public void draw(DrawContext graphics, TextRenderer font, long visibleFor) {
      int right = this.width - 3;
      int bottom = this.height - 1;
      rounded(graphics, 0, 1, right, bottom, BACKGROUND);
      double duration = DISPLAY_MILLIS * MinecraftClient.getInstance().getToastManager().getNotificationDisplayTimeMultiplier();
      float left = 1.0F - (float)MathHelper.clamp(visibleFor / duration, 0.0, 1.0);
      int x0 = 4;
      int x1 = right - 4;
      int y = bottom - 2;
      graphics.fill(x0, y, x1, y + 1, TIMER_TRACK);
      graphics.fill(x0, y, x0 + Math.round((x1 - x0) * left), y + 1, TIMER_FILL);
      graphics.drawText(font, this.title, TEXT_X, this.lines.isEmpty() ? 12 : 7, TITLE_COLOR, true);

      for (int i = 0; i < this.lines.size(); i++) {
         graphics.drawText(font, this.lines.get(i), TEXT_X, 18 + i * 10, MESSAGE_COLOR, true);
      }

      graphics.drawItemWithoutEntity(this.icon, 8, 8);
   }

   private static void rounded(DrawContext graphics, int x0, int y0, int x1, int y1, int color) {
      graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, color);
      graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, color);
      graphics.fill(x0, y0 + 2, x1, y1 - 2, color);
      graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, color);
      graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, color);
   }
}
