package dev.venti.modules.spotify.hud;

import dev.venti.gui.image.ThemeImages;
import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.spotify.SpotifyHud;
import dev.venti.modules.spotify.sky.SkyLyrics;
import dev.venti.modules.spotify.spotify.NowPlaying;
import imgui.ImDrawList;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

/**
 * The glowing sky-lyrics text drawn flat on the HUD instead of in the sky: the current line big, the song title
 * small above it, with the same fade and rise between lines. The text images come from {@link SkyLyrics}.
 */
final class ScreenLyrics {
   private static final float LINE_PX = 96.0F;
   private static final float TITLE_PX = 64.0F;
   private static final float FADE_RATE = 3.2F;
   private static final String PREVIEW = "Screen lyrics preview";
   private final ExecutorService raster = Executors.newSingleThreadExecutor(task -> {
      Thread thread = new Thread(task, "Venti Screen Lyrics");
      thread.setDaemon(true);
      return thread;
   });
   private final Slot line = new Slot(LINE_PX);
   private final Slot title = new Slot(TITLE_PX);
   private long lastFrame;

   /** True while anything is on screen or fading out. */
   boolean visible() {
      return !this.line.idle() || !this.title.idle();
   }

   void render(ImDrawList dl, boolean editMode, float screenW, float screenH) {
      long now = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min((now - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = now;
      SpotifyHud module = SpotifyHud.active();
      boolean on = module != null && module.screenLyrics();
      NowPlaying track = on ? module.nowPlaying() : null;
      boolean playing = track != null && track.playing();
      String lineText = on && playing ? SkyLyrics.lineNow(module, track) : "";
      String titleText = on && playing && module.screenTitle() ? track.title().strip() : "";
      if (editMode && on && lineText.isEmpty()) {
         lineText = PREVIEW;
      }

      this.line.want(lineText, this.raster);
      this.title.want(titleText, this.raster);
      this.line.step(dt);
      this.title.step(dt);
      if (!this.visible() || module == null) {
         return;
      }

      float s = ImGuiManager.getInstance().uiScale() * module.screenSize();
      float centerX = module.screenX() / 100.0F * screenW;
      float lineY = module.screenY() / 100.0F * screenH;
      float lineH = 34.0F * s;
      float titleH = 20.0F * s;
      int glow = module.skyGlowColor();
      Colors lineColors = new Colors(module.skyTextColor(), glow, module.sungWordColor(), module.sungWordGlow(), module.skyGlow(), module.skyOpacity());
      Colors titleColors = new Colors(module.skyTitleColor(), glow, module.skyTitleColor(), glow, module.skyGlow(), module.skyOpacity());
      float progress = module.wordHighlight() && track != null ? SkyLyrics.lineProgress(module, track) : -1.0F;
      this.line.draw(dl, centerX, lineY, lineH, lineColors, progress, s);
      this.title.draw(dl, centerX, lineY - lineH * 0.5F - titleH * 0.9F, titleH, titleColors, -1.0F, s);
   }

   void release() {
      this.line.close();
      this.title.close();
   }

   private static final class Shown {
      final ThemeImages.Texture fill;
      final ThemeImages.Texture glow;
      final String text;
      final float[] edges;
      float alpha;

      Shown(ThemeImages.Texture fill, ThemeImages.Texture glow, String text, float[] edges) {
         this.fill = fill;
         this.glow = glow;
         this.text = text;
         this.edges = edges;
      }

      void close() {
         GL11.glDeleteTextures(this.fill.id());
         GL11.glDeleteTextures(this.glow.id());
      }
   }

   private static final class Slot {
      private final float pixels;
      private String wanted = "";
      private String pending;
      private String makingText;
      private CompletableFuture<SkyLyrics.TextImage> making;
      private Shown current;
      private Shown leaving;

      Slot(float pixels) {
         this.pixels = pixels;
      }

      boolean idle() {
         return this.current == null && this.leaving == null && this.making == null;
      }

      void want(String text, ExecutorService raster) {
         if (!text.equals(this.wanted)) {
            this.wanted = text;
            this.pending = text;
         }

         if (this.pending != null && this.making == null) {
            String next = this.pending;
            this.pending = null;
            if (next.isEmpty()) {
               this.retire();
            } else {
               this.makingText = next;
               this.making = CompletableFuture.supplyAsync(() -> SkyLyrics.rasterizeText(next, this.pixels), raster);
            }
         }

         if (this.making != null && this.making.isDone()) {
            CompletableFuture<SkyLyrics.TextImage> done = this.making;
            this.making = null;
            try {
               SkyLyrics.TextImage image = done.join();
               this.retire();
               this.current = new Shown(
                  upload(image.fill(), image.width(), image.height()), upload(image.glow(), image.width(), image.height()), this.makingText, image.edges()
               );
            } catch (RuntimeException ignored) {
            }
         }
      }

      private void retire() {
         if (this.current != null) {
            if (this.leaving != null) {
               this.leaving.close();
            }

            this.leaving = this.current;
            this.current = null;
         }
      }

      void step(float dt) {
         if (this.current != null) {
            this.current.alpha = Math.min(1.0F, this.current.alpha + dt * FADE_RATE);
         }

         if (this.leaving != null) {
            this.leaving.alpha -= dt * FADE_RATE;
            if (this.leaving.alpha <= 0.0F) {
               this.leaving.close();
               this.leaving = null;
            }
         }
      }

      /** {@code progress} is how far the current line is sung; -1 turns word highlighting off. */
      void draw(ImDrawList dl, float centerX, float centerY, float height, Colors colors, float progress, float s) {
         if (this.leaving != null) {
            drawShown(dl, this.leaving, centerX, centerY - (1.0F - this.leaving.alpha) * 8.0F * s, height, colors, progress < 0.0F ? -1.0F : 1.0F);
         }

         if (this.current != null) {
            float settle = 1.0F - this.current.alpha;
            float lit = progress >= 0.0F && this.current.text.equals(this.wanted) ? progress : -1.0F;
            drawShown(dl, this.current, centerX, centerY + settle * settle * 8.0F * s, height, colors, lit);
         }
      }

      void close() {
         if (this.current != null) {
            this.current.close();
         }

         if (this.leaving != null) {
            this.leaving.close();
         }

         this.current = null;
         this.leaving = null;
      }
   }

   private record Colors(int text, int glow, int sungText, int sungGlow, float glowAmount, float opacity) {
   }

   private static void drawShown(ImDrawList dl, Shown shown, float centerX, float centerY, float height, Colors colors, float progress) {
      float alpha = Math.max(0.0F, Math.min(1.0F, shown.alpha)) * colors.opacity();
      if (alpha > 0.003F) {
         // The text sits inside a padded image (room for the glow), so scale by the image's own height.
         float h = height * shown.fill.height() / Math.max(1.0F, shown.fill.height() - 56.0F);
         float w = h * shown.fill.width() / Math.max(1.0F, shown.fill.height());
         float x = centerX - w * 0.5F;
         float y = centerY - h * 0.5F;
         float lit = progress < 0.0F ? 0.0F : SkyLyrics.litX(shown.edges, progress) / Math.max(1, shown.fill.width());
         float split = x + w * lit;
         if (colors.glowAmount() > 0.0F) {
            dl.addImage(shown.glow.id(), split, y, x + w, y + h, lit, 0.0F, 1.0F, 1.0F, abgr(colors.glow(), alpha * colors.glowAmount()));
            if (lit > 0.0F) {
               dl.addImage(shown.glow.id(), x, y, split, y + h, 0.0F, 0.0F, lit, 1.0F, abgr(colors.sungGlow(), alpha * colors.glowAmount()));
            }
         }

         dl.addImage(shown.fill.id(), split, y, x + w, y + h, lit, 0.0F, 1.0F, 1.0F, abgr(colors.text(), alpha * (colors.text() >>> 24) / 255.0F));
         if (lit > 0.0F) {
            dl.addImage(shown.fill.id(), x, y, split, y + h, 0.0F, 0.0F, lit, 1.0F, abgr(colors.sungText(), alpha * (colors.sungText() >>> 24) / 255.0F));
         }
      }
   }

   private static ThemeImages.Texture upload(int[] argb, int width, int height) {
      ByteBuffer rgba = MemoryUtil.memAlloc(width * height * 4);
      try {
         for (int i = 0; i < width * height; i++) {
            int c = argb[i];
            rgba.put((byte)(c >> 16)).put((byte)(c >> 8)).put((byte)c).put((byte)(c >>> 24));
         }

         rgba.flip();
         return ThemeImages.fromPixels(rgba, width, height, false);
      } finally {
         MemoryUtil.memFree(rgba);
      }
   }

   private static int abgr(int argb, float alpha) {
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      return a << 24 | (argb & 0xFF) << 16 | argb & 0xFF00 | argb >> 16 & 0xFF;
   }
}
