package dev.venti.gui;

import imgui.ImGui;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

/**
 * Tiny easing store for the click GUI: each animated thing has an id and eases toward its target every frame,
 * so switches slide, highlights fade and panels expand instead of jumping.
 */
final class GuiAnim {
   private static final Int2FloatOpenHashMap VALUES = new Int2FloatOpenHashMap();

   private GuiAnim() {
   }

   /** Eases the value for {@code id} toward {@code target}; {@code speed} ~ 1 / seconds to settle. Starts at the target. */
   static float approach(int id, float target, float speed) {
      float value = VALUES.containsKey(id) ? VALUES.get(id) : target;
      float dt = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
      value += (target - value) * (1.0F - (float)Math.exp(-speed * dt));
      if (Math.abs(target - value) < 0.002F) {
         value = target;
      }

      VALUES.put(id, value);
      return value;
   }

   /** Same, keyed by an ImGui id string within the current ID stack (so "##v" in different rows stays separate). */
   static float approach(String id, float target, float speed) {
      return approach(ImGui.getID(id), target, speed);
   }

   /** Jumps the value so the next approach animates from here (used to restart a page fade). */
   static void set(int id, float value) {
      VALUES.put(id, value);
   }

   static float ease(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   /** Blends two ImGui (ABGR) colors channel by channel, alpha included. */
   static int mix(int from, int to, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int out = 0;
      for (int shift = 0; shift < 32; shift += 8) {
         int a = from >>> shift & 0xFF;
         int b = to >>> shift & 0xFF;
         out |= Math.round(a + (b - a) * t) << shift;
      }

      return out;
   }
}
