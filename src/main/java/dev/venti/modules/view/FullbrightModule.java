package dev.venti.modules.view;

import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;

public final class FullbrightModule extends Module {
   private static FullbrightModule instance;
   private final NumberSetting gamma = this.add("gamma", new NumberSetting("Gamma", 8.0F, 1.0F, 16.0F).step(0.5F));

   public FullbrightModule() {
      super("fullbright", "Fullbright", "Brightens dark areas by overriding the client lightmap gamma.", ModuleCategory.VISUALS);
      instance = this;
   }

   /** The gamma to feed the lightmap, or {@code vanilla} when the module is off. */
   public static float gamma(float vanilla) {
      FullbrightModule module = instance;
      return module != null && module.isEnabled() ? Math.max(1.0F, module.gamma.get()) : vanilla;
   }
}
