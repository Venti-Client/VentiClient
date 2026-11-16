package dev.venti.modules.spotify;

import com.sun.jna.Platform;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.Settings;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import dev.venti.modules.spotify.spotify.AlbumArt;
import dev.venti.modules.spotify.spotify.Lyrics;
import dev.venti.modules.spotify.spotify.LyricsQuery;
import dev.venti.modules.spotify.spotify.LyricsService;
import dev.venti.modules.spotify.spotify.MediaSession;
import dev.venti.modules.spotify.spotify.NowPlaying;
import dev.venti.modules.spotify.spotify.PlayerSource;
import dev.venti.modules.spotify.spotify.SpotifyWindow;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Anubis client "Spotify HUD" module, without the Anubis module framework:
 * it is registered and driven like every other Venti module.
 */
@Environment(EnvType.CLIENT)
public final class SpotifyHud extends Module {
   private static final long KEY_COOLDOWN_MS = 200L;
   private final BooleanSetting showArtist = this.add("show-artist", new BooleanSetting("Show Artist", true));
   private final BooleanSetting showAlbumArt = this.add("show-album-art", new BooleanSetting("Show Album Art", true));
   private final BooleanSetting showProgress = this.add("show-progress", new BooleanSetting("Show Progress", true));
   private final BooleanSetting scrollTitles = this.add("scroll-titles", new BooleanSetting("Scroll Long Titles", true));
   private final BooleanSetting showLyrics = this.add("show-lyrics", new BooleanSetting("Show Lyrics", true));
   private final ModeSetting lyricLines = this.add("lyric-lines", new ModeSetting("Lyric Lines", "2", "1", "2", "3"));
   private final ModeSetting lyricMotion = this.add("lyric-motion", new ModeSetting("Lyric Motion", "Wave", "Wave", "Glide", "Fade"));
   private final ModeSetting lyricHighlight = this.add("lyric-highlight", new ModeSetting("Lyric Highlight", "Karaoke", "Karaoke", "Line", "Off"));
   private final BooleanSetting lyricsOnline = this.add("lyrics-online", new BooleanSetting("Lyrics Online", true));
   private final BooleanSetting skyLyrics = this.add("sky-lyrics", new BooleanSetting("Sky Lyrics", true));
   private final BooleanSetting skyTitle = this.add("sky-title", new BooleanSetting("Sky Song Title", false));
   private final BooleanSetting skyEmojis = this.add("sky-emojis", new BooleanSetting("Sky Emojis", true));
   private final ModeSetting skyEmojiStyle = this.add("sky-emoji-style", new ModeSetting("Sky Emoji Style", "Behind", "Beside", "Behind", "Replace Word"));
   private final BooleanSetting skyFollow = this.add("sky-follow", new BooleanSetting("Sky Follows View", true));
   private final NumberSetting skyHeight = this.add("sky-height", new NumberSetting("Sky Height", 10.0F, -30.0F, 70.0F).step(1.0F));
   private final NumberSetting skyOffset = this.add("sky-offset", new NumberSetting("Sky Side Offset", 0.0F, -180.0F, 180.0F).step(1.0F));
   private final NumberSetting skySize = this.add("sky-size", new NumberSetting("Sky Text Size", 1.25F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyTitleSize = this.add("sky-title-size", new NumberSetting("Sky Title Size", 1.0F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyEmojiSize = this.add("sky-emoji-size", new NumberSetting("Sky Emoji Size", 1.35F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting skyOpacity = this.add("sky-opacity", new NumberSetting("Sky Opacity", 0.9F, 0.1F, 1.0F).step(0.05F));
   private final ColorSetting skyTextColor = this.add("sky-text-color", new ColorSetting("Sky Text Color", -1));
   private final ColorSetting skyTitleColor = this.add("sky-title-color", new ColorSetting("Sky Title Color", -1447442));
   private final ColorSetting skyGlowColor = this.add("sky-glow-color", new ColorSetting("Sky Glow Color", -7713281));
   private final NumberSetting skyGlow = this.add("sky-glow", new NumberSetting("Sky Glow", 0.8F, 0.0F, 1.0F).step(0.05F));
   private final BooleanSetting wordHighlight = this.add("word-highlight", new BooleanSetting("Sung Word Highlight", true));
   private final ColorSetting sungWordColor = this.add("sung-word-color", new ColorSetting("Sung Word Color", 0xFFFFF4C2));
   private final ColorSetting sungWordGlow = this.add("sung-word-glow", new ColorSetting("Sung Word Glow", 0xFF1ED760));
   private final BooleanSetting screenLyrics = this.add("screen-lyrics", new BooleanSetting("Screen Lyrics", false));
   private final BooleanSetting screenTitle = this.add("screen-title", new BooleanSetting("Screen Song Title", true));
   private final NumberSetting screenX = this.add("screen-x", new NumberSetting("Screen Lyrics X %", 50.0F, 0.0F, 100.0F).step(1.0F));
   private final NumberSetting screenY = this.add("screen-y", new NumberSetting("Screen Lyrics Y %", 30.0F, 0.0F, 100.0F).step(1.0F));
   private final NumberSetting screenSize = this.add("screen-size", new NumberSetting("Screen Lyrics Size", 1.0F, 0.3F, 3.0F).step(0.05F));
   private final NumberSetting verticalPosition = this.add("vertical-position", new NumberSetting("Vertical Position", 30.0F, 0.0F, 150.0F).step(1.0F));
   private final NumberSetting cardWidth = this.add("card-width", new NumberSetting("Card Width", 320.0F, 200.0F, 560.0F).step(1.0F));
   private final ColorSetting titleColor = this.add("title-color", new ColorSetting("Title Color", -1447442));
   private final ColorSetting artistColor = this.add("artist-color", new ColorSetting("Artist Color", -6052949));
   private final ColorSetting lyricsColor = this.add("lyrics-color", new ColorSetting("Lyrics Color", -657929));
   private final ColorSetting upcomingLyricsColor = this.add("upcoming-lyrics-color", new ColorSetting("Upcoming Lyrics Color", -6052949));
   private final ColorSetting highlightColor = this.add("highlight-color", new ColorSetting("Highlight Color", -14756000));
   private final ColorSetting accentColor = this.add("accent-color", new ColorSetting("Accent Color", 0xFFF0435A));
   private final ColorSetting backgroundColor = this.add("background-color", new ColorSetting("Background Color", -15329251));
   private final NumberSetting backgroundOpacity = this.add("background-opacity", new NumberSetting("Background Opacity", 0.85F, 0.2F, 1.0F).step(0.01F));
   private final NumberSetting cornerRadius = this.add("corner-radius", new NumberSetting("Corner Radius", 8.0F, 0.0F, 16.0F).step(1.0F));
   private final BooleanSetting shadow = this.add("shadow", new BooleanSetting("Card Shadow", true));
   private final BooleanSetting showWhenIdle = this.add("show-when-idle", new BooleanSetting("Show When Idle", false));
   private volatile NowPlaying nowPlaying = NowPlaying.WAITING;
   private volatile SpotifyHud.TrackLyrics lyrics;
   private volatile SpotifyHud.Poller poller;
   private final Object publishLock = new Object();
   private long lastKeyAt;

   public SpotifyHud() {
      super("spotify-hud", "Spotify HUD", "Shows what Spotify is playing, with synced lyrics and optional media keys.", ModuleCategory.HUD, true);
      this.artistColor.visibleWhen(this.showArtist::get);
      this.lyricsColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Line"));
      this.upcomingLyricsColor.visibleWhen(this.showLyrics::get);
      this.highlightColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Off"));
      this.lyricLines.visibleWhen(this.showLyrics::get);
      this.lyricMotion.visibleWhen(this.showLyrics::get);
      this.lyricHighlight.visibleWhen(this.showLyrics::get);
      this.lyricsOnline.visibleWhen(this.showLyrics::get);
      this.skyLyrics.visibleWhen(this.showLyrics::get);
      this.skyTitle.visibleWhen(this::skyLyrics);
      this.skyEmojis.visibleWhen(this::skyLyrics);
      this.skyFollow.visibleWhen(this::skyLyrics);
      this.skyHeight.visibleWhen(this::skyLyrics);
      this.skyOffset.visibleWhen(this::skyLyrics);
      this.screenLyrics.visibleWhen(this.showLyrics::get);
      this.wordHighlight.visibleWhen(() -> this.skyLyrics() || this.screenLyrics());
      this.sungWordColor.visibleWhen(() -> (this.skyLyrics() || this.screenLyrics()) && this.wordHighlight.get());
      this.sungWordGlow.visibleWhen(() -> (this.skyLyrics() || this.screenLyrics()) && this.wordHighlight.get());
      this.screenTitle.visibleWhen(this::screenLyrics);
      this.screenX.visibleWhen(this::screenLyrics);
      this.screenY.visibleWhen(this::screenLyrics);
      this.screenSize.visibleWhen(this::screenLyrics);
      this.skySize.visibleWhen(this::skyLyrics);
      this.skyTitleSize.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
      this.skyEmojiStyle.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
      this.skyEmojiSize.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
      this.skyOpacity.visibleWhen(() -> this.skyLyrics() || this.screenLyrics());
      this.skyTextColor.visibleWhen(() -> this.skyLyrics() || this.screenLyrics());
      this.skyTitleColor.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get() || this.screenLyrics() && this.screenTitle.get());
      this.skyGlowColor.visibleWhen(() -> this.skyLyrics() || this.screenLyrics());
      this.skyGlow.visibleWhen(() -> this.skyLyrics() || this.screenLyrics());
      this.lyricsOnline.onChange(() -> {
         SpotifyHud.Poller running = this.poller;
         if (running != null) {
            running.lyricsService.setOnline(this.lyricsOnline.get());
         }
      });
   }

   public static SpotifyHud get() {
      return Modules.get(SpotifyHud.class);
   }

   public static SpotifyHud active() {
      SpotifyHud module = get();
      return module != null && module.isEnabled() ? module : null;
   }

   @Override
   protected void onEnable() {
      if (!PlayerSource.supported()) {
         synchronized (this.publishLock) {
            this.nowPlaying = NowPlaying.UNSUPPORTED;
            this.lyrics = null;
         }

         this.chat(Text.literal("Not supported").formatted(Formatting.RED)
            .append(Text.literal(", only the Spotify app on Windows, macOS and Linux works").formatted(Formatting.GRAY)));
      } else {
         SpotifyHud.Poller started = new SpotifyHud.Poller();
         synchronized (this.publishLock) {
            this.poller = started;
            this.nowPlaying = NowPlaying.WAITING;
            this.lyrics = null;
         }

         started.start();
      }
   }

   @Override
   protected void onDisable() {
      SpotifyHud.Poller stopped;
      synchronized (this.publishLock) {
         stopped = this.poller;
         this.poller = null;
         this.nowPlaying = PlayerSource.supported() ? NowPlaying.WAITING : NowPlaying.UNSUPPORTED;
         this.lyrics = null;
      }

      if (stopped != null) {
         stopped.stop();
      }
   }

   private void publish(SpotifyHud.Poller from, NowPlaying state, SpotifyHud.TrackLyrics trackLyrics) {
      synchronized (this.publishLock) {
         if (from == this.poller) {
            this.nowPlaying = state;
            this.lyrics = trackLyrics;
         }
      }
   }

   public static void hudButton(SpotifyWindow.MediaKey key) {
      SpotifyHud module = active();
      if (module != null) {
         module.press(key);
      }
   }

   public static boolean shouldShowHud() {
      SpotifyHud module = active();
      if (module == null) {
         return false;
      } else {
         NowPlaying now = module.nowPlaying;
         return now.status() != NowPlaying.Status.UNSUPPORTED && now.status() != NowPlaying.Status.WAITING ? now.active() || module.showWhenIdle.get() : false;
      }
   }

   public NowPlaying nowPlaying() {
      return this.nowPlaying;
   }

   public Lyrics lyricsFor(NowPlaying now) {
      SpotifyHud.TrackLyrics current = this.lyrics;
      SpotifyHud.Poller running = this.poller;
      if (current != null && running != null && this.wantsLyrics(now)) {
         return current.title().equals(now.title()) && current.artist().equals(now.artist()) ? running.lyricsService.lookup(current.query()) : null;
      } else {
         return null;
      }
   }

   public boolean wantsLyrics(NowPlaying now) {
      return this.showLyrics.get() && now != null && now.lyricsEligible();
   }

   public boolean showArtist() {
      return this.showArtist.get();
   }

   public boolean showAlbumArt() {
      return this.showAlbumArt.get();
   }

   public boolean showProgress() {
      return this.showProgress.get();
   }

   public boolean scrollTitles() {
      return this.scrollTitles.get();
   }

   public boolean showLyrics() {
      return this.showLyrics.get();
   }

   public boolean skyLyrics() {
      return this.showLyrics.get() && this.skyLyrics.get();
   }

   public boolean skyTitle() {
      return this.skyTitle.get();
   }

   public boolean skyEmojis() {
      return this.skyEmojis.get();
   }

   public SpotifyHud.SkyEmojiStyle skyEmojiStyle() {
      return switch (this.skyEmojiStyle.get()) {
         case "Behind" -> SpotifyHud.SkyEmojiStyle.BEHIND;
         case "Replace Word" -> SpotifyHud.SkyEmojiStyle.REPLACE_WORD;
         default -> SpotifyHud.SkyEmojiStyle.BESIDE;
      };
   }

   public boolean skyFollow() {
      return this.skyFollow.get();
   }

   public float skyHeight() {
      return this.skyHeight.get();
   }

   /** Degrees to turn the sky text left (negative) or right of where it would sit. */
   public float skyOffset() {
      return this.skyOffset.get();
   }

   public float skySize() {
      return this.skySize.get();
   }

   public float skyTitleSize() {
      return this.skyTitleSize.get();
   }

   public float skyEmojiSize() {
      return this.skyEmojiSize.get();
   }

   public float skyOpacity() {
      return this.skyOpacity.get();
   }

   public int skyTextColor() {
      return this.skyTextColor.argb();
   }

   public int skyTitleColor() {
      return this.skyTitleColor.argb();
   }

   public int skyGlowColor() {
      return this.skyGlowColor.argb();
   }

   public float skyGlow() {
      return this.skyGlow.get();
   }

   public int lyricLines() {
      try {
         return Math.max(1, Math.min(3, Integer.parseInt(this.lyricLines.get())));
      } catch (NumberFormatException var2) {
         return 2;
      }
   }

   public SpotifyHud.LyricMotion lyricMotion() {
      return switch (this.lyricMotion.get()) {
         case "Glide" -> SpotifyHud.LyricMotion.GLIDE;
         case "Fade" -> SpotifyHud.LyricMotion.FADE;
         default -> SpotifyHud.LyricMotion.WAVE;
      };
   }

   public SpotifyHud.LyricHighlight lyricHighlight() {
      return switch (this.lyricHighlight.get()) {
         case "Line" -> SpotifyHud.LyricHighlight.LINE;
         case "Off" -> SpotifyHud.LyricHighlight.OFF;
         default -> SpotifyHud.LyricHighlight.KARAOKE;
      };
   }

   public boolean lyricsOnline() {
      return this.lyricsOnline.get();
   }

   /** Words already sung turn {@link #sungWordColor()} with their own glow, in the sky text and Screen Lyrics. */
   public boolean wordHighlight() {
      return this.wordHighlight.get();
   }

   public int sungWordColor() {
      return this.sungWordColor.argb();
   }

   public int sungWordGlow() {
      return this.sungWordGlow.argb();
   }

   /** The sky-lyrics look drawn flat on the HUD; it shares the sky text colors, glow and opacity. */
   public boolean screenLyrics() {
      return this.showLyrics.get() && this.screenLyrics.get();
   }

   public boolean screenTitle() {
      return this.screenTitle.get();
   }

   public float screenX() {
      return this.screenX.get();
   }

   public float screenY() {
      return this.screenY.get();
   }

   public float screenSize() {
      return this.screenSize.get();
   }

   /** Percent of the way down the screen; past 100 the card hangs below the bottom edge. */
   public float verticalPosition() {
      return this.verticalPosition.get();
   }

   public void setVerticalPosition(float percent) {
      this.verticalPosition.set(percent);
   }

   public float cardWidth() {
      return this.cardWidth.get();
   }

   public int titleColor() {
      return this.titleColor.argb();
   }

   public int artistColor() {
      return this.artistColor.argb();
   }

   public int lyricsColor() {
      return this.lyricsColor.argb();
   }

   public int upcomingLyricsColor() {
      return this.upcomingLyricsColor.argb();
   }

   public int highlightColor() {
      return this.highlightColor.argb();
   }

   public int accentColor() {
      return this.accentColor.argb();
   }

   public int backgroundColor() {
      return this.backgroundColor.argb();
   }

   public float backgroundOpacity() {
      return this.backgroundOpacity.get();
   }

   public float cornerRadius() {
      return this.cornerRadius.get();
   }

   public boolean shadow() {
      return this.shadow.get();
   }

   public void press(SpotifyWindow.MediaKey key) {
      long now = System.currentTimeMillis();
      SpotifyHud.Poller running = this.poller;
      if (running != null && now - this.lastKeyAt >= KEY_COOLDOWN_MS) {
         this.lastKeyAt = now;
         if (running.press(key)) {
            if (key == SpotifyWindow.MediaKey.PLAY_PAUSE) {
               synchronized (this.publishLock) {
                  if (this.poller == running) {
                     this.nowPlaying = this.nowPlaying.toggled();
                  }
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricHighlight {
      KARAOKE,
      LINE,
      OFF;
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricMotion {
      WAVE,
      GLIDE,
      FADE;
   }

   @Environment(EnvType.CLIENT)
   private final class Poller {
      final ScheduledThreadPoolExecutor executor;
      final LyricsService lyricsService = new LyricsService();
      private PlayerSource media;
      private boolean mediaFailed;
      private SpotifyWindow window;
      private boolean windowFailed;
      private long nextWindowReadAt;
      private AlbumArt art;
      private int artVersion;
      private boolean artDecoded;
      private LyricsQuery lyricsCandidate;
      private int lyricsCandidatePolls;
      private LyricsQuery lyricsQuery;
      private SpotifyHud.TrackLyrics published;
      private int sessionFailures;
      private String lastError;
      private long lastErrorLoggedAt;

      Poller() {
         this.lyricsService.setOnline(SpotifyHud.this.lyricsOnline.get());
         this.executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "SpotifyHUD-Poller");
            thread.setDaemon(true);
            return thread;
         });
         this.executor.setRemoveOnCancelPolicy(true);
         this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      }

      void start() {
         this.executor.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
      }

      void stop() {
         try {
            this.executor.execute(this::closeMedia);
         } catch (RejectedExecutionException var2) {
         }

         this.executor.shutdown();
         this.lyricsService.close();
      }

      private void closeMedia() {
         if (this.media != null) {
            this.media.close();
         }
      }

      boolean press(SpotifyWindow.MediaKey key) {
         try {
            this.executor.execute(() -> this.control(key));
            this.executor.schedule(this::poll, 250L, TimeUnit.MILLISECONDS);
            return true;
         } catch (RejectedExecutionException var3) {
            return false;
         }
      }

      private void poll() {
         if (SpotifyHud.this.poller == this) {
            try {
               NowPlaying next = this.readSession();
               if (next == null) {
                  if (this.sessionFailures > 0 && this.sessionFailures <= 2 && SpotifyHud.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                     return;
                  }

                  long now = System.currentTimeMillis();
                  if (SpotifyHud.this.nowPlaying.source() != NowPlaying.Source.SESSION && now < this.nextWindowReadAt) {
                     return;
                  }

                  this.nextWindowReadAt = now + 1000L;
                  next = this.readWindow();
               }

               if (next != null) {
                  SpotifyHud.this.publish(this, next, this.lyricsFor(next));
               }
            } catch (LinkageError | RuntimeException var4) {
               SpotifyHud.LOGGER.debug("[Venti] Spotify HUD: poll failed", var4);
            }
         }
      }

      private NowPlaying readSession() {
         if (this.mediaFailed) {
            return null;
         } else {
            if (this.media == null) {
               try {
                  this.media = PlayerSource.spotify();
               } catch (LinkageError var3) {
                  SpotifyHud.LOGGER.warn("[Venti] Spotify HUD: media session unavailable ({})", var3.toString());
                  this.mediaFailed = true;
                  return null;
               }
            }

            if (this.media.isUnsupported()) {
               return null;
            } else {
               MediaSession.Snapshot snapshot = this.media.poll();
               if (snapshot != null) {
                  this.sessionFailures = 0;
                  return NowPlaying.fromSession(snapshot, this.artFor(snapshot), SpotifyHud.this.nowPlaying);
               } else {
                  String error = this.media.lastError();
                  this.logError(error);
                  this.sessionFailures = error != null && !this.media.isUnsupported() ? this.sessionFailures + 1 : 0;
                  return null;
               }
            }
         }
      }

      private AlbumArt artFor(MediaSession.Snapshot snapshot) {
         if (!SpotifyHud.this.showAlbumArt.get()) {
            this.art = null;
            this.artDecoded = false;
            return null;
         } else {
            if (!this.artDecoded || snapshot.thumbnailVersion() != this.artVersion) {
               this.artDecoded = true;
               this.artVersion = snapshot.thumbnailVersion();
               this.art = null;
               if (snapshot.hasThumbnail()) {
                  try {
                     this.art = AlbumArt.decode(snapshot.thumbnail(), 256);
                  } catch (RuntimeException | LinkageError var3) {
                     SpotifyHud.LOGGER.debug("[Venti] Spotify HUD: album art could not be decoded", var3);
                  }
               }
            }

            return this.art;
         }
      }

      private NowPlaying readWindow() {
         if (!Platform.isWindows()) {
            if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
               return NowPlaying.NOT_RUNNING;
            } else {
               this.executor.shutdown();
               return NowPlaying.UNSUPPORTED;
            }
         } else {
            if (!this.windowFailed && this.window == null) {
               try {
                  this.window = new SpotifyWindow();
               } catch (LinkageError var2) {
                  this.windowFailed(var2);
               }
            }

            if (this.window != null) {
               try {
                  return NowPlaying.fromWindowTitle(this.window.readTitle(), SpotifyHud.this.nowPlaying);
               } catch (LinkageError var3) {
                  this.windowFailed(var3);
               }
            }

            if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
               return NowPlaying.NOT_RUNNING;
            } else {
               this.executor.shutdown();
               return NowPlaying.UNSUPPORTED;
            }
         }
      }

      private void windowFailed(LinkageError failure) {
         SpotifyHud.LOGGER.warn("[Venti] Spotify HUD: Win32 access unavailable ({})", failure.toString());
         this.window = null;
         this.windowFailed = true;
      }

      private SpotifyHud.TrackLyrics lyricsFor(NowPlaying now) {
         if (!SpotifyHud.this.wantsLyrics(now)) {
            this.lyricsCandidate = null;
            this.lyricsCandidatePolls = 0;
            this.lyricsQuery = null;
            this.published = null;
            return null;
         } else {
            LyricsQuery query = new LyricsQuery(now.title(), now.artist(), now.album(), 0).withDurationMillis(now.durationMs());
            if (query.equals(this.lyricsCandidate)) {
               this.lyricsCandidatePolls++;
            } else {
               this.lyricsCandidate = query;
               this.lyricsCandidatePolls = 1;
            }

            if (this.lyricsCandidatePolls >= 2) {
               this.lyricsQuery = this.lyricsCandidate;
            }

            if (this.lyricsQuery != null && this.lyricsQuery.title().equals(now.title()) && this.lyricsQuery.artist().equals(now.artist())) {
               if (this.published == null
                  || !this.published.query().equals(this.lyricsQuery)
                  || !this.published.title().equals(now.title())
                  || !this.published.artist().equals(now.artist())) {
                  this.published = new SpotifyHud.TrackLyrics(now.title(), now.artist(), this.lyricsQuery);
               }

               return this.published;
            } else {
               return this.published;
            }
         }
      }

      private void control(SpotifyWindow.MediaKey key) {
         if (SpotifyHud.this.poller == this) {
            if (this.media != null && SpotifyHud.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
               boolean accepted = switch (key) {
                  case PLAY_PAUSE -> this.media.togglePlayPause();
                  case NEXT -> this.media.skipNext();
                  case PREVIOUS -> this.media.skipPrevious();
               };
               if (accepted || this.media.lastError() == null) {
                  return;
               }

               SpotifyHud.LOGGER.debug("[Venti] Spotify HUD: media session control failed ({}); sending the media key", this.media.lastError());
            }

            if (Platform.isWindows()) {
               try {
                  SpotifyWindow.tap(key);
               } catch (RuntimeException | LinkageError var3) {
                  SpotifyHud.LOGGER.debug("[Venti] Spotify HUD: media key failed", var3);
               }
            }
         }
      }

      private void logError(String error) {
         if (error != null) {
            long now = System.currentTimeMillis();
            if (!Objects.equals(error, this.lastError) || now - this.lastErrorLoggedAt >= 60000L) {
               this.lastError = error;
               this.lastErrorLoggedAt = now;
               SpotifyHud.LOGGER.debug("[Venti] Spotify HUD: media session: {}", error);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum SkyEmojiStyle {
      BESIDE,
      BEHIND,
      REPLACE_WORD;
   }

   @Environment(EnvType.CLIENT)
   private record TrackLyrics(String title, String artist, LyricsQuery query) {
   }
}
