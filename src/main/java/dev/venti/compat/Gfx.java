package dev.venti.compat;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
//? if >=26.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * The 2D GUI drawing calls Venti uses, over {@code GuiGraphics} (up to 1.21.11) or {@code GuiGraphicsExtractor} (26.x),
 * so HUD and toast code is written once.
 */
@Environment(EnvType.CLIENT)
public final class Gfx {
   //? if >=26.2 {
   public final GuiGraphicsExtractor raw;

   public Gfx(GuiGraphicsExtractor raw) {
      this.raw = raw;
   }
   //?} else {
   /*public final GuiGraphics raw;

   public Gfx(GuiGraphics raw) {
      this.raw = raw;
   }
   *///?}

   public void fill(int x0, int y0, int x1, int y1, int color) {
      this.raw.fill(x0, y0, x1, y1, color);
   }

   public Matrix3x2fStack pose() {
      return this.raw.pose();
   }

   public int guiWidth() {
      return this.raw.guiWidth();
   }

   public int guiHeight() {
      return this.raw.guiHeight();
   }

   public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
      //? if >=26.2 {
      this.raw.text(font, text, x, y, color, shadow);
      //?} else {
      /*this.raw.drawString(font, text, x, y, color, shadow);
      *///?}
   }

   public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
      //? if >=26.2 {
      this.raw.text(font, text, x, y, color, shadow);
      //?} else {
      /*this.raw.drawString(font, text, x, y, color, shadow);
      *///?}
   }

   public void fakeItem(ItemStack stack, int x, int y) {
      //? if >=26.2 {
      this.raw.fakeItem(stack, x, y);
      //?} else {
      /*this.raw.renderFakeItem(stack, x, y);
      *///?}
   }

   public void requestCursor(CursorType cursor) {
      this.raw.requestCursor(cursor);
   }
}
