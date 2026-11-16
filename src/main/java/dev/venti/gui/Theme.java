package dev.venti.gui;

import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.StringSetting;
import dev.venti.gui.image.ThemeImages;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;

/** GUI look: colors plus the header banner and window background images, like the Anubis theme page. */
public final class Theme extends Module {
   public static final String[] IMAGE_TYPES = {"png", "jpg", "jpeg", "gif"};
   final ModeSetting guiStyle = this.add("gui-style", new ModeSetting("GUI Style", "Venti", "Venti", "Panels"));
   final ModeSetting moduleView = this.add("module-view", new ModeSetting("Module View", "List", "List", "Grid"));
   final ModeSetting descriptions = this.add("descriptions", new ModeSetting("Descriptions", "Mouse", "Mouse", "Off"));
   final ColorSetting accent = this.add("accent", new ColorSetting("Accent Color", 0xFF8B7CF7));
   final ColorSetting panel = this.add("panel", new ColorSetting("Panel Color", 0xFF111026));
   final NumberSetting panelOpacity = this.add("panel-opacity", new NumberSetting("Panel Opacity", 0.94F, 0.3F, 1.0F).step(0.01F));
   final StringSetting headerImage = this.add("header-image", new StringSetting("Header Image", "").files(IMAGE_TYPES));
   final NumberSetting headerHeight = this.add("header-height", new NumberSetting("Header Height", 120.0F, 70.0F, 220.0F).step(1.0F));
   final NumberSetting headerX = this.add("header-x", new NumberSetting("Header Focus X", 0.5F, 0.0F, 1.0F).step(0.05F));
   final NumberSetting headerY = this.add("header-y", new NumberSetting("Header Focus Y", 0.4F, 0.0F, 1.0F).step(0.05F));
   final StringSetting backgroundImage = this.add("background-image", new StringSetting("Background Image", "").files(IMAGE_TYPES));
   final NumberSetting backgroundOpacity = this.add("background-opacity", new NumberSetting("Background Image Opacity", 0.25F, 0.05F, 1.0F).step(0.05F));
   final NumberSetting rounding = this.add("rounding", new NumberSetting("Corner Radius", 10.0F, 0.0F, 16.0F).step(1.0F));
   final NumberSetting uiScale = this.add("ui-scale", new NumberSetting("UI Scale", 1.0F, 0.75F, 1.5F).step(0.05F));

   public Theme() {
      super("theme", "Theme", "Colors and pictures for the Venti GUI. Pick a PNG, JPG or animated GIF for the header and background.", ModuleCategory.MISC, true);
      this.headerX.visibleWhen(() -> !this.headerImage.get().isBlank());
      this.headerY.visibleWhen(() -> !this.headerImage.get().isBlank());
      this.headerHeight.visibleWhen(() -> !this.headerImage.get().isBlank());
      this.backgroundOpacity.visibleWhen(() -> !this.backgroundImage.get().isBlank());
      ThemeImages.get().addUser(this.headerImage::get);
      ThemeImages.get().addUser(this.backgroundImage::get);
   }

   public static Theme get() {
      return Modules.get(Theme.class);
   }

   /** The classic floating-panels click GUI instead of the Venti window. */
   public boolean panelsStyle() {
      return this.guiStyle.is("Panels");
   }

   public float scale() {
      return this.uiScale.get();
   }
}
