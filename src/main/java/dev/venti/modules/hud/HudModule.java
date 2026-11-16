package dev.venti.modules.hud;

import dev.venti.config.ActionSetting;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.Setting;
import dev.venti.gui.Theme;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;

/**
 * The Anubis HUD: info bar, coordinates, module list, potions, armor and item cooldowns.
 * While this page (or the Hotkeys page) is open in the click GUI the elements can be dragged and scrolled to resize.
 */
public final class HudModule extends Module {
   private static final int DEFAULT_ACCENT = 0xFFF0435A;
   private final BooleanSetting clock = this.add("clock", new BooleanSetting("Clock", true));
   private final BooleanSetting fps = this.add("fps", new BooleanSetting("FPS", true));
   private final BooleanSetting ping = this.add("ping", new BooleanSetting("Ping", true));
   private final BooleanSetting logo = this.add("logo", new BooleanSetting("Logo", true));
   private final BooleanSetting coordinates = this.add("coordinates", new BooleanSetting("Overworld Coords", true));
   private final BooleanSetting netherCoordinates = this.add("nether-coordinates", new BooleanSetting("Nether Coords", true));
   private final BooleanSetting moduleList = this.add("module-list", new BooleanSetting("Module List", true));
   private final BooleanSetting potions = this.add("potions", new BooleanSetting("Potions", true));
   private final BooleanSetting armorStatus = this.add("armor-status", new BooleanSetting("Armor HUD", true));
   private final ModeSetting armorLayout = this.add("armor-layout", new ModeSetting("Armor Layout", "Row", "Row", "Column"));
   private final ModeSetting armorWear = this.add("armor-wear", new ModeSetting("Wear As", "Gauge", "Gauge", "Percent", "Gauge + Percent"));
   private final BooleanSetting armorHands = this.add("armor-hands", new BooleanSetting("Include Hands", false));
   private final BooleanSetting itemCooldowns = this.add("item-cooldowns", new BooleanSetting("Cooldown", true));
   private final ModeSetting cooldownDial = this.add("cooldown-dial", new ModeSetting("Cooldown Dial", "Halo", "Halo", "Sandbar"));
   private final BooleanSetting shadow = this.add("shadow", new BooleanSetting("Shadow", true));
   private final ColorSetting background = this.add("background", new ColorSetting("Background Color", 0xFF16181D));
   private final NumberSetting cardOpacity = this.add("card-opacity", new NumberSetting("Info Bar Opacity", 0.85F, 0.2F, 1.0F).step(0.01F));
   private final NumberSetting rowOpacity = this.add("row-opacity", new NumberSetting("List Opacity", 0.6F, 0.2F, 1.0F).step(0.01F));
   private final NumberSetting rounding = this.add("rounding", new NumberSetting("Corner Radius", 8.0F, 0.0F, 16.0F).step(1.0F));
   private final BooleanSetting customAccent = this.add("custom-accent", new BooleanSetting("Custom Accent", false));
   private final ColorSetting accentColor = this.add("accent-color", new ColorSetting("Accent Color", -13338378));
   private final ActionSetting resetLayout = this.add("reset-layout", new ActionSetting("Reset Layout", HudRenderer::requestLayoutReset));

   public HudModule() {
      super("hud", "HUD", "Info bar, coordinates, module list, potions, armor and item cooldowns. Drag and scroll the elements while this page is open.", ModuleCategory.HUD, true);
      this.accentColor.visibleWhen(this.customAccent::get);
      this.armorLayout.visibleWhen(this.armorStatus::get);
      this.armorWear.visibleWhen(this.armorStatus::get);
      this.armorHands.visibleWhen(this.armorStatus::get);
      this.cooldownDial.visibleWhen(this.itemCooldowns::get);
   }

   public static HudModule get() {
      return Modules.get(HudModule.class);
   }

   boolean coordinates() {
      return this.coordinates.get();
   }

   boolean netherCoordinates() {
      return this.netherCoordinates.get();
   }

   boolean ping() {
      return this.ping.get();
   }

   boolean fps() {
      return this.fps.get();
   }

   boolean clock() {
      return this.clock.get();
   }

   boolean logo() {
      return this.logo.get();
   }

   boolean moduleList() {
      return this.moduleList.get();
   }

   boolean potions() {
      return this.potions.get();
   }

   boolean armorStatus() {
      return this.armorStatus.get();
   }

   boolean armorColumn() {
      return this.armorLayout.is("Column");
   }

   boolean armorHands() {
      return this.armorHands.get();
   }

   boolean itemCooldowns() {
      return this.itemCooldowns.get();
   }

   boolean cooldownSandbar() {
      return this.cooldownDial.is("Sandbar");
   }

   boolean shadow() {
      return this.shadow.get();
   }

   float rounding() {
      return this.rounding.get();
   }

   /** ABGR fill for an element: the info bar is a solid card, the row-style elements are lighter. */
   int fill(boolean card) {
      float opacity = card ? this.cardOpacity.get() : this.rowOpacity.get();
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F);
      return HudChrome.abgr(this.background.argb() & 0xFFFFFF | a << 24);
   }

   /** ARGB accent: the custom one, or the Venti theme accent. */
   int accentArgb() {
      return this.customAccent.get() ? this.accentColor.argb() | 0xFF000000 : themeAccent();
   }

   static int themeAccent() {
      Theme theme = Theme.get();
      if (theme != null) {
         for (Setting<?> setting : theme.settings().all()) {
            if (setting instanceof ColorSetting color && "accent".equals(setting.key())) {
               return color.argb() | 0xFF000000;
            }
         }
      }

      return DEFAULT_ACCENT;
   }

   ArmorWear armorWear() {
      return switch (this.armorWear.get()) {
         case "Percent" -> ArmorWear.PERCENT;
         case "Gauge + Percent" -> ArmorWear.BOTH;
         default -> ArmorWear.GAUGE;
      };
   }

   enum ArmorWear {
      GAUGE,
      PERCENT,
      BOTH;
   }
}
