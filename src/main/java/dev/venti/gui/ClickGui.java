package dev.venti.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.venti.config.ActionSetting;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.Setting;
import dev.venti.config.StringListSetting;
import dev.venti.config.StringSetting;
import dev.venti.gui.image.ThemeImages;
import dev.venti.imgui.ImGuiManager;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import dev.venti.modules.hud.HotkeysModule;
import dev.venti.modules.hud.HudModule;
import dev.venti.modules.spotify.SpotifyHud;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
//? if >=26.3 {
/*import dev.venti.compat.Keys;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

/**
 * Anubis-style click GUI: a category sidebar, module cards, and per-module pages with a picture banner
 * (Theme > Header Image) and the label-left / control-right settings rows.
 */
final class ClickGui {
   private static final int ROW_HOVER = 0x14FFFFFF;
   private static final int CONTROL = 0xFF2C282E;
   private static final int CONTROL_HOVER = 0xFF39343C;
   private static final int TEXT = 0xFFEDEDED;
   private static final int TEXT_DIM = 0xFF9C979E;
   private static final int DIVIDER = 0x1AFFFFFF;
   private final float[] number = new float[1];
   private final float[] color = new float[4];
   private final ImString text = new ImString(512);
   private final ImString search = new ImString(64);
   private static final int PAGE_FADE = "venti.page.fade".hashCode();
   private String lastView = "";
   private ModuleCategory category = ModuleCategory.RENDER;
   private Module open;
   private StringSetting picking;
   private Module binding;
   private boolean[] keysAtBindStart;
   private boolean closeRequested;
   private final PanelsGui panels = new PanelsGui(this);

   boolean closeRequested() {
      return this.closeRequested;
   }

   /** HUD layout editing: the Spotify, HUD or Hotkeys page is open. */
   boolean editingHud() {
      return this.open instanceof SpotifyHud || this.open instanceof HudModule || this.open instanceof HotkeysModule || this.open instanceof dev.venti.modules.hud.FpsCounterModule;
   }

   void saveAll() {
      for (Module module : Modules.all()) {
         module.settings().save();
      }
   }

   void render() {
      Theme theme = Theme.get();
      float s = ImGuiManager.getInstance().uiScale();
      int accent = abgr(theme.accent.argb());
      this.takePickedFile();
      ImGui.setNextWindowPos(ImGui.getIO().getDisplaySizeX() * 0.5F, ImGui.getIO().getDisplaySizeY() * 0.5F, ImGuiCond.FirstUseEver, 0.5F, 0.5F);
      ImGui.setNextWindowSize(780.0F * s, 540.0F * s, ImGuiCond.FirstUseEver);
      ImGui.setNextWindowSizeConstraints(560.0F * s, 360.0F * s, 4000.0F, 4000.0F);
      float r = theme.rounding.get() * s;
      ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, r);
      ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, r);
      ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6.0F * s);
      ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, 6.0F * s);
      ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0F, 0.0F);
      ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 8.0F * s, 0.0F);
      ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarSize, 6.0F * s);
      ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 1.0F);
      int panel = abgr(withAlpha(theme.panel.argb(), theme.panelOpacity.get()));
      ImGui.pushStyleColor(ImGuiCol.WindowBg, panel);
      ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
      ImGui.pushStyleColor(ImGuiCol.Border, abgr(0x26FFFFFF));
      ImGui.pushStyleColor(ImGuiCol.FrameBg, abgr(CONTROL));
      ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, abgr(CONTROL_HOVER));
      ImGui.pushStyleColor(ImGuiCol.FrameBgActive, abgr(CONTROL_HOVER));
      ImGui.pushStyleColor(ImGuiCol.Button, abgr(CONTROL));
      ImGui.pushStyleColor(ImGuiCol.ButtonHovered, abgr(CONTROL_HOVER));
      ImGui.pushStyleColor(ImGuiCol.ButtonActive, accent);
      ImGui.pushStyleColor(ImGuiCol.SliderGrab, accent);
      ImGui.pushStyleColor(ImGuiCol.SliderGrabActive, accent);
      ImGui.pushStyleColor(ImGuiCol.Header, abgr(CONTROL_HOVER));
      ImGui.pushStyleColor(ImGuiCol.HeaderHovered, abgr(CONTROL_HOVER));
      ImGui.pushStyleColor(ImGuiCol.PopupBg, abgr(0xF8221E24));
      ImGui.pushStyleColor(ImGuiCol.Text, abgr(TEXT));
      ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, 0);
      ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, abgr(0x40FFFFFF));
      ImGui.pushStyleColor(ImGuiCol.CheckMark, accent);
      int colors = 18;
      if (theme.panelsStyle()) {
         this.panels.render(theme, s, accent);
      } else {
         if (ImGui.begin("Venti##venti_gui", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoScrollbar)) {
            this.backgroundImage(theme, r);
            this.sidebar(theme, s, accent);
            ImGui.sameLine(0.0F, 0.0F);
            String view = this.open != null ? "m:" + this.open.id() : this.search.get().isBlank() ? "c:" + this.category.name() : "s";
            if (!view.equals(this.lastView)) {
               this.lastView = view;
               GuiAnim.set(PAGE_FADE, 0.0F);
            }

            float page = GuiAnim.ease(GuiAnim.approach(PAGE_FADE, 1.0F, 9.0F));
            ImGui.setCursorPosX(ImGui.getCursorPosX() + (1.0F - page) * 18.0F * s);
            ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.02F, page));
            ImGui.beginChild("##content", 0.0F, 0.0F, false);
            if (this.open == null) {
               this.moduleList(theme, s, accent);
            } else {
               this.modulePage(this.open, theme, s, accent);
            }

            ImGui.endChild();
            ImGui.popStyleVar();
         }

         ImGui.end();
      }
      ImGui.popStyleColor(colors);
      ImGui.popStyleVar(8);
      this.listenForBind();
      if (this.binding == null && ImGui.isKeyPressed(ImGuiKey.Escape) && !ImGui.getIO().getWantTextInput() && !ImGui.isPopupOpen("", 384)) {
         if (this.open != null && !theme.panelsStyle()) {
            this.open = null;
         } else {
            this.closeRequested = true;
         }
      }
   }

   private void backgroundImage(Theme theme, float rounding) {
      ThemeImages.Texture texture = ThemeImages.get().texture(theme.backgroundImage.get());
      if (texture != null) {
         float x = ImGui.getWindowPosX();
         float y = ImGui.getWindowPosY();
         float w = ImGui.getWindowWidth();
         float h = ImGui.getWindowHeight();
         float[] uv = cover(texture, w, h, 0.5F, 0.5F);
         int tint = abgr(withAlpha(0xFFFFFFFF, theme.backgroundOpacity.get()));
         ImGui.getWindowDrawList().addImageRounded(texture.id(), x, y, x + w, y + h, uv[0], uv[1], uv[2], uv[3], tint, rounding);
      }
   }

   private void sidebar(Theme theme, float s, int accent) {
      float width = 196.0F * s;
      ImGui.beginChild("##sidebar", width, 0.0F, false);
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float h = ImGui.getWindowHeight();
      ImDrawList dl = ImGui.getWindowDrawList();
      dl.addRectFilled(x, y, x + width, y + h, abgr(0x38000000), theme.rounding.get() * s, ImDrawFlags.RoundCornersLeft);
      dl.addLine(x + width - 0.5F, y, x + width - 0.5F, y + h, abgr(DIVIDER));
      // Brand: a small mark, the name, and a dim subtitle.
      float markX = x + 22.0F * s;
      float markY = y + 26.0F * s;
      dl.addCircleFilled(markX, markY, 9.0F * s, withAlphaAbgr(accent, 0.25F), 20);
      dl.addCircle(markX, markY, 9.0F * s, accent, 20, 1.5F * s);
      dl.addLine(markX - 4.0F * s, markY - 3.0F * s, markX, markY + 4.0F * s, accent, 1.8F * s);
      dl.addLine(markX, markY + 4.0F * s, markX + 4.0F * s, markY - 3.0F * s, accent, 1.8F * s);
      dl.addText(ImGuiManager.getInstance().categoryFont(), Math.round(18.0F * s), x + 38.0F * s, y + 14.0F * s, abgr(TEXT), "Venti");
      dl.addText(ImGuiManager.getInstance().smallFont(), Math.round(12.0F * s), x + 38.0F * s, y + 33.0F * s, abgr(TEXT_DIM), "Client");
      ImGui.setCursorScreenPos(x, y + 64.0F * s);
      this.sectionLabel("MODULES", s);

      for (ModuleCategory each : ModuleCategory.values()) {
         boolean selected = this.search.get().isBlank()
            && (this.open == null && this.category == each || this.open != null && this.open.category() == each && !(this.open instanceof Theme));
         if (this.sideButton(each.label, PanelsGui.iconFor(each), selected, s, accent, width)) {
            this.category = each;
            this.open = null;
            this.search.set("");
         }
      }

      ImGui.dummy(0.0F, 10.0F * s);
      this.sectionLabel("OPTIONS", s);
      if (this.sideButton("Theme", PanelsGui.Icon.MENU, this.open instanceof Theme, s, accent, width)) {
         this.open = theme;
      }

      if (this.sideButton("Configs", PanelsGui.Icon.FOLDER, false, s, accent, width)) {
         try {
            java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("venti");
            java.nio.file.Files.createDirectories(dir);
            //? if >=26.3 {
            /*com.mojang.blaze3d.Blaze3D.openPath(dir);
            *///?} else {
            net.minecraft.util.Util.getPlatform().openFile(dir.toFile());
            //?}
         } catch (java.io.IOException | RuntimeException ignored) {
         }
      }

      ImGui.endChild();
   }

   private void sectionLabel(String label, float s) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      ImGui.getWindowDrawList().addText(ImGuiManager.getInstance().smallFont(), Math.round(11.0F * s), x + 22.0F * s, y + 4.0F * s, abgr(0xFF6E6A82), label);
      ImGui.dummy(0.0F, 22.0F * s);
   }

   private boolean sideButton(String label, PanelsGui.Icon icon, boolean selected, float s, int accent, float width) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float h = 34.0F * s;
      boolean clicked = ImGui.invisibleButton("##side_" + label, width, h);
      boolean hovered = ImGui.isItemHovered();
      ImDrawList dl = ImGui.getWindowDrawList();
      float pick = GuiAnim.approach("##side_" + label + "#sel", selected ? 1.0F : 0.0F, 12.0F);
      float hover = GuiAnim.approach("##side_" + label + "#hover", hovered ? 1.0F : 0.0F, 16.0F);
      if (pick > 0.01F || hover > 0.01F) {
         int fill = GuiAnim.mix(withAlphaAbgr(abgr(ROW_HOVER), hover * ((abgr(ROW_HOVER) >>> 24) / 255.0F)), withAlphaAbgr(accent, 0.20F), pick);
         dl.addRectFilled(x + 10.0F * s, y + 2.0F * s, x + width - 10.0F * s, y + h - 2.0F * s, fill, 7.0F * s);
      }

      int ink = GuiAnim.mix(abgr(TEXT_DIM), abgr(TEXT), Math.max(pick, hover * 0.6F));
      PanelsGui.drawIcon(dl, icon, x + 31.0F * s, y + h * 0.5F, 6.0F * s, GuiAnim.mix(ink, accent, pick));
      dl.addText(ImGuiManager.getInstance().categoryFont(), Math.round(15.0F * s), x + 48.0F * s, y + (h - 15.0F * s) * 0.5F, ink, label);
      return clicked;
   }

   private void moduleList(Theme theme, float s, int accent) {
      float pad = 24.0F * s;
      String query = this.search.get().strip().toLowerCase(java.util.Locale.ROOT);
      boolean searching = !query.isEmpty();
      java.util.List<Module> shown = new java.util.ArrayList<>();
      int enabled = 0;
      for (Module module : Modules.all()) {
         boolean match = searching
            ? module.name().toLowerCase(java.util.Locale.ROOT).contains(query) || module.description().toLowerCase(java.util.Locale.ROOT).contains(query)
            : module.category() == this.category;
         if (match && !(module instanceof Theme)) {
            shown.add(module);
            enabled += module.isEnabled() ? 1 : 0;
         }
      }

      // Header: "<Category> Modules", the count line, then search and the list/grid switch on the right.
      ImGui.setCursorPos(pad, pad);
      float hx = ImGui.getCursorScreenPosX();
      float hy = ImGui.getCursorScreenPosY();
      float avail = ImGui.getContentRegionAvailX() - pad;
      ImDrawList dl = ImGui.getWindowDrawList();
      ImFont title = ImGuiManager.getInstance().titleFont();
      int titlePx = Math.round(22.0F * s);
      String first = searching ? "Search" : this.category.label;
      dl.addText(title, titlePx, hx, hy, accent, first);
      float firstW = title.calcTextSizeA(titlePx, Float.MAX_VALUE, 0.0F, first + " ").x;
      dl.addText(title, titlePx, hx + firstW, hy, abgr(TEXT), searching ? "Results" : "Modules");
      String count = shown.size() + (shown.size() == 1 ? " module" : " modules") + "  \u00b7  " + enabled + " enabled";
      dl.addText(ImGuiManager.getInstance().smallFont(), Math.round(12.5F * s), hx, hy + 30.0F * s, abgr(TEXT_DIM), count);
      float searchW = Math.min(220.0F * s, avail * 0.4F);
      float viewW = 64.0F * s;
      ImGui.setCursorScreenPos(hx + avail - searchW - viewW - 8.0F * s, hy + 2.0F * s);
      ImGui.setNextItemWidth(searchW);
      ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10.0F * s, 6.0F * s);
      ImGui.inputTextWithHint("##search", "Search modules", this.search);
      ImGui.popStyleVar();
      boolean grid = theme.moduleView.is("Grid");
      ImGui.setCursorScreenPos(hx + avail - viewW, hy + 2.0F * s);
      if (this.viewButton("##list", false, !grid, s, accent)) {
         theme.moduleView.set("List");
      }

      ImGui.sameLine(0.0F, 4.0F * s);
      if (this.viewButton("##grid", true, grid, s, accent)) {
         theme.moduleView.set("Grid");
      }

      ImGui.setCursorScreenPos(hx, hy + 58.0F * s);
      ImGui.dummy(0.0F, 0.0F);
      if (shown.isEmpty()) {
         ImGui.setCursorPosX(pad);
         ImGui.textColored(abgr(TEXT_DIM), searching ? "No modules match \"" + this.search.get().strip() + "\"." : "No modules here yet.");
         return;
      }

      float width = ImGui.getContentRegionAvailX() - pad;
      if (grid) {
         float gap = 10.0F * s;
         float cardW = (width - gap) * 0.5F;
         for (int i = 0; i < shown.size(); i++) {
            ImGui.setCursorPosX(pad + (i % 2) * (cardW + gap));
            float rowY = ImGui.getCursorPosY();
            this.moduleCard(shown.get(i), s, accent, cardW, true);
            if (i % 2 == 0 && i + 1 < shown.size()) {
               ImGui.setCursorPosY(rowY);
            } else {
               ImGui.dummy(0.0F, gap);
            }
         }
      } else {
         for (Module module : shown) {
            ImGui.setCursorPosX(pad);
            this.moduleCard(module, s, accent, width, false);
            ImGui.dummy(0.0F, 8.0F * s);
         }
      }

      ImGui.dummy(0.0F, 12.0F * s);
   }

   /** The two little view buttons: three lines for List, four squares for Grid. */
   private boolean viewButton(String id, boolean gridIcon, boolean selected, float s, int accent) {
      float size = 30.0F * s;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      boolean clicked = ImGui.invisibleButton(id, size, size);
      ImDrawList dl = ImGui.getWindowDrawList();
      dl.addRectFilled(x, y, x + size, y + size, selected ? withAlphaAbgr(accent, 0.22F) : abgr(ImGui.isItemHovered() ? CONTROL_HOVER : CONTROL), 6.0F * s);
      int ink = selected ? accent : abgr(TEXT_DIM);
      float cx = x + size * 0.5F;
      float cy = y + size * 0.5F;
      float r = 6.0F * s;
      if (gridIcon) {
         float c = 2.5F * s;
         for (int gx = -1; gx <= 1; gx += 2) {
            for (int gy = -1; gy <= 1; gy += 2) {
               dl.addRectFilled(cx + gx * c - c * 0.9F, cy + gy * c - c * 0.9F, cx + gx * c + c * 0.9F, cy + gy * c + c * 0.9F, ink, 1.0F * s);
            }
         }
      } else {
         for (int i = -1; i <= 1; i++) {
            dl.addLine(cx - r, cy + i * r * 0.6F, cx + r, cy + i * r * 0.6F, ink, 1.6F * s);
         }
      }

      return clicked;
   }

   private void moduleCard(Module module, float s, int accent, float width, boolean compact) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float h = (compact ? 70.0F : 60.0F) * s;
      ImDrawList dl = ImGui.getWindowDrawList();
      boolean hovered = ImGui.isMouseHoveringRect(x, y, x + width, y + h) && ImGui.isWindowHovered();
      boolean on = module.isEnabled();
      float hover = GuiAnim.approach("##card_" + module.id() + "#hover", hovered ? 1.0F : 0.0F, 14.0F);
      float lit = GuiAnim.approach("##card_" + module.id() + "#on", on ? 1.0F : 0.0F, 10.0F);
      dl.addRectFilled(x, y, x + width, y + h, GuiAnim.mix(abgr(0x10FFFFFF), abgr(0x1FFFFFFF), hover), 10.0F * s);
      dl.addRect(x, y, x + width, y + h, GuiAnim.mix(abgr(0x14FFFFFF), withAlphaAbgr(accent, 0.6F), lit), 10.0F * s, 0, 1.0F);
      ImFont name = ImGuiManager.getInstance().categoryFont();
      float textRoom = width - 112.0F * s;
      dl.addText(name, Math.round(16.0F * s), x + 18.0F * s, y + (compact ? 13.0F : 12.0F) * s, abgr(TEXT), module.name());
      String description = ellipsize(module.description(), textRoom, 13.0F * s);
      dl.addText(ImGuiManager.getInstance().smallFont(), Math.round(12.5F * s), x + 18.0F * s, y + (compact ? 38.0F : 34.0F) * s, abgr(TEXT_DIM), description);
      // Arrow to the settings page, then the switch, like the reference layout.
      float cx = x + width - 82.0F * s;
      float cy = y + h * 0.5F;
      dl.addLine(cx - 2.5F * s, cy - 5.0F * s, cx + 2.5F * s, cy, abgr(TEXT_DIM), 1.6F * s);
      dl.addLine(cx + 2.5F * s, cy, cx - 2.5F * s, cy + 5.0F * s, abgr(TEXT_DIM), 1.6F * s);
      ImGui.setCursorScreenPos(x + width - 60.0F * s, y + (h - 22.0F * s) * 0.5F);
      if (this.toggle("##on_" + module.id(), on, s, accent)) {
         module.toggle();
      }

      ImGui.setCursorScreenPos(x, y);
      ImGui.invisibleButton("##open_" + module.id(), width - 66.0F * s, h);
      if (ImGui.isItemClicked()) {
         this.open = module;
      }

      ImGui.setCursorScreenPos(x, y + h);
      ImGui.dummy(0.0F, 0.0F);
   }

   private void modulePage(Module module, Theme theme, float s, int accent) {
      this.banner(module, theme, s, accent);
      float pad = 22.0F * s;
      ImGui.setCursorPosX(pad);
      ImGui.pushStyleColor(ImGuiCol.Text, abgr(TEXT_DIM));
      ImGui.pushTextWrapPos(ImGui.getWindowWidth() - pad);
      ImGui.textWrapped(module.description());
      if (module instanceof SpotifyHud) {
         ImGui.setCursorPosX(pad);
         ImGui.textWrapped("While this page is open you can drag the card to move it and scroll over it to resize.");
      }

      ImGui.popTextWrapPos();
      ImGui.popStyleColor();
      ImGui.dummy(0.0F, 12.0F * s);
      float width = ImGui.getContentRegionAvailX() - pad * 2.0F;
      ImGui.setCursorPosX(pad);
      ImGui.pushStyleColor(ImGuiCol.Text, abgr(TEXT_DIM));
      ImGui.text("Settings");
      ImGui.popStyleColor();
      ImGui.sameLine(pad + width - 64.0F * s);
      ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8.0F * s, 3.0F * s);
      if (ImGui.button("Reset all")) {
         module.settings().resetAll();
         if (module instanceof SpotifyHud) {
            module.settings().hudX = Float.NaN;
            module.settings().hudY = Float.NaN;
            module.settings().hudScale = 1.0F;
         }

         module.settings().save();
      }

      ImGui.popStyleVar();
      ImGui.dummy(0.0F, 6.0F * s);

      for (Setting<?> setting : module.settings().all()) {
         if (setting.visible()) {
            ImGui.setCursorPosX(pad);
            this.row(module, setting, s, accent, width);
         }
      }

      if (module instanceof CustomPage page) {
         ImGui.dummy(0.0F, 8.0F * s);
         ImGui.setCursorPosX(pad);
         page.renderPage(width, s, accent);
      }

      if (!(module instanceof Theme)) {
         ImGui.setCursorPosX(pad);
         this.bindRow(module, s, accent, width);
      }

      ImGui.dummy(0.0F, 16.0F * s);
   }

   /** The picture header from the Anubis module page: image, fade into the panel, back arrow, name and toggle. */
   private void banner(Module module, Theme theme, float s, int accent) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float w = ImGui.getContentRegionAvailX();
      ThemeImages.Texture texture = ThemeImages.get().texture(theme.headerImage.get());
      float h = texture != null ? theme.headerHeight.get() * s : 72.0F * s;
      ImDrawList dl = ImGui.getWindowDrawList();
      if (texture != null) {
         float[] uv = cover(texture, w, h, theme.headerX.get(), theme.headerY.get());
         dl.addImageRounded(texture.id(), x, y, x + w, y + h, uv[0], uv[1], uv[2], uv[3], 0xFFFFFFFF, theme.rounding.get() * s, ImDrawFlags.RoundCornersTop);
         int panel = abgr(withAlpha(theme.panel.argb(), theme.panelOpacity.get()));
         int clear = panel & 0x00FFFFFF;
         float fadeTop = y + h * 0.35F;
         dl.addRectFilledMultiColor(x, fadeTop, x + w, y + h + 1.0F, clear, clear, panel, panel);
      }

      float rowY = y + h - 46.0F * s;
      float backX = x + 22.0F * s;
      ImGui.setCursorScreenPos(backX, rowY);
      boolean back = ImGui.invisibleButton("##back", 32.0F * s, 32.0F * s);
      boolean backHover = ImGui.isItemHovered();
      float cx = backX + 16.0F * s;
      float cy = rowY + 16.0F * s;
      dl.addCircleFilled(cx, cy, 16.0F * s, abgr(backHover ? 0xCC3A353D : 0xAA25212A));
      dl.addLine(cx + 2.5F * s, cy - 5.5F * s, cx - 3.0F * s, cy, abgr(TEXT), 1.8F * s);
      dl.addLine(cx - 3.0F * s, cy, cx + 2.5F * s, cy + 5.5F * s, abgr(TEXT), 1.8F * s);
      if (back) {
         this.open = null;
      }

      ImFont title = ImGuiManager.getInstance().titleFont();
      dl.addText(title, Math.round(24.0F * s), backX + 46.0F * s, rowY + 4.0F * s, abgr(TEXT), module.name());
      if (!(module instanceof Theme)) {
         ImGui.setCursorScreenPos(x + w - 70.0F * s, rowY + 5.0F * s);
         if (this.toggle("##page_on", module.isEnabled(), s, accent)) {
            module.toggle();
         }
      }

      ImGui.setCursorScreenPos(x, y + h + 6.0F * s);
      ImGui.dummy(0.0F, 0.0F);
   }

   private void row(Module module, Setting<?> setting, float s, int accent, float width) {
      float rowH = 44.0F * s;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      ImDrawList dl = ImGui.getWindowDrawList();
      if (setting instanceof StringListSetting list) {
         this.listRows(list, s, accent, width);
         return;
      }

      boolean hovered = ImGui.isMouseHoveringRect(x, y, x + width, y + rowH) && ImGui.isWindowHovered();
      if (hovered) {
         dl.addRectFilled(x - 6.0F * s, y + 2.0F * s, x + width + 6.0F * s, y + rowH - 2.0F * s, abgr(ROW_HOVER), 8.0F * s);
      }

      if (!setting.isDefault()) {
         dl.addCircleFilled(x - 11.0F * s, y + rowH * 0.5F, 2.5F * s, accent);
      }

      ImGui.pushID(module.id() + "/" + setting.key());
      float textY = y + (rowH - ImGui.getTextLineHeight()) * 0.5F;
      dl.addText(x + 4.0F * s, textY, abgr(TEXT), setting.name());
      if (setting instanceof BooleanSetting toggle) {
         ImGui.setCursorScreenPos(x + width - 42.0F * s, y + (rowH - 22.0F * s) * 0.5F);
         if (this.toggle("##v", toggle.get(), s, accent)) {
            toggle.set(!toggle.get());
         }
      } else if (setting instanceof ModeSetting mode) {
         this.placeControl(x, y, width, rowH, 140.0F * s);
         if (ImGui.beginCombo("##v", mode.get())) {
            for (String option : mode.modes()) {
               if (ImGui.selectable(option, option.equals(mode.get()))) {
                  mode.set(option);
               }
            }

            ImGui.endCombo();
         }
      } else if (setting instanceof NumberSetting slider) {
         this.placeControl(x, y, width, rowH, 190.0F * s);
         this.number[0] = slider.get();
         String format = slider.stepSize() >= 1.0F ? "%.0f" : "%.2f";
         if (ImGui.sliderFloat("##v", this.number, slider.min(), slider.max(), format)) {
            slider.set(this.number[0]);
         }
      } else if (setting instanceof ColorSetting swatch) {
         int argb = swatch.argb();
         this.color[0] = (argb >> 16 & 0xFF) / 255.0F;
         this.color[1] = (argb >> 8 & 0xFF) / 255.0F;
         this.color[2] = (argb & 0xFF) / 255.0F;
         this.color[3] = (argb >>> 24) / 255.0F;
         ImGui.setCursorScreenPos(x + width - 28.0F * s, y + (rowH - 22.0F * s) * 0.5F);
         float swatchPad = Math.max(0.0F, (22.0F * s - ImGui.getFontSize()) * 0.5F);
         ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, swatchPad, swatchPad);
         if (ImGui.colorEdit4("##v", this.color, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel | ImGuiColorEditFlags.AlphaBar)) {
            swatch.set(
               Math.round(this.color[3] * 255.0F) << 24 | Math.round(this.color[0] * 255.0F) << 16 | Math.round(this.color[1] * 255.0F) << 8 | Math.round(this.color[2] * 255.0F)
            );
         }

         ImGui.popStyleVar();
      } else if (setting instanceof StringSetting field && field.isFile()) {
         this.fileControl(field, x, y, width, rowH, s);
      } else if (setting instanceof StringSetting field) {
         this.placeControl(x, y, width, rowH, 220.0F * s);
         this.text.set(field.get());
         if (ImGui.inputText("##v", this.text)) {
            field.set(this.text.get());
         }
      } else if (setting instanceof ActionSetting action) {
         this.placeControl(x, y, width, rowH, 110.0F * s);
         if (ImGui.button("Run", 110.0F * s, 0.0F)) {
            action.run();
         }
      }

      ImGui.popID();
      ImGui.setCursorScreenPos(x, y + rowH);
      dl.addLine(x, y + rowH - 0.5F, x + width, y + rowH - 0.5F, abgr(DIVIDER));
      ImGui.dummy(0.0F, 0.0F);
   }

   /** A picked picture: a small preview, its file name, and Browse / Clear. */
   private void fileControl(StringSetting field, float x, float y, float width, float rowH, float s) {
      String current = field.get();
      ThemeImages.Texture preview = current.isBlank() ? null : ThemeImages.get().preview(current);
      float controlW = 250.0F * s;
      float left = x + width - controlW;
      ImDrawList dl = ImGui.getWindowDrawList();
      float thumb = 30.0F * s;
      float ty = y + (rowH - thumb) * 0.5F;
      if (preview != null) {
         float[] uv = cover(preview, thumb, thumb, 0.5F, 0.5F);
         dl.addImageRounded(preview.id(), left, ty, left + thumb, ty + thumb, uv[0], uv[1], uv[2], uv[3], 0xFFFFFFFF, 5.0F * s);
      } else {
         dl.addRectFilled(left, ty, left + thumb, ty + thumb, abgr(CONTROL), 5.0F * s);
      }

      String label = current.isBlank() ? "None" : ellipsize(current, 96.0F * s, 14.0F * s);
      dl.addText(left + thumb + 8.0F * s, y + (rowH - ImGui.getTextLineHeight()) * 0.5F, abgr(current.isBlank() ? TEXT_DIM : TEXT), label);
      float buttonY = y + (rowH - ImGui.getFrameHeight()) * 0.5F;
      ImGui.setCursorScreenPos(x + width - 112.0F * s, buttonY);
      if (ImGui.button(this.picking == field ? "..." : "Browse", 62.0F * s, 0.0F) && this.picking == null) {
         this.picking = field;
         ThemeImages.get().browseAsync();
      }

      ImGui.setCursorScreenPos(x + width - 44.0F * s, buttonY);
      if (ImGui.button("X", 44.0F * s, 0.0F)) {
         field.set("");
      }
   }

   private void listRows(StringListSetting list, float s, int accent, float width) {
      List<String> lines = list.get();
      ImGui.pushID(list.key());
      float x = ImGui.getCursorScreenPosX();
      ImGui.dummy(0.0F, 10.0F * s);
      ImGui.setCursorScreenPos(x + 4.0F * s, ImGui.getCursorScreenPosY());
      ImGui.text(list.name());
      ImGui.dummy(0.0F, 6.0F * s);
      float fieldX = x + width * 0.35F;
      float fieldW = width * 0.65F - 40.0F * s;
      int remove = -1;

      for (int i = 0; i < lines.size(); i++) {
         ImGui.pushID(i);
         ImGui.setCursorScreenPos(fieldX, ImGui.getCursorScreenPosY());
         ImGui.setNextItemWidth(fieldW);
         this.text.set(lines.get(i));
         if (ImGui.inputText("##line", this.text)) {
            lines.set(i, this.text.get());
            list.edited();
         }

         ImGui.sameLine(0.0F, 6.0F * s);
         if (ImGui.button("-", 30.0F * s, 0.0F)) {
            remove = i;
         }

         ImGui.popID();
         ImGui.dummy(0.0F, 4.0F * s);
      }

      if (remove >= 0) {
         lines.remove(remove);
         list.edited();
      }

      ImGui.setCursorScreenPos(fieldX, ImGui.getCursorScreenPosY());
      if (ImGui.button("Add", fieldW, 0.0F)) {
         lines.add("");
         list.edited();
      }

      ImGui.dummy(0.0F, 10.0F * s);
      float y = ImGui.getCursorScreenPosY();
      ImGui.getWindowDrawList().addLine(x, y - 0.5F, x + width, y - 0.5F, abgr(DIVIDER));
      ImGui.popID();
   }

   private void bindRow(Module module, float s, int accent, float width) {
      float rowH = 44.0F * s;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      ImDrawList dl = ImGui.getWindowDrawList();
      dl.addText(x + 4.0F * s, y + (rowH - ImGui.getTextLineHeight()) * 0.5F, abgr(TEXT), "Bind");
      this.placeControl(x, y, width, rowH, 140.0F * s);
      String label = this.binding == module ? "Press a key..." : keyName(module.settings().bind);
      if (ImGui.button(label + "##bind", 140.0F * s, 0.0F)) {
         this.binding = module;
         this.keysAtBindStart = keysDown();
      }

      ImGui.setCursorScreenPos(x, y + rowH);
      ImGui.dummy(0.0F, 0.0F);
   }

   /** Takes the next fresh key press as the module's toggle key; Escape or Backspace clears it. */
   private void listenForBind() {
      if (this.binding != null) {
         boolean[] now = keysDown();
         //? if >=26.3 {
         /*for (int key = Keys.GLFW_KEY_SPACE; key < now.length; key++) {
            if (now[key] && !this.keysAtBindStart[key]) {
               this.binding.settings().bind = key == Keys.GLFW_KEY_ESCAPE || key == Keys.GLFW_KEY_BACKSPACE ? -1 : key;
         *///?} else {
         for (int key = GLFW.GLFW_KEY_SPACE; key < now.length; key++) {
            if (now[key] && !this.keysAtBindStart[key]) {
               this.binding.settings().bind = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key;
         //?}
               this.binding.settings().save();
               this.binding = null;
               return;
            }
         }

         this.keysAtBindStart = now;
      }
   }

   /** Opens the picture picker for this setting (shared with the Panels style). */
   void startPick(StringSetting field) {
      if (this.picking == null) {
         this.picking = field;
         ThemeImages.get().browseAsync();
      }
   }

   boolean isPicking(StringSetting field) {
      return this.picking == field;
   }

   /** Waits for the next key press to become this module's toggle key (shared with the Panels style). */
   void startBind(Module module) {
      this.binding = module;
      this.keysAtBindStart = keysDown();
   }

   boolean isBinding(Module module) {
      return this.binding == module;
   }

   private static boolean[] keysDown() {
      //? if >=26.3 {
      /*return Keys.glfwKeysDown();
      *///?} else {
      long window = Minecraft.getInstance().getWindow().handle();
      boolean[] down = new boolean[GLFW.GLFW_KEY_LAST + 1];
      for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
         down[key] = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
      }

      return down;
      //?}
   }

   static String keyName(int key) {
      //? if >=26.3 {
      /*return key <= 0 ? "None" : Keys.name(key);
      *///?} else {
      return key <= 0 ? "None" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
      //?}
   }

   private void takePickedFile() {
      Path picked = ThemeImages.get().takePendingImport();
      if (picked != null && this.picking != null) {
         String name = ThemeImages.get().importFile(picked);
         if (name != null) {
            this.picking.set(name);
         }

         this.picking = null;
      } else if (this.picking != null && !ThemeImages.get().dialogOpen()) {
         this.picking = null;
      }
   }

   private void placeControl(float x, float y, float width, float rowH, float controlW) {
      ImGui.setCursorScreenPos(x + width - controlW, y + (rowH - ImGui.getFrameHeight()) * 0.5F);
      ImGui.setNextItemWidth(controlW);
   }

   /** The pill switch from the Anubis GUI; returns true when clicked. */
   private boolean toggle(String id, boolean on, float s, int accent) {
      float w = 42.0F * s;
      float h = 22.0F * s;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      boolean clicked = ImGui.invisibleButton(id, w, h);
      boolean hovered = ImGui.isItemHovered();
      ImDrawList dl = ImGui.getWindowDrawList();
      float t = GuiAnim.ease(GuiAnim.approach(id, on ? 1.0F : 0.0F, 14.0F));
      float hover = GuiAnim.approach(id + "#hover", hovered ? 1.0F : 0.0F, 16.0F);
      int off = GuiAnim.mix(abgr(CONTROL), abgr(CONTROL_HOVER), hover);
      dl.addRectFilled(x, y, x + w, y + h, GuiAnim.mix(off, accent, t), h * 0.5F);
      float knob = h * 0.5F - 3.0F * s;
      float cx = x + h * 0.5F + (w - h) * t;
      dl.addCircleFilled(cx, y + h * 0.5F, knob, abgr(0xFFF5F5F5));
      return clicked;
   }

   /** UVs that fill a w×h box with the texture, cropping around the focus point instead of stretching. */
   static float[] cover(ThemeImages.Texture texture, float w, float h, float focusX, float focusY) {
      float imageAspect = (float)texture.width() / Math.max(1, texture.height());
      float boxAspect = w / Math.max(1.0F, h);
      float u0 = 0.0F;
      float v0 = 0.0F;
      float u1 = 1.0F;
      float v1 = 1.0F;
      if (imageAspect > boxAspect) {
         float span = boxAspect / imageAspect;
         u0 = (1.0F - span) * focusX;
         u1 = u0 + span;
      } else {
         float span = imageAspect / boxAspect;
         v0 = (1.0F - span) * focusY;
         v1 = v0 + span;
      }

      return new float[]{u0, v0, u1, v1};
   }

   private static String ellipsize(String text, float maxW, float size) {
      ImFont font = ImGuiManager.getInstance().smallFont();
      if (font == null || font.calcTextSizeA(size, Float.MAX_VALUE, 0.0F, text).x <= maxW) {
         return text;
      } else {
         String cut = text;
         while (cut.length() > 1 && font.calcTextSizeA(size, Float.MAX_VALUE, 0.0F, cut + "...").x > maxW) {
            cut = cut.substring(0, cut.length() - 1);
         }

         return cut.strip() + "...";
      }
   }

   static int withAlpha(int argb, float alpha) {
      return argb & 0xFFFFFF | Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24;
   }

   private static int withAlphaAbgr(int abgr, float alpha) {
      return abgr & 0xFFFFFF | Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24;
   }

   static int abgr(int argb) {
      return argb & 0xFF00FF00 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
   }
}
