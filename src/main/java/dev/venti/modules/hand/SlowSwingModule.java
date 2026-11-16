package dev.venti.modules.hand;

import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;

/** Slow Swing: scales how long your hand swing takes (ported from Anubis by 4ldenz). */
public final class SlowSwingModule extends Module {
   private final NumberSetting speed = this.add("speed", new NumberSetting("Speed", 0.25F, 0.1F, 2.0F).step(0.05F));

   public SlowSwingModule() {
      super("slow-swing", "Slow Swing", "Control hand swing animation speed.", ModuleCategory.VISUALS);
   }

   /** The running module, or null. */
   public static SlowSwingModule active() {
      SlowSwingModule module = Modules.get(SlowSwingModule.class);
      return module != null && module.isEnabled() ? module : null;
   }

   public int getModifiedSwingDuration(int vanilla) {
      float s = this.speed.get();
      if (s <= 0.0F) {
         s = 0.1F;
      }

      return Math.max(1, Math.round(vanilla / s));
   }
}
