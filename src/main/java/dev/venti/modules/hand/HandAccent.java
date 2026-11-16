package dev.venti.modules.hand;

import dev.venti.config.ColorSetting;
import dev.venti.config.Setting;
import dev.venti.gui.Theme;

/** The GUI theme accent colour, used by the "Theme"/"Accent" colour modes. */
final class HandAccent {
   private static final int FALLBACK = 0xFFF0435A;

   private HandAccent() {
   }

   static int argb() {
      Theme theme = Theme.get();
      if (theme != null) {
         // Theme.accent is package-private; read it through the public settings list instead.
         for (Setting<?> setting : theme.settings().all()) {
            if (setting instanceof ColorSetting color && "accent".equals(setting.key())) {
               return color.argb() | 0xFF000000;
            }
         }
      }

      return FALLBACK;
   }
}
