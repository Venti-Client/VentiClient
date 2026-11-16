package dev.venti.modules.view;

import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;

/** Holds the sprint input down, like vanilla's Toggle Sprint option. Vanilla still decides whether you can sprint. */
public final class SprintModule extends Module {
   private static volatile SprintModule active;

   public SprintModule() {
      super("sprint", "Sprint", "Keeps you sprinting while you move forward, like vanilla Toggle Sprint.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static boolean holding() {
      return active != null;
   }
}
