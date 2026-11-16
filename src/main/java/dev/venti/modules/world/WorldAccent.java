package dev.venti.modules.world;

import dev.venti.config.Setting;
import dev.venti.gui.Theme;

/** The GUI accent colour, used where the Anubis originals followed the client theme. */
final class WorldAccent {
   private static final int FALLBACK = 0xFFF0435A;

   private WorldAccent() {
   }

   static int argb() {
      Theme theme = Theme.get();
      if (theme != null) {
         for (Setting<?> setting : theme.settings().all()) {
            if ("accent".equals(setting.key()) && setting.get() instanceof Integer argb) {
               return argb | 0xFF000000;
            }
         }
      }

      return FALLBACK;
   }
}
