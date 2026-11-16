package dev.venti.modules.spotify.hud;

public final class GuiPalette {
   private GuiPalette() {
   }

   /** ImGui packs colors as ABGR; settings store ARGB. */
   public static int abgr(int argb) {
      return argb & -16711936 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
   }

   public static int withAlpha(int argb, float alpha) {
      return argb & 0xFFFFFF | Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24;
   }

   public static int lerpColor(int c1, int c2, float t) {
      int r1 = c1 & 0xFF;
      int r2 = c2 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b1 = c1 >> 16 & 0xFF;
      int b2 = c2 >> 16 & 0xFF;
      int a1 = c1 >>> 24;
      int a2 = c2 >>> 24;
      return (int)(a1 + (a2 - a1) * t) << 24 | (int)(b1 + (b2 - b1) * t) << 16 | (int)(g1 + (g2 - g1) * t) << 8 | (int)(r1 + (r2 - r1) * t);
   }
}
