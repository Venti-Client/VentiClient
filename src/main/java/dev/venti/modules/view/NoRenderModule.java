package dev.venti.modules.view;

import dev.venti.config.BooleanSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;

public final class NoRenderModule extends Module {
   private static NoRenderModule instance;
   private final BooleanSetting fireOverlay = this.add("fire-overlay", new BooleanSetting("Fire Overlay", true));
   private final BooleanSetting totemAnimation = this.add("totem-animation", new BooleanSetting("Totem Animation", true));
   private final BooleanSetting pumpkinPowderSnow = this.add("pumpkin-powder-snow", new BooleanSetting("Pumpkin & Powder Snow", true));
   private final BooleanSetting nauseaPortal = this.add("nausea-portal", new BooleanSetting("Nausea & Portal", true));
   private final BooleanSetting blindnessDarkness = this.add("blindness-darkness", new BooleanSetting("Blindness & Darkness", true));
   private final BooleanSetting spawnerMob = this.add("spawner-mob", new BooleanSetting("Spawner Mob", true));
   private final BooleanSetting fallingBlocks = this.add("falling-blocks", new BooleanSetting("Falling Blocks", false));
   private final BooleanSetting weather = this.add("weather", new BooleanSetting("Weather", true));
   private final BooleanSetting lavaFog = this.add("lava-fog", new BooleanSetting("Lava Fog", true));
   private final BooleanSetting entityFire = this.add("entity-fire", new BooleanSetting("Entity Fire", false));

   public NoRenderModule() {
      super("no-render", "No Render", "Hides selected overlays, screen effects and entity renders.", ModuleCategory.VISUALS);
      instance = this;
   }

   private static boolean on(BooleanSetting setting) {
      NoRenderModule module = instance;
      return module != null && module.isEnabled() && setting.get();
   }

   public static boolean hideFireOverlay() {
      return instance != null && on(instance.fireOverlay);
   }

   public static boolean hideTotemAnimation() {
      return instance != null && on(instance.totemAnimation);
   }

   public static boolean hidePumpkinPowderSnow() {
      return instance != null && on(instance.pumpkinPowderSnow);
   }

   public static boolean hideNauseaPortal() {
      return instance != null && on(instance.nauseaPortal);
   }

   public static boolean hideBlindnessDarkness() {
      return instance != null && on(instance.blindnessDarkness);
   }

   public static boolean hideSpawnerMob() {
      return instance != null && on(instance.spawnerMob);
   }

   public static boolean hideFallingBlocks() {
      return instance != null && on(instance.fallingBlocks);
   }

   public static boolean hideWeather() {
      return instance != null && on(instance.weather);
   }

   public static boolean hideLavaFog() {
      return instance != null && on(instance.lavaFog);
   }

   public static boolean hideEntityFire() {
      return instance != null && on(instance.entityFire);
   }
}
