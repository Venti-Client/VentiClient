package dev.venti.modules.hud;

import dev.venti.gui.Theme;
import dev.venti.module.Module;
import dev.venti.module.Modules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Animated rows of the module list, widest name first (from the Anubis ModuleListRows). */
final class ModuleListRows {
   private static final Comparator<ModuleListRows.Row> WIDEST_FIRST = Comparator.<ModuleListRows.Row>comparingDouble(row -> -row.nameW)
      .thenComparing(row -> row.module.name());
   private static final Comparator<ModuleListRows.Row> WIDEST_LAST = WIDEST_FIRST.reversed();
   private final List<ModuleListRows.Row> rows = new ArrayList<>();
   private final List<ModuleListRows.Row> view = Collections.unmodifiableList(this.rows);
   private boolean unsorted;
   private boolean sortedBottomFirst;

   void update(float dt) {
      for (Module module : Modules.all()) {
         if (module.isEnabled() && listed(module) && !this.hasRow(module)) {
            this.rows.add(new ModuleListRows.Row(module));
            this.unsorted = true;
         }
      }

      float k = 1.0F - (float)Math.exp(-12.0F * dt);

      for (int i = this.rows.size() - 1; i >= 0; i--) {
         ModuleListRows.Row row = this.rows.get(i);
         float target = row.module.isEnabled() ? 1.0F : 0.0F;
         if (row.anim != target) {
            float next = row.anim + (target - row.anim) * k;
            if (Math.abs(next - target) < 0.005F) {
               next = target;
            }

            row.anim = next;
            if (next == 0.0F) {
               this.rows.remove(i);
            }
         }
      }
   }

   <F> List<ModuleListRows.Row> ordered(F font, int px, ModuleListRows.TextWidth<F> width, boolean bottomFirst) {
      for (int i = 0; i < this.rows.size(); i++) {
         ModuleListRows.Row row = this.rows.get(i);
         if (row.measuredPx != px || row.measuredFont != font) {
            row.nameW = width.of(font, px, row.module.name());
            row.measuredFont = font;
            row.measuredPx = px;
            this.unsorted = true;
         }
      }

      if (this.unsorted || bottomFirst != this.sortedBottomFirst) {
         this.rows.sort(bottomFirst ? WIDEST_LAST : WIDEST_FIRST);
         this.unsorted = false;
         this.sortedBottomFirst = bottomFirst;
      }

      return this.view;
   }

   /** The GUI theme and the HUD's own modules are always on, so they stay out of the list. */
   private static boolean listed(Module module) {
      return !(module instanceof Theme) && !(module instanceof HudModule) && !(module instanceof HotkeysModule);
   }

   private boolean hasRow(Module module) {
      for (int i = 0; i < this.rows.size(); i++) {
         if (this.rows.get(i).module == module) {
            return true;
         }
      }

      return false;
   }

   static final class Row {
      private final Module module;
      private float anim;
      private float nameW;
      private Object measuredFont;
      private int measuredPx = -1;

      private Row(Module module) {
         this.module = module;
      }

      Module module() {
         return this.module;
      }

      float anim() {
         return this.anim;
      }

      float nameW() {
         return this.nameW;
      }
   }

   @FunctionalInterface
   interface TextWidth<F> {
      float of(F font, int px, String text);
   }
}
