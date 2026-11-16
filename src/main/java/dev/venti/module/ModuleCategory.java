package dev.venti.module;

public enum ModuleCategory {
   VISUALS("Visuals"),
   RENDER("Render"),
   HUD("HUD"),
   MISC("Misc");

   public final String label;

   ModuleCategory(String label) {
      this.label = label;
   }
}
