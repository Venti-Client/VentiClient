package dev.venti.modules.hud;

import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;

/** Lists the modules bound to a key, and their keys, in a HUD panel drawn by {@link HudRenderer}. */
public final class HotkeysModule extends Module {
   public HotkeysModule() {
      super("hotkeys", "Hotkeys", "Lists the modules you have bound to a key, and their keys. Drag and scroll the panel while this page is open.", ModuleCategory.HUD);
   }

   public static HotkeysModule get() {
      return Modules.get(HotkeysModule.class);
   }

   static boolean shouldShowHud() {
      HotkeysModule module = get();
      return module != null && module.isEnabled();
   }
}
