package dev.venti.gui;

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
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Util;

/**
 * The classic click GUI: one floating, draggable panel per category with a coloured header, modules as rows
 * (click to toggle, arrow or right-click to open settings inline), plus Menu and Configs panels.
 */
final class PanelsGui {
   private static final int PANEL_BG = 0xF0141216;
   private static final int ROW_HOVER = 0x1AFFFFFF;
   private static final int TEXT = 0xFFE8E8E8;
   private static final int TEXT_DIM = 0xFF8E8A92;
   private static final int DIVIDER = 0x14FFFFFF;
   private static final float PANEL_W = 214.0F;
   private final ClickGui owner;
   private final Set<String> expanded = new HashSet<>();
   private final java.util.Map<Integer, Float> heights = new java.util.HashMap<>();
   private boolean menuColorOpen;
   private final float[] number = new float[1];
   private final float[] color = new float[4];
   private final ImString text = new ImString(512);

   PanelsGui(ClickGui owner) {
      this.owner = owner;
   }

   void render(Theme theme, float s, int accent) {
      float gap = 10.0F * s;
      float x = 16.0F * s;
      float y = 16.0F * s;
      int column = 0;

      for (ModuleCategory category : ModuleCategory.values()) {
         this.categoryPanel(category, x + column * (PANEL_W * s + gap), y, theme, s, accent);
         column++;
      }

      this.menuPanel(x + column * (PANEL_W * s + gap), y, theme, s, accent);
      this.configsPanel(x + (column + 1) * (PANEL_W * s + gap), y, s, accent);
   }

   // ---- panels ----

   private void categoryPanel(ModuleCategory category, float x, float y, Theme theme, float s, int accent) {
      if (this.begin(category.label, x, y, s)) {
         this.header(category.label, iconFor(category), s, accent);

         for (Module module : Modules.all()) {
            if (module.category() == category && !(module instanceof Theme)) {
               this.moduleRow(module, theme, s, accent);
            }
         }

         ImGui.dummy(0.0F, 4.0F * s);
      }

      this.end();
   }

   private void menuPanel(float x, float y, Theme theme, float s, int accent) {
      if (this.begin("Menu", x, y, s)) {
         this.header("Menu", Icon.MENU, s, accent);
         float pad = 10.0F * s;
         float width = ImGui.getContentRegionAvailX() - pad * 2.0F;
         ImGui.setCursorPosX(pad);
         this.labelValue("Menu Key", menuKeyName(), width, s);
         ImGui.setCursorPosX(pad);
         this.setting(theme, theme.guiStyle, width, s, accent);
         ImGui.setCursorPosX(pad);
         this.setting(theme, theme.descriptions, width, s, accent);
         ImGui.setCursorPosX(pad);
         this.menuColor(theme, pad, width, s, accent);

         ImGui.dummy(0.0F, 6.0F * s);
         this.moduleRow(theme, theme, s, accent);
         ImGui.dummy(0.0F, 4.0F * s);
      }

      this.end();
   }

   /** "Menu Color" row with a swatch and arrow; the big picker slides open beneath it and can be folded away. */
   private void menuColor(Theme theme, float pad, float width, float s, int accent) {
      float x = ImGui.getWindowPosX() + pad;
      float y = ImGui.getCursorScreenPosY();
      float h = 24.0F * s;
      ImGui.setCursorPosX(pad);
      boolean clicked = ImGui.invisibleButton("##menu_color_row", width, h);
      boolean hovered = ImGui.isItemHovered();
      if (clicked) {
         this.menuColorOpen = !this.menuColorOpen;
      }

      ImDrawList dl = ImGui.getWindowDrawList();
      float hover = GuiAnim.approach("##menu_color_row#hover", hovered ? 1.0F : 0.0F, 16.0F);
      if (hover > 0.01F) {
         dl.addRectFilled(x - 4.0F * s, y, x + width + 4.0F * s, y + h, GuiAnim.mix(ClickGui.abgr(ROW_HOVER) & 0x00FFFFFF, ClickGui.abgr(ROW_HOVER), hover), 4.0F * s);
      }

      dl.addText(x, y + (h - ImGui.getTextLineHeight()) * 0.5F, abgrF(TEXT_DIM), "Menu Color");
      float sw = 14.0F * s;
      float sx = x + width - 30.0F * s - sw;
      float sy = y + (h - sw) * 0.5F;
      dl.addRectFilled(sx, sy, sx + sw, sy + sw, accent, 3.0F * s);
      dl.addRect(sx, sy, sx + sw, sy + sw, 0x40FFFFFF, 3.0F * s, 0, 1.0F);
      float openT = GuiAnim.ease(GuiAnim.approach("##menu_color#open", this.menuColorOpen ? 1.0F : 0.0F, 11.0F));
      float cx = x + width - 10.0F * s;
      float cy = y + h * 0.5F;
      float a = 3.5F * s;
      float tip = a * 0.5F * (1.0F - 2.0F * openT);
      int arrow = GuiAnim.mix(ClickGui.abgr(TEXT_DIM), ClickGui.abgr(TEXT), hover);
      dl.addLine(cx - a, cy - tip, cx, cy + tip, arrow, 1.5F * s);
      dl.addLine(cx, cy + tip, cx + a, cy - tip, arrow, 1.5F * s);
      if (openT > 0.001F) {
         int key = ImGui.getID("##menu_color_picker");
         float full = this.heights.getOrDefault(key, 0.0F);
         float shown = full <= 0.0F ? 1.0F : full * openT;
         ImGui.setCursorPosX(0.0F);
         ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.02F, openT));
         ImGui.beginChild("##menu_color_picker", ImGui.getWindowWidth(), Math.max(1.0F, shown), false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
         ImGui.dummy(0.0F, 4.0F * s);
         ImGui.setCursorPosX(pad);
         this.toFloats(theme.accent.argb());
         ImGui.setNextItemWidth(width);
         if (ImGui.colorPicker4("##menu_color", this.color, ImGuiColorEditFlags.NoSidePreview | ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel | ImGuiColorEditFlags.AlphaBar)) {
            theme.accent.set(this.fromFloats());
         }

         ImGui.dummy(0.0F, 4.0F * s);
         this.heights.put(key, ImGui.getCursorPosY());
         ImGui.endChild();
         ImGui.popStyleVar();
      }
   }

   private void configsPanel(float x, float y, float s, int accent) {
      if (this.begin("Configs", x, y, s)) {
         this.header("Configs", Icon.FOLDER, s, accent);
         float pad = 10.0F * s;
         float width = ImGui.getContentRegionAvailX() - pad * 2.0F;
         ImGui.setCursorPosX(pad);
         if (ImGui.button("Browse Configs", width, 0.0F)) {
            open(FabricLoader.getInstance().getConfigDir().resolve("venti"));
         }

         ImGui.dummy(0.0F, 4.0F * s);
         ImGui.setCursorPosX(pad);
         if (ImGui.button("Browse Pictures", width, 0.0F)) {
            open(ThemeImages.dir());
         }

         ImGui.dummy(0.0F, 4.0F * s);
         ImGui.setCursorPosX(pad);
         if (ImGui.button("Save All", width, 0.0F)) {
            this.owner.saveAll();
         }

         ImGui.dummy(0.0F, 8.0F * s);
      }

      this.end();
   }

   private boolean begin(String title, float x, float y, float s) {
      ImGui.setNextWindowPos(x, y, ImGuiCond.FirstUseEver);
      float maxH = ImGui.getIO().getDisplaySizeY() * 0.9F;
      ImGui.setNextWindowSizeConstraints(PANEL_W * s, 0.0F, PANEL_W * s, maxH);
      ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 4.0F * s);
      ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0F, 0.0F);
      ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 6.0F * s, 0.0F);
      ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 6.0F * s, 3.0F * s);
      ImGui.pushStyleColor(ImGuiCol.WindowBg, ClickGui.abgr(PANEL_BG));
      boolean open = ImGui.begin(title + "##venti_panel_" + title, ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.AlwaysAutoResize);
      ImGui.popStyleColor();
      return open;
   }

   private void end() {
      ImGui.end();
      ImGui.popStyleVar(4);
   }

   /** The coloured title strip; dragging it moves the panel (ImGui moves windows by any empty area). */
   private void header(String title, Icon icon, float s, int accent) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float w = ImGui.getWindowWidth();
      float h = 28.0F * s;
      ImDrawList dl = ImGui.getWindowDrawList();
      dl.addRectFilled(x, y, x + w, y + h, accent, 4.0F * s, 48);
      drawIcon(dl, icon, x + 15.0F * s, y + h * 0.5F, 7.0F * s, 0xFFFFFFFF);
      float textW = ImGui.calcTextSize(title).x;
      dl.addText(ImGuiManager.getInstance().categoryFont(), Math.round(16.0F * s), x + (w - textW * 1.05F) * 0.5F, y + (h - 16.0F * s) * 0.5F, 0xFFFFFFFF, title);
      ImGui.dummy(w, h);
      ImGui.dummy(0.0F, 3.0F * s);
   }

   private void moduleRow(Module module, Theme theme, float s, int accent) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      float w = ImGui.getWindowWidth();
      float h = 24.0F * s;
      boolean on = module.isEnabled() || module instanceof Theme;
      boolean isOpen = this.expanded.contains(module.id());
      ImDrawList dl = ImGui.getWindowDrawList();
      ImGui.pushID(module.id());
      ImGui.invisibleButton("##row", w - 30.0F * s, h);
      boolean hovered = ImGui.isItemHovered();
      if (ImGui.isItemClicked(0) && !(module instanceof Theme)) {
         module.toggle();
      }

      if (ImGui.isItemClicked(1)) {
         this.toggleOpen(module);
      }

      if (hovered && theme.descriptions.is("Mouse")) {
         ImGui.setTooltip(module.description());
      }

      ImGui.sameLine(0.0F, 0.0F);
      if (ImGui.invisibleButton("##arrow", 30.0F * s, h)) {
         this.toggleOpen(module);
      }

      boolean arrowHover = ImGui.isItemHovered();
      float hover = GuiAnim.approach("##rowhover", hovered || arrowHover ? 1.0F : 0.0F, 16.0F);
      if (hover > 0.01F) {
         dl.addRectFilled(x, y, x + w, y + h, GuiAnim.mix(ClickGui.abgr(ROW_HOVER) & 0x00FFFFFF, ClickGui.abgr(ROW_HOVER), hover));
      }

      float lit = GuiAnim.ease(GuiAnim.approach("##rowon", on && !(module instanceof Theme) ? 1.0F : 0.0F, 12.0F));
      if (lit > 0.01F) {
         float barH = (h - 6.0F * s) * lit;
         dl.addRectFilled(x, y + h * 0.5F - barH * 0.5F, x + 2.5F * s, y + h * 0.5F + barH * 0.5F, accent);
      }

      int nameColor = module instanceof Theme ? ClickGui.abgr(TEXT) : GuiAnim.mix(ClickGui.abgr(TEXT_DIM), accent, lit);
      dl.addText(x + 10.0F * s, y + (h - ImGui.getTextLineHeight()) * 0.5F, nameColor, module.name());
      float cx = x + w - 15.0F * s;
      float cy = y + h * 0.5F;
      float a = 3.5F * s;
      int arrowColor = GuiAnim.mix(ClickGui.abgr(TEXT_DIM), ClickGui.abgr(TEXT), GuiAnim.approach("##arrowhover", arrowHover ? 1.0F : 0.0F, 16.0F));
      float openT = GuiAnim.ease(GuiAnim.approach("##open", isOpen ? 1.0F : 0.0F, 11.0F));
      // The chevron flips through flat as it opens: tip goes from below the ends to above them.
      float tip = a * 0.5F * (1.0F - 2.0F * openT);
      dl.addLine(cx - a, cy - tip, cx, cy + tip, arrowColor, 1.5F * s);
      dl.addLine(cx, cy + tip, cx + a, cy - tip, arrowColor, 1.5F * s);
      dl.addLine(x + 6.0F * s, y + h - 0.5F, x + w - 6.0F * s, y + h - 0.5F, ClickGui.abgr(DIVIDER));
      if (openT > 0.001F) {
         // Draw the settings in a child whose height grows with the animation; the full height is measured each frame.
         int key = ImGui.getID("##settings");
         float full = this.heights.getOrDefault(key, 0.0F);
         float shown = full <= 0.0F ? 1.0F : full * openT;
         ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.02F, openT));
         ImGui.beginChild("##settings", w, Math.max(1.0F, shown), false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
         this.settingsBlock(module, s, accent);
         this.heights.put(key, ImGui.getCursorPosY());
         ImGui.endChild();
         ImGui.popStyleVar();
      }

      ImGui.popID();
   }

   private void toggleOpen(Module module) {
      if (!this.expanded.remove(module.id())) {
         this.expanded.add(module.id());
      }
   }

   private void settingsBlock(Module module, float s, int accent) {
      float pad = 12.0F * s;
      float width = ImGui.getWindowWidth() - pad * 2.0F;
      ImGui.dummy(0.0F, 4.0F * s);

      for (Setting<?> setting : module.settings().all()) {
         if (setting.visible() && !(module instanceof Theme && (setting == ((Theme)module).guiStyle || setting == ((Theme)module).descriptions || setting == ((Theme)module).accent))) {
            ImGui.setCursorPosX(pad);
            this.setting(module, setting, width, s, accent);
         }
      }

      if (module instanceof CustomPage page) {
         ImGui.setCursorPosX(pad);
         page.renderPage(width, s, accent);
      }

      if (!(module instanceof Theme)) {
         ImGui.setCursorPosX(pad);
         String label = this.owner.isBinding(module) ? "Press a key..." : ClickGui.keyName(module.settings().bind);
         ImGui.textColored(abgrF(TEXT_DIM), "Bind");
         ImGui.sameLine(pad + width * 0.45F);
         if (ImGui.button(label + "##bind", width * 0.55F, 0.0F)) {
            this.owner.startBind(module);
         }

         ImGui.dummy(0.0F, 3.0F * s);
         ImGui.setCursorPosX(pad);
         if (ImGui.button("Reset##reset", width, 0.0F)) {
            module.settings().resetAll();
            module.settings().save();
         }
      }

      ImGui.dummy(0.0F, 6.0F * s);
   }

   // ---- one setting, compact: label on the left, control on the right (or below for wide controls) ----

   private void setting(Module module, Setting<?> setting, float width, float s, int accent) {
      ImGui.pushID(setting.key());
      float labelW = width * 0.45F;
      float controlX = ImGui.getCursorPosX() + labelW;
      float controlW = width - labelW;
      int labelColor = abgrF(setting.isDefault() ? TEXT_DIM : TEXT);
      if (setting instanceof BooleanSetting toggle) {
         ImGui.textColored(labelColor, setting.name());
         ImGui.sameLine(controlX + controlW - 26.0F * s);
         if (this.box("##v", toggle.get(), s, accent)) {
            toggle.set(!toggle.get());
         }
      } else if (setting instanceof ModeSetting mode) {
         ImGui.alignTextToFramePadding();
         ImGui.textColored(labelColor, setting.name());
         ImGui.sameLine(controlX);
         ImGui.setNextItemWidth(controlW);
         if (ImGui.beginCombo("##v", mode.get())) {
            for (String option : mode.modes()) {
               if (ImGui.selectable(option, option.equals(mode.get()))) {
                  mode.set(option);
               }
            }

            ImGui.endCombo();
         }
      } else if (setting instanceof NumberSetting slider) {
         ImGui.textColored(labelColor, setting.name());
         ImGui.setCursorPosX(controlX - labelW);
         ImGui.setNextItemWidth(width);
         this.number[0] = slider.get();
         if (ImGui.sliderFloat("##v", this.number, slider.min(), slider.max(), slider.stepSize() >= 1.0F ? "%.0f" : "%.2f")) {
            slider.set(this.number[0]);
         }
      } else if (setting instanceof ColorSetting swatch) {
         ImGui.alignTextToFramePadding();
         ImGui.textColored(labelColor, setting.name());
         ImGui.sameLine(controlX + controlW - 24.0F * s);
         this.toFloats(swatch.argb());
         if (ImGui.colorEdit4("##v", this.color, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel | ImGuiColorEditFlags.AlphaBar)) {
            swatch.set(this.fromFloats());
         }
      } else if (setting instanceof StringSetting field && field.isFile()) {
         ImGui.textColored(labelColor, setting.name());
         ImGui.setCursorPosX(controlX - labelW);
         String current = field.get().isBlank() ? "None" : field.get();
         if (ImGui.button((this.owner.isPicking(field) ? "..." : current) + "##pick", width - 30.0F * s, 0.0F)) {
            this.owner.startPick(field);
         }

         ImGui.sameLine(0.0F, 4.0F * s);
         if (ImGui.button("X##clear", 26.0F * s, 0.0F)) {
            field.set("");
         }
      } else if (setting instanceof StringSetting field) {
         ImGui.textColored(labelColor, setting.name());
         ImGui.setCursorPosX(controlX - labelW);
         ImGui.setNextItemWidth(width);
         this.text.set(field.get());
         if (ImGui.inputText("##v", this.text)) {
            field.set(this.text.get());
         }
      } else if (setting instanceof StringListSetting list) {
         this.listSetting(list, controlX - labelW, width, s);
      } else if (setting instanceof ActionSetting action) {
         if (ImGui.button(setting.name() + "##run", width, 0.0F)) {
            action.run();
         }
      }

      ImGui.dummy(0.0F, 4.0F * s);
      ImGui.popID();
   }

   private void listSetting(StringListSetting list, float x, float width, float s) {
      ImGui.textColored(abgrF(list.isDefault() ? TEXT_DIM : TEXT), list.name());
      List<String> lines = list.get();
      int remove = -1;

      for (int i = 0; i < lines.size(); i++) {
         ImGui.pushID(i);
         ImGui.setCursorPosX(x);
         ImGui.setNextItemWidth(width - 30.0F * s);
         this.text.set(lines.get(i));
         if (ImGui.inputText("##line", this.text)) {
            lines.set(i, this.text.get());
            list.edited();
         }

         ImGui.sameLine(0.0F, 4.0F * s);
         if (ImGui.button("-##del", 26.0F * s, 0.0F)) {
            remove = i;
         }

         ImGui.popID();
         ImGui.dummy(0.0F, 2.0F * s);
      }

      if (remove >= 0) {
         lines.remove(remove);
         list.edited();
      }

      ImGui.setCursorPosX(x);
      if (ImGui.button("Add##add", width, 0.0F)) {
         lines.add("");
         list.edited();
      }
   }

   private void labelValue(String label, String value, float width, float s) {
      ImGui.textColored(abgrF(TEXT_DIM), label);
      float valueW = ImGui.calcTextSize(value).x;
      ImGui.sameLine(ImGui.getCursorPosX() + width - valueW);
      ImGui.text(value);
      ImGui.dummy(0.0F, 4.0F * s);
   }

   /** A small square check box in the menu colour. */
   private boolean box(String id, boolean on, float s, int accent) {
      float size = 14.0F * s;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY() + 1.0F * s;
      boolean clicked = ImGui.invisibleButton(id, size + 12.0F * s, size + 2.0F * s);
      ImDrawList dl = ImGui.getWindowDrawList();
      float bx = x + 12.0F * s;
      float hover = GuiAnim.approach(id + "#hover", ImGui.isItemHovered() ? 1.0F : 0.0F, 16.0F);
      dl.addRect(bx, y, bx + size, y + size, GuiAnim.mix(ClickGui.abgr(TEXT_DIM), ClickGui.abgr(TEXT), hover), 2.0F * s, 0, 1.0F);
      float fill = GuiAnim.ease(GuiAnim.approach(id, on ? 1.0F : 0.0F, 14.0F));
      if (fill > 0.01F) {
         float inset = 3.0F * s + (1.0F - fill) * (size * 0.5F - 3.0F * s);
         dl.addRectFilled(bx + inset, y + inset, bx + size - inset, y + size - inset, accent, 1.0F * s);
      }

      return clicked;
   }

   // ---- icons drawn with lines, one per panel ----

   enum Icon {
      EYE,
      CUBE,
      LAYOUT,
      DOTS,
      MENU,
      FOLDER
   }

   static Icon iconFor(ModuleCategory category) {
      return switch (category) {
         case VISUALS -> Icon.EYE;
         case RENDER -> Icon.CUBE;
         case HUD -> Icon.LAYOUT;
         case MISC -> Icon.DOTS;
      };
   }

   static void drawIcon(ImDrawList dl, Icon icon, float cx, float cy, float r, int color) {
      float t = Math.max(1.2F, r * 0.22F);
      switch (icon) {
         case EYE -> {
            dl.addBezierCubic(cx - r, cy, cx - r * 0.4F, cy - r * 0.9F, cx + r * 0.4F, cy - r * 0.9F, cx + r, cy, color, t, 12);
            dl.addBezierCubic(cx - r, cy, cx - r * 0.4F, cy + r * 0.9F, cx + r * 0.4F, cy + r * 0.9F, cx + r, cy, color, t, 12);
            dl.addCircleFilled(cx, cy, r * 0.32F, color, 12);
         }
         case CUBE -> {
            dl.addQuad(cx, cy - r, cx + r, cy - r * 0.5F, cx, cy, cx - r, cy - r * 0.5F, color, t);
            dl.addLine(cx - r, cy - r * 0.5F, cx - r, cy + r * 0.5F, color, t);
            dl.addLine(cx + r, cy - r * 0.5F, cx + r, cy + r * 0.5F, color, t);
            dl.addLine(cx, cy, cx, cy + r, color, t);
            dl.addLine(cx - r, cy + r * 0.5F, cx, cy + r, color, t);
            dl.addLine(cx + r, cy + r * 0.5F, cx, cy + r, color, t);
         }
         case LAYOUT -> {
            dl.addRect(cx - r, cy - r, cx + r, cy + r, color, r * 0.2F, 0, t);
            dl.addLine(cx - r, cy - r * 0.3F, cx + r, cy - r * 0.3F, color, t);
            dl.addLine(cx - r * 0.2F, cy - r * 0.3F, cx - r * 0.2F, cy + r, color, t);
         }
         case DOTS -> {
            for (int i = -1; i <= 1; i++) {
               dl.addCircleFilled(cx + i * r * 0.75F, cy, r * 0.24F, color, 8);
            }
         }
         case MENU -> {
            for (int i = -1; i <= 1; i++) {
               dl.addLine(cx - r, cy + i * r * 0.6F, cx + r, cy + i * r * 0.6F, color, t);
            }
         }
         case FOLDER -> {
            dl.addRect(cx - r, cy - r * 0.55F, cx + r, cy + r * 0.8F, color, r * 0.15F, 0, t);
            dl.addLine(cx - r, cy - r * 0.55F, cx - r * 0.55F, cy - r * 0.95F, color, t);
            dl.addLine(cx - r * 0.55F, cy - r * 0.95F, cx - r * 0.05F, cy - r * 0.95F, color, t);
            dl.addLine(cx - r * 0.05F, cy - r * 0.95F, cx + r * 0.2F, cy - r * 0.55F, color, t);
         }
      }
   }

   // ---- helpers ----

   private static String menuKeyName() {
      for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
         if ("key.venti.gui".equals(binding.getId())) {
            return binding.getBoundKeyLocalizedText().getString();
         }
      }

      return "Right Shift";
   }

   private static void open(Path dir) {
      try {
         java.nio.file.Files.createDirectories(dir);
         Util.getOperatingSystem().open(dir.toFile());
      } catch (java.io.IOException | RuntimeException ignored) {
      }
   }

   private void toFloats(int argb) {
      this.color[0] = (argb >> 16 & 0xFF) / 255.0F;
      this.color[1] = (argb >> 8 & 0xFF) / 255.0F;
      this.color[2] = (argb & 0xFF) / 255.0F;
      this.color[3] = (argb >>> 24) / 255.0F;
   }

   private int fromFloats() {
      return Math.round(this.color[3] * 255.0F) << 24 | Math.round(this.color[0] * 255.0F) << 16 | Math.round(this.color[1] * 255.0F) << 8 | Math.round(this.color[2] * 255.0F);
   }

   private static int abgrF(int argb) {
      return ClickGui.abgr(argb);
   }
}
