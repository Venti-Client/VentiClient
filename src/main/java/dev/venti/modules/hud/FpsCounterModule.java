package dev.venti.modules.hud;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.NumberSetting;
import dev.venti.imgui.ImGuiManager;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import dev.venti.modules.spotify.SpotifyHud;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.client.Minecraft;

/**
 * Frames per second on the same rounded card the Spotify HUD uses, so the two match.
 * Drag it (or use the X/Y sliders) while its page is open in the GUI.
 */
public final class FpsCounterModule extends Module {
   private static final float TEXT_PX = 17.0F;
   private static final float PAD_X = 14.0F;
   private static final float HEIGHT = 34.0F;
   private final BooleanSetting label = this.add("label", new BooleanSetting("Show \"FPS\"", true));
   private final BooleanSetting colorByFps = this.add("color-by-fps", new BooleanSetting("Color By FPS", false));
   private final ColorSetting textColor = this.add("text-color", new ColorSetting("Text Color", 0xFFE6E6E6));
   private final NumberSetting posX = this.add("x", new NumberSetting("Position X %", 50.0F, 0.0F, 100.0F).step(0.5F));
   private final NumberSetting posY = this.add("y", new NumberSetting("Position Y %", 3.0F, 0.0F, 100.0F).step(0.5F));
   private final NumberSetting scale = this.add("scale", new NumberSetting("Scale", 1.0F, 0.5F, 2.5F).step(0.05F));
   private boolean dragging;
   private float dragDX;
   private float dragDY;

   public FpsCounterModule() {
      super("fps-counter", "FPS Counter", "Your frames per second on a card that matches the Spotify HUD. Drag it while this page is open.", ModuleCategory.HUD);
      this.textColor.visibleWhen(() -> !this.colorByFps.get());
   }

   public static FpsCounterModule get() {
      return Modules.get(FpsCounterModule.class);
   }

   public boolean hasWork(boolean editing) {
      Minecraft mc = Minecraft.getInstance();
      return this.isEnabled() && (editing || mc.level != null && !mc.options.hideGui);
   }

   public void render(boolean editing) {
      if (!this.hasWork(editing)) {
         this.dragging = false;
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      float s = ImGuiManager.getInstance().uiScale() * this.scale.get();
      int fps = Math.max(0, mc.getFps());
      String text = this.label.get() ? fps + " FPS" : Integer.toString(fps);
      ImFont font = ImGuiManager.getInstance().hudTextFont();
      int px = Math.round(TEXT_PX * s);
      float textW = font != null ? font.calcTextSizeA(px, Float.MAX_VALUE, 0.0F, text).x : text.length() * px * 0.55F;
      float w = textW + PAD_X * 2.0F * s;
      float h = HEIGHT * s;
      float screenW = ImGui.getIO().getDisplaySizeX();
      float screenH = ImGui.getIO().getDisplaySizeY();
      float x = this.posX.get() / 100.0F * screenW - w * 0.5F;
      float y = this.posY.get() / 100.0F * screenH;
      x = Math.max(0.0F, Math.min(screenW - w, x));
      y = Math.max(0.0F, Math.min(screenH - h, y));
      if (editing) {
         this.drag(x, y, w, h, screenW, screenH);
      }

      ImDrawList dl = ImGui.getBackgroundDrawList();
      // Same look as the Spotify card: its background color, opacity and corner radius, plus a soft shadow and hairline.
      SpotifyHud spotify = SpotifyHud.get();
      int bg = spotify != null ? spotify.backgroundColor() : 0xFF161E1D;
      float opacity = spotify != null ? spotify.backgroundOpacity() : 0.85F;
      float r = Math.min(h * 0.5F, (spotify != null ? spotify.cornerRadius() : 8.0F) * s);
      if (spotify == null || spotify.shadow()) {
         float k = Math.max(1.0F, s);
         dl.addRectFilled(x - k, y - 0.5F * k, x + w + k, y + h + 1.5F * k, 0x2E000000, r + k);
         dl.addRectFilled(x - 2.0F * k, y + 0.5F * k, x + w + 2.0F * k, y + h + 3.5F * k, 0x17000000, r + 2.0F * k);
      }

      dl.addRectFilled(x, y, x + w, y + h, abgr(withAlpha(bg, opacity)), r);
      dl.addRect(x, y, x + w, y + h, editing && this.dragging ? 0xC0FFFFFF : 0x18FFFFFF, r, 0, 1.0F);
      int color = this.colorByFps.get() ? fpsColor(fps) : this.textColor.argb();
      float ty = y + (h - px) * 0.5F;
      if (font != null) {
         dl.addText(font, px, x + PAD_X * s, ty, abgr(color), text);
      } else {
         dl.addText(x + PAD_X * s, ty, abgr(color), text);
      }
   }

   private void drag(float x, float y, float w, float h, float screenW, float screenH) {
      float mx = ImGui.getMousePosX();
      float my = ImGui.getMousePosY();
      boolean over = mx >= x && mx <= x + w && my >= y && my <= y + h;
      // 1|2|4|8|32: any window, child windows, popups, root and docking — so dragging never steals clicks from the GUI.
      boolean overUi = ImGui.isWindowHovered(1 | 2 | 4 | 8 | 32);
      if (this.dragging) {
         if (ImGui.isMouseDown(0)) {
            float nx = mx - this.dragDX;
            float ny = my - this.dragDY;
            this.posX.set((nx + w * 0.5F) / screenW * 100.0F);
            this.posY.set(Math.max(0.0F, ny) / screenH * 100.0F);
         } else {
            this.dragging = false;
            this.settings().save();
         }
      } else if (over && !overUi && ImGui.isMouseClicked(0)) {
         this.dragging = true;
         this.dragDX = mx - x;
         this.dragDY = my - y;
      }
   }

   private static int fpsColor(int fps) {
      if (fps >= 120) {
         return 0xFF5BE37D;
      } else if (fps >= 60) {
         return 0xFFE6E6E6;
      } else {
         return fps >= 30 ? 0xFFFFC14D : 0xFFFF5C5C;
      }
   }

   private static int withAlpha(int argb, float alpha) {
      return argb & 0xFFFFFF | Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24;
   }

   private static int abgr(int argb) {
      return argb & 0xFF00FF00 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
   }
}
