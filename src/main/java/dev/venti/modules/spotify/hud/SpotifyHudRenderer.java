package dev.venti.modules.spotify.hud;

import dev.venti.config.Settings;
import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.spotify.SpotifyHud;
import dev.venti.modules.spotify.spotify.AlbumArt;
import dev.venti.modules.spotify.spotify.DisplayText;
import dev.venti.modules.spotify.spotify.Lyrics;
import dev.venti.modules.spotify.spotify.NowPlaying;
import dev.venti.modules.spotify.spotify.PlaybackClock;
import dev.venti.modules.spotify.spotify.SpotifyWindow;
import dev.venti.modules.spotify.spotify.SyncedLyrics;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

/**
 * The Spotify card from the Anubis client HUD, cut out of its shared HudRenderer.
 * Everything below render() is the original card, lyrics and transport code.
 */
public final class SpotifyHudRenderer {
   private static final SpotifyHudRenderer INSTANCE = new SpotifyHudRenderer();
   private static final float EDGE_MARGIN = 12.0F;
   private final HudElement element = new HudElement();
   private final ScreenLyrics screenLyrics = new ScreenLyrics();
   private final Map<String, Int2ObjectOpenHashMap<StableWidth>> slots = new HashMap<>();
   private int slotCount;
   private float introTime;
   private boolean hudVisibleLastFrame;
   private boolean chatInput;
   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;

   private SpotifyHudRenderer() {
   }

   public static SpotifyHudRenderer getInstance() {
      return INSTANCE;
   }

   /** True when the card has anything to draw this frame, so ImGui can stay idle otherwise. */
   public boolean hasWork(boolean editMode) {
      Minecraft client = Minecraft.getInstance();
      if (editMode) {
         return true;
      } else if (client.level == null || client.options.hideGui) {
         if (this.spotifyArtTexture != null) {
            this.releaseSpotifyArt();
         }

         return false;
      } else {
         SpotifyHud module = SpotifyHud.active();
         return SpotifyHud.shouldShowHud() || this.hudVisibleLastFrame || this.screenLyrics.visible() || module != null && module.screenLyrics();
      }
   }

   public void render(boolean editMode) {
      Minecraft client = Minecraft.getInstance();
      SpotifyHud module = SpotifyHud.get();
      Settings settings = module.settings();
      this.chatInput = client.screen instanceof ChatScreen;
      this.spotifyButtonsShown = this.spotifyButtonsDrawn;
      this.spotifyButtonsDrawn = false;
      float dt = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
      float screenW = ImGui.getIO().getDisplaySizeX();
      float screenH = ImGui.getIO().getDisplaySizeY();
      boolean shown = SpotifyHud.shouldShowHud();
      if (client.level != null || editMode) {
         this.screenLyrics.render(ImGui.getBackgroundDrawList(), editMode, screenW, screenH);
      } else if (this.screenLyrics.visible()) {
         this.screenLyrics.release();
      }

      if (!shown && !editMode) {
         this.hudVisibleLastFrame = false;
         this.introTime = 0.0F;
         this.slots.clear();
         this.slotCount = 0;
         this.releaseSpotifyArt();
      } else {
         this.updateIntro(editMode, dt);
         if (this.slotCount > 256) {
            this.slots.clear();
            this.slotCount = 0;
         }

         HudElement state = this.element;
         state.scale = settings.hudScale;
         SpotifyHudRenderer.SpotifyPlan plan = this.planSpotify(state, editMode, dt);
         state.width = plan.width();
         state.height = plan.height();
         this.place(state, settings, screenW, screenH);
         if (editMode) {
            this.editorInput(state, settings, screenW, screenH);
         } else if (this.chatInput && this.spotifyButtonsShown && ImGui.isMouseClicked(0)) {
            int button = this.spotifyButtonAt(ImGui.getMousePosX(), ImGui.getMousePosY());
            if (button >= 0) {
               SpotifyHud.hudButton(SPOTIFY_BUTTONS[button]);
            }
         }

         ImGui.setNextWindowPos(0.0F, 0.0F, 1);
         ImGui.setNextWindowSize(screenW, screenH);
         ImGui.pushStyleVar(4, 0.0F);
         // NoDecoration | NoMove | NoSavedSettings | NoFocusOnAppearing | NoBringToFrontOnFocus | NoNav | NoBackground | NoInputs
         boolean open = ImGui.begin("##spotify_hud", 205215 | 197120 | 128);
         ImGui.popStyleVar();
         if (open) {
            ImDrawList dl = ImGui.getBackgroundDrawList();
            this.drawSpotify(dl, state, plan, module.shadow(), this.transform(state, editMode, shown));
            if (editMode) {
               int outline = this.dragging ? 0xFFFFFFFF : 0x80FFFFFF;
               dl.addRect(state.x - 2.0F, state.y - 2.0F, state.x + state.width + 2.0F, state.y + state.height + 2.0F, outline, 6.0F, 0, 1.0F);
            }
         }

         ImGui.end();
      }
   }

   /** Settings keep the card's near edge as a fraction of the screen, so it survives resizes and grows away from that edge. */
   private void place(HudElement state, Settings settings, float screenW, float screenH) {
      float margin = EDGE_MARGIN * ImGuiManager.getInstance().uiScale();
      if (Float.isNaN(settings.hudX)) {
         settings.hudRight = true;
         settings.hudX = (screenW - margin) / screenW;
      }

      float x = settings.hudRight ? settings.hudX * screenW - state.width : settings.hudX * screenW;
      state.x = Math.round(Math.max(0.0F, Math.min(screenW - state.width, x)));
      state.y = Math.round(yFor(SpotifyHud.get().verticalPosition(), state.height, screenH));
      state.anchorX = settings.hudRight ? 2 : 0;
   }

   /** 0% puts the card on the top edge, 100% on the bottom edge, and above 100% it slides below the screen. */
   private static float yFor(float percent, float height, float screenH) {
      float room = Math.max(0.0F, screenH - height);
      return percent <= 100.0F ? percent / 100.0F * room : room + (percent - 100.0F) / 100.0F * height;
   }

   private static float percentFor(float y, float height, float screenH) {
      float room = Math.max(1.0F, screenH - height);
      return y <= room ? y / room * 100.0F : 100.0F + (y - room) / Math.max(1.0F, height) * 100.0F;
   }

   private void editorInput(HudElement state, Settings settings, float screenW, float screenH) {
      float mouseX = ImGui.getMousePosX();
      float mouseY = ImGui.getMousePosY();
      boolean overCard = mouseX >= state.x && mouseX <= state.x + state.width && mouseY >= state.y && mouseY <= state.y + state.height;
      boolean overUi = ImGui.isWindowHovered(1 | 2 | 4 | 8 | 32);
      if (this.dragging) {
         if (ImGui.isMouseDown(0)) {
            float x = Math.max(0.0F, Math.min(screenW - state.width, mouseX - this.dragOffsetX));
            float y = Math.max(0.0F, mouseY - this.dragOffsetY);
            settings.hudRight = x + state.width * 0.5F > screenW * 0.5F;
            settings.hudX = (settings.hudRight ? x + state.width : x) / screenW;
            SpotifyHud.get().setVerticalPosition(percentFor(y, state.height, screenH));
         } else {
            this.dragging = false;
            settings.save();
         }
      } else if (overCard && !overUi) {
         float wheel = ImGui.getIO().getMouseWheel();
         if (wheel != 0.0F) {
            settings.hudScale = Math.max(0.5F, Math.min(3.0F, settings.hudScale + wheel * 0.05F));
            settings.save();
         }

         if (ImGui.isMouseClicked(0)) {
            this.dragging = true;
            this.dragOffsetX = mouseX - state.x;
            this.dragOffsetY = mouseY - state.y;
         }
      }
   }

   private void updateIntro(boolean editMode, float dt) {
      float end = 0.56F;
      if (editMode) {
         this.introTime = end;
         this.hudVisibleLastFrame = true;
      } else if (!this.hudVisibleLastFrame) {
         this.introTime = 0.0F;
         this.hudVisibleLastFrame = true;
      } else {
         this.introTime = Math.min(this.introTime + dt, end);
      }
   }

   private SpotifyHudRenderer.DrawTransform introTransform(HudElement state, boolean editMode) {
      if (editMode) {
         return SpotifyHudRenderer.DrawTransform.identity();
      } else {
         float t = clamp01((this.introTime - 0.12F) / 0.36F);
         float eased = easeOutCubic(t);
         float slide = 6.0F * scaleOf(state) * (1.0F - eased);

         return switch (state.anchorX) {
            case 1 -> new SpotifyHudRenderer.DrawTransform(0.0F, -slide, eased);
            case 2 -> new SpotifyHudRenderer.DrawTransform(slide, 0.0F, eased);
            default -> new SpotifyHudRenderer.DrawTransform(-slide, 0.0F, eased);
         };
      }
   }

   private float slot(String key, int px, float target, float dt) {
      Int2ObjectOpenHashMap<StableWidth> bySize = this.slots.computeIfAbsent(key, k -> new Int2ObjectOpenHashMap<>());
      StableWidth width = bySize.get(px);
      if (width == null) {
         width = new StableWidth();
         bySize.put(px, width);
         this.slotCount++;
      }

      return width.update(target, dt);
   }

   private void removeSlot(String key, int px) {
      Int2ObjectOpenHashMap<StableWidth> bySize = this.slots.get(key);
      if (bySize != null && bySize.remove(px) != null) {
         this.slotCount--;
      }
   }

   private static final SpotifyWindow.MediaKey[] SPOTIFY_BUTTONS = new SpotifyWindow.MediaKey[]{
      SpotifyWindow.MediaKey.PREVIOUS, SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT
   };
   private final float[] spotifyButtons = new float[12];
   private boolean spotifyButtonsShown;
   private boolean spotifyButtonsDrawn;

   private static final float SPOTIFY_PAD_Y = 8.0F;
   private static final float SPOTIFY_LINE_GAP = 2.0F;
   private static final float SPOTIFY_GLYPH = 10.0F;
   private static final float SPOTIFY_GLYPH_GAP = 10.0F;
   private static final float SPOTIFY_PAUSED = 0.62F;
   private static final float MARQUEE_SPEED = 30.0F;
   private static final float MARQUEE_HOLD = 2.4F;
   private static final float MARQUEE_GAP = 40.0F;
   private static final String ELLIPSIS = "...";
   private static final float CARD_PAD = 10.0F;
   private static final float CARD_MIN_WIDTH = 200.0F;
   private static final float CARD_DEFAULT_WIDTH = 320.0F;
   private static final float ART_GAP = 10.0F;
   private static final float ART_MIN_COLUMN = 128.0F;
   private static final float ART_RADIUS = 4.0F;
   private static final int ART_PLACEHOLDER = 318767103;
   private static final float LABEL_GAP = 3.0F;
   private static final float ARTIST_GAP = 1.0F;
   private static final float TRANSPORT_GAP = 7.0F;
   private static final float PROGRESS_GAP = 6.0F;
   private static final float PROGRESS_H = 2.0F;
   private static final float SKIP_GLYPH = 8.0F;
   private static final float PLAY_GLYPH = 10.0F;
   private static final float GLYPH_SPACING = 14.0F;
   private static final float GLYPH_MARGIN = 8.0F;
   private static final float LYRICS_GAP = 12.0F;
   private static final float LYRIC_ROW_GAP = 6.0F;
   private static final float LYRIC_LINE_GAP = 6.0F;
   private static final float LYRIC_SPRING = 13.0F;
   private static final float LYRIC_SETTLE_TIME = 1.1F;
   private static final float LYRIC_STAGGER = 0.045F;
   private static final int LYRIC_MAX_STAGGERED = 6;
   private static final int LYRIC_MAX_SHIFTS = 8;
   private static final float LYRIC_GLOW_TIME = 0.35F;
   private static final float LYRIC_ARRIVE_TIME = 0.3F;
   private static final float LYRIC_FADE_TIME = 0.32F;
   private static final float LYRIC_RISE = 8.0F;
   private static final int LYRIC_MAX_ROWS = 4;
   private static final float LYRIC_GROW_SPEED = 14.0F;
   private static final float LYRIC_SHRINK_SPEED = 9.0F;
   private static final float LYRIC_FAR_ALPHA = 0.6F;
   private static final float LYRIC_PAUSED_ALPHA = 0.7F;
   private static final float LYRIC_PAUSE_SPEED = 8.0F;
   private static final float LYRIC_BREATH_PERIOD = 1.6F;
   private static final long LYRIC_LEAD_MS = 200L;
   private static final long LYRIC_SWEEP_MS = 180L;
   private static final float LYRIC_SLOW_PACE = 2.0F;
   private static final float LYRIC_CHAR_MS = 90.0F;
   private static final float LYRIC_LIT_ALL = Float.POSITIVE_INFINITY;
   private static final long LYRIC_QUIET_MS = 700L;
   private static final float LYRIC_MIN_COVERAGE = 0.75F;
   private static final String[] NO_ROWS = new String[0];
   private static final NowPlaying SAMPLE_TRACK = new NowPlaying(
      NowPlaying.Status.PLAYING, "Golden Hour Drive", "Sample Artist", "", NowPlaying.Source.SESSION, 214000L, 84000L, 0L, 1.0, null
   );
   private static final Lyrics SAMPLE_LYRICS = new Lyrics(
      Lyrics.Status.SYNCED,
      SyncedLyrics.parse(
         "[01:21.00]Every light in the city knows my name\n[01:27.40]We keep on driving till the morning comes\n[01:33.80]Nothing but the radio and you\n"
      ),
      List.of(),
      0L
   );
   private float marqueeTime;
   private String marqueeTitle = "";
   private String marqueeArtist = "";
   private final PlaybackClock spotifyClock = new PlaybackClock();
   private NowPlaying spotifyFolded;
   private String spotifyTitle = "";
   private String spotifyArtist = "";
   private AlbumArt spotifyArt;
   private HudTexture spotifyArtTexture;
   private final Map<Long, Float> capTops = new HashMap<>();
   private Lyrics lyricPrepared;
   private int lyricGeneration = -1;
   private int foldedGeneration = -1;
   private String[] lyricTexts = NO_ROWS;
   private String[][] lyricRows = new String[0][];
   private int lyricRowsPx;
   private int lyricRowsWidth;
   private SpotifyHudRenderer.LyricWords[] lyricWords = new SpotifyHudRenderer.LyricWords[0];
   private float lyricCharMs = 90.0F;
   private boolean lyricReadable = true;
   private float lyricViewH = -1.0F;
   private int lyricViewLines;
   private float lyricViewScale;
   private SyncedLyrics lyricSource;
   private int lyricStart;
   private final List<SpotifyHudRenderer.LyricShift> lyricShifts = new ArrayList<>(8);
   private float lyricClock;
   private List<SpotifyHudRenderer.LyricItem> lyricFadeItems;
   private float lyricFade = 1.0F;
   private List<SpotifyHudRenderer.LyricItem> lyricLastItems = List.of();
   private List<SpotifyHudRenderer.LyricItem> lyricLeaveItems;
   private float lyricLeave;
   private float[] lyricShown = new float[0];
   private float lyricBreath;
   private float lyricPlaying = 1.0F;
   private int lyricCurrentFirst = -1;
   private int lyricCurrentLast = -1;
   private float[] lyricBright = new float[0];
   private int lyricBrightLo;
   private int lyricBrightHi = -1;
   private float lyricBlockH;
   private long lyricLoadingSince;
   private ImFont glyphFont;
   private final Map<Integer, Boolean> glyphs = new HashMap<>();
   private static final float SNAP_THRESHOLD = 6.0F;

   private SpotifyHudRenderer.DrawTransform transform(HudElement state, boolean editMode, boolean shownInGame) {
      SpotifyHudRenderer.DrawTransform tr = this.introTransform(state, editMode);
      return editMode && !shownInGame ? new SpotifyHudRenderer.DrawTransform(tr.xOffset(), tr.yOffset(), tr.alpha() * 0.35F) : tr;
   }

   private SpotifyHudRenderer.SpotifyPlan planSpotify(HudElement state, boolean editMode, float dt) {
      float s = scaleOf(state);
      SpotifyHudRenderer.Type t = SpotifyHudRenderer.Type.of(s);
      if (!editMode && !SpotifyHud.shouldShowHud()) {
         this.removeSlot("spotify", t.textPx());
         this.releaseSpotifyArt();
         this.lyricBlockH = 0.0F;
         return new SpotifyHudRenderer.SpotifyPlan(
            t,
            SpotifyHudRenderer.SpotifyLook.HIDDEN,
            SpotifyHudRenderer.SpotifyGlyph.NONE,
            "",
            "",
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            0.0F,
            false,
            null,
            SpotifyHudRenderer.SpotifyColors.DEFAULT,
            1.0F,
            (float)Math.round(28.0F * s)
         );
      } else {
         SpotifyHud module = SpotifyHud.get();
         SpotifyHud active = SpotifyHud.active();
         NowPlaying now = active != null ? active.nowPlaying() : NowPlaying.WAITING;
         boolean track = now.active() && now.hasTrack();
         boolean lyricsSeen = true;
         if (track && now.source() == NowPlaying.Source.SESSION) {
            return this.planSpotifyCard(module, now, false, lyricsSeen, t, s, editMode, dt);
         } else if (editMode && !track) {
            return this.planSpotifyCard(module, SAMPLE_TRACK, true, true, t, s, true, dt);
         } else {
            this.releaseSpotifyArt();
            this.lyricBlockH = 0.0F;
            return this.planSpotifyCompact(module, now, t, s, editMode, dt);
         }
      }
   }

   private SpotifyHudRenderer.SpotifyPlan planSpotifyCompact(SpotifyHud module, NowPlaying now, SpotifyHudRenderer.Type t, float s, boolean editMode, float dt) {
      boolean withArtist = module == null || module.showArtist();
      String artist = "";
      SpotifyHudRenderer.SpotifyGlyph glyph = SpotifyHudRenderer.SpotifyGlyph.NONE;
      boolean idle = false;
      String title;
      switch (now.status()) {
         case PLAYING:
         case PAUSED:
            glyph = now.playing() ? SpotifyHudRenderer.SpotifyGlyph.PLAYING : SpotifyHudRenderer.SpotifyGlyph.PAUSED;
            this.foldTrack(now, t.text());
            title = now.hasTrack() ? this.spotifyTitle : "Not playing";
            if (withArtist) {
               artist = this.spotifyArtist;
            }
            break;
         case NOT_RUNNING:
            title = "Spotify is not open";
            idle = true;
            break;
         case UNSUPPORTED:
            title = "Spotify · not supported here";
            idle = true;
            break;
         default:
            title = "Nothing playing";
            idle = true;
      }

      ImFont titleFont = idle ? t.label() : t.text();
      int titlePx = idle ? t.labelPx() : t.textPx();
      float titleW = HudChrome.width(titleFont, titlePx, title);
      float artistW = artist.isEmpty() ? 0.0F : HudChrome.width(t.label(), t.labelPx(), artist);
      float glyphW = glyph == SpotifyHudRenderer.SpotifyGlyph.NONE ? 0.0F : 20.0F * s;
      float fixed = 12.0F * s * 2.0F + glyphW;
      float maxText = Math.max(40.0F * s, (module != null ? module.cardWidth() : 320.0F) * s - fixed);
      float textW = Math.min(Math.max(titleW, artistW), maxText);
      float w = this.slot("spotify", t.textPx(), (float)Math.ceil(fixed + textW), dt);
      float height = artist.isEmpty() ? 28.0F * s : 8.0F * s * 2.0F + t.textPx() + 2.0F * s + t.labelPx();
      float avail = Math.max(1.0F, w - fixed);
      boolean scroll = module == null || module.scrollTitles();
      float loopGap = 40.0F * s;
      float[] offsets = this.marquee(title, artist, titleW, artistW, avail, scroll, now.playing() || editMode, loopGap, s, dt);
      return new SpotifyHudRenderer.SpotifyPlan(
         t,
         idle ? SpotifyHudRenderer.SpotifyLook.IDLE : SpotifyHudRenderer.SpotifyLook.COMPACT,
         glyph,
         title,
         artist,
         titleW,
         artistW,
         offsets[0],
         offsets[1],
         loopGap,
         scroll,
         null,
         SpotifyHudRenderer.SpotifyColors.of(module),
         (float)Math.round(w),
         (float)Math.round(height)
      );
   }

   private void drawSpotify(ImDrawList dl, HudElement state, SpotifyHudRenderer.SpotifyPlan plan, boolean shadow, SpotifyHudRenderer.DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F) && plan.look() != SpotifyHudRenderer.SpotifyLook.HIDDEN) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         SpotifyHud module = SpotifyHud.get();
         boolean guard = HudChrome.needsTextGuard(module.backgroundOpacity());
         int fill = GuiPalette.abgr(GuiPalette.withAlpha(module.backgroundColor(), module.backgroundOpacity()));
         HudChrome.card(dl, fill, x, y, w, h, module.cornerRadius() * s, a, shadow, s);
         if (plan.look() == SpotifyHudRenderer.SpotifyLook.CARD) {
            this.drawSpotifyCard(dl, plan, x, y, w, h, s, a, guard);
         } else {
            this.drawSpotifyCompact(dl, plan, x, y, w, h, s, a, guard);
         }
      }
   }

   private void drawSpotifyCompact(ImDrawList dl, SpotifyHudRenderer.SpotifyPlan plan, float x, float y, float w, float h, float s, float a, boolean guard) {
      SpotifyHudRenderer.Type t = plan.type();
      float cx = x + 12.0F * s;
      if (plan.glyph() != SpotifyHudRenderer.SpotifyGlyph.NONE) {
         this.drawPlayState(dl, cx, y + h * 0.5F, 10.0F * s, plan.glyph() == SpotifyHudRenderer.SpotifyGlyph.PLAYING, a);
         cx += 20.0F * s;
      }

      float avail = Math.max(1.0F, x + w - 12.0F * s - cx);
      if (plan.look() == SpotifyHudRenderer.SpotifyLook.IDLE) {
         float top = y + (float)Math.floor((h - t.labelPx()) * 0.5F);
         marqueeLine(
            dl,
            t.label(),
            t.labelPx(),
            cx,
            top,
            avail,
            plan.titleW(),
            0.0F,
            plan.loopGap(),
            false,
            plan.title(),
            HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
            guard
         );
      } else {
         float dim = plan.glyph() == SpotifyHudRenderer.SpotifyGlyph.PAUSED ? 0.62F : 1.0F;
         boolean twoLines = !plan.artist().isEmpty();
         float contentH = twoLines ? t.textPx() + 2.0F * s + t.labelPx() : t.textPx();
         float titleTop = y + (float)Math.floor((h - contentH) * 0.5F);
         marqueeLine(
            dl,
            t.text(),
            t.textPx(),
            cx,
            titleTop,
            avail,
            plan.titleW(),
            plan.titleOffset(),
            plan.loopGap(),
            plan.scroll(),
            plan.title(),
            HudChrome.scaleAlpha(plan.colors().title(), a * dim),
            guard
         );
         if (twoLines) {
            float artistTop = titleTop + t.textPx() + 2.0F * s;
            marqueeLine(
               dl,
               t.label(),
               t.labelPx(),
               cx,
               artistTop,
               avail,
               plan.artistW(),
               plan.artistOffset(),
               plan.loopGap(),
               plan.scroll(),
               plan.artist(),
               HudChrome.scaleAlpha(plan.colors().artist(), a),
               guard
            );
         }
      }
   }

   private void drawPlayState(ImDrawList dl, float left, float centerY, float size, boolean playing, float a) {
      float top = Math.round(centerY - size * 0.5F);
      float bottom = top + Math.round(size);
      if (playing) {
         dl.addTriangleFilled(left, top, left + size * 0.9F, (top + bottom) * 0.5F, left, bottom, HudChrome.scaleAlpha(this.hudAccent(), a));
      } else {
         float bar = Math.max(2.0F, (float)Math.round(size * 0.3F));
         float gap = Math.max(2.0F, (float)Math.round(size * 0.25F));
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a);
         float l = Math.round(left + (size - bar * 2.0F - gap) * 0.5F);
         dl.addRectFilled(l, top, l + bar, bottom, color, 1.0F);
         dl.addRectFilled(l + bar + gap, top, l + bar * 2.0F + gap, bottom, color, 1.0F);
      }
   }

   private SpotifyHudRenderer.SpotifyPlan planSpotifyCard(
      SpotifyHud module, NowPlaying track, boolean sample, boolean lyricsSeen, SpotifyHudRenderer.Type t, float s, boolean editMode, float dt
   ) {
      boolean withArtist = module == null || module.showArtist();
      boolean withArt = module == null || module.showAlbumArt();
      boolean withProgress = module == null || module.showProgress();
      boolean withLyrics = module == null || module.showLyrics();
      boolean scroll = module == null || module.scrollTitles();
      long nowMs = System.currentTimeMillis();
      this.foldTrack(track, t.text());
      String title = this.spotifyTitle;
      String artist = withArtist ? this.spotifyArtist : "";
      boolean timeline = track.hasTimeline();
      long position = track.positionMs();
      if (!sample) {
         this.spotifyClock.update(track, nowMs);
         position = this.spotifyClock.positionAt(nowMs);
      }

      float capTop = this.capTop(t.label(), t.labelPx());
      float transportTop = t.labelPx() + 3.0F * s + t.textPx() + (artist.isEmpty() ? 0.0F : 1.0F * s + t.labelPx()) + 7.0F * s;
      boolean progressBar = withProgress && timeline;
      float columnBottom = progressBar ? transportTop + t.labelPx() + 6.0F * s + progressHeight(s) : transportTop + HudChrome.ascent(t.label(), t.labelPx());
      float header = Math.round(columnBottom - capTop);
      float pad = 10.0F * s;
      float w = Math.round(Math.max(200.0F, module != null ? module.cardWidth() : 320.0F) * s);
      boolean art = withArt && w - pad * 2.0F - header - 10.0F * s >= 128.0F * s;
      float column = w - pad * 2.0F - (art ? header + 10.0F * s : 0.0F);
      String elapsed = "";
      String remaining = "";
      float progress = 0.0F;
      if (timeline) {
         long elapsedSeconds = position / 1000L;
         long totalSeconds = (track.durationMs() + 500L) / 1000L;
         elapsed = formatSeconds(elapsedSeconds);
         remaining = "−" + formatSeconds(Math.max(0L, totalSeconds - elapsedSeconds));
         progress = clamp01((float)position / (float)track.durationMs());
      }

      float titleW = HudChrome.width(t.text(), t.textPx(), title);
      float artistW = artist.isEmpty() ? 0.0F : HudChrome.width(t.label(), t.labelPx(), artist);
      float loopGap = 40.0F * s;
      float[] offsets = this.marquee(title, artist, titleW, artistW, column, scroll, track.playing() || editMode, loopGap, s, dt);
      boolean lyricsWanted = sample ? withLyrics && timeline : lyricsSeen && module != null && module.wantsLyrics(track);
      Lyrics lyrics = !lyricsWanted ? null : (sample ? SAMPLE_LYRICS : module.lyricsFor(track));
      boolean online = module == null || module.lyricsOnline();
      int lines = module == null ? 2 : module.lyricLines();
      SpotifyHud.LyricMotion motion = module == null ? SpotifyHud.LyricMotion.WAVE : module.lyricMotion();
      SpotifyHud.LyricHighlight highlight = module == null ? SpotifyHud.LyricHighlight.OFF : module.lyricHighlight();
      SpotifyHudRenderer.LyricView view = this.planLyrics(lyrics, lyricsWanted, online, position, lines, motion, highlight, t, w - pad * 2.0F, s, dt, nowMs);
      float playing = track.playing() ? 1.0F : 0.0F;
      this.lyricPlaying = this.lyricPlaying + (playing - this.lyricPlaying) * (1.0F - (float)Math.exp(-8.0F * dt));
      float lyricAlpha = 0.7F + 0.3F * this.lyricPlaying;
      SpotifyHudRenderer.SpotifyCard card = new SpotifyHudRenderer.SpotifyCard(
         track, sample, art, header, pad - capTop, column, timeline, elapsed, remaining, progress, progressBar, view, lyricAlpha
      );
      float height = pad * 2.0F + header + view.height();
      return new SpotifyHudRenderer.SpotifyPlan(
         t,
         SpotifyHudRenderer.SpotifyLook.CARD,
         SpotifyHudRenderer.SpotifyGlyph.NONE,
         title,
         artist,
         titleW,
         artistW,
         offsets[0],
         offsets[1],
         loopGap,
         scroll,
         card,
         SpotifyHudRenderer.SpotifyColors.of(module),
         w,
         (float)Math.round(height)
      );
   }

   private void drawSpotifyCard(ImDrawList dl, SpotifyHudRenderer.SpotifyPlan plan, float x, float y, float w, float h, float s, float a, boolean guard) {
      SpotifyHudRenderer.SpotifyCard card = plan.card();
      SpotifyHudRenderer.Type t = plan.type();
      float pad = 10.0F * s;
      boolean playing = card.track().playing();
      float left = x + pad;
      if (card.art()) {
         this.drawAlbumArt(dl, card, left, y + pad, card.header(), s, a);
         left += card.header() + 10.0F * s;
      } else {
         this.releaseSpotifyArt();
      }

      float column = card.column();
      float top = y + card.textTop();
      HudChrome.text(
         dl, t.label(), t.labelPx(), left, Math.round(top), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), playing ? "NOW PLAYING" : "PAUSED", guard
      );
      top += t.labelPx() + 3.0F * s;
      marqueeLine(
         dl,
         t.text(),
         t.textPx(),
         left,
         Math.round(top),
         column,
         plan.titleW(),
         plan.titleOffset(),
         plan.loopGap(),
         plan.scroll(),
         plan.title(),
         HudChrome.scaleAlpha(plan.colors().title(), a),
         guard
      );
      top += t.textPx();
      if (!plan.artist().isEmpty()) {
         top += 1.0F * s;
         marqueeLine(
            dl,
            t.label(),
            t.labelPx(),
            left,
            Math.round(top),
            column,
            plan.artistW(),
            plan.artistOffset(),
            plan.loopGap(),
            plan.scroll(),
            plan.artist(),
            HudChrome.scaleAlpha(plan.colors().artist(), a),
            guard
         );
         top += t.labelPx();
      }

      top = Math.round(top + 7.0F * s);
      this.drawTransport(dl, t, card, left, top, column, s, a, guard);
      if (card.progressBar()) {
         float barTop = Math.round(top + t.labelPx() + 6.0F * s);
         this.drawProgress(dl, left, barTop, column, progressHeight(s), card.progress(), playing, a);
      }

      SpotifyHudRenderer.LyricView lyrics = card.lyrics();
      if (lyrics.height() > 0.5F) {
         float blockTop = Math.round(y + pad + card.header() + 12.0F * s);
         this.drawLyrics(dl, t, lyrics, plan.colors(), x + pad, blockTop, w - pad * 2.0F, y + h - Math.max(1.0F, s), s, a * card.lyricAlpha(), guard);
      }
   }

   private void drawAlbumArt(ImDrawList dl, SpotifyHudRenderer.SpotifyCard card, float x, float y, float size, float s, float a) {
      float r = Math.min(SpotifyHud.get().cornerRadius() * 0.5F, 4.0F) * s;
      if (card.sample()) {
         this.releaseSpotifyArt();
      }

      HudTexture texture = card.sample() ? null : this.spotifyArtTexture(card.track().art());
      if (texture != null) {
         dl.addImageRounded(texture.id(), x, y, x + size, y + size, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, a), r, 240);
      } else {
         dl.addRectFilled(x, y, x + size, y + size, HudChrome.scaleAlpha(318767103, a), r);
         float bar = Math.max(2.0F, (float)Math.round(size * 0.07F));
         float gap = Math.max(2.0F, (float)Math.round(bar * 0.8F));
         float base = Math.round(y + size * 0.62F);
         float l = Math.round(x + (size - bar * 3.0F - gap * 2.0F) * 0.5F);
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a * 0.45F);
         float[] heights = new float[]{0.2F, 0.34F, 0.26F};

         for (int i = 0; i < heights.length; i++) {
            float bx = l + i * (bar + gap);
            dl.addRectFilled(bx, base - Math.round(size * heights[i]), bx + bar, base, color, 1.0F);
         }
      }

      dl.addRect(x, y, x + size, y + size, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
   }

   private void drawTransport(
      ImDrawList dl, SpotifyHudRenderer.Type t, SpotifyHudRenderer.SpotifyCard card, float left, float top, float column, float s, float a, boolean guard
   ) {
      float elapsedW = 0.0F;
      float remainingW = 0.0F;
      if (card.timeline()) {
         elapsedW = HudChrome.tabularWidth(t.label(), t.labelPx(), card.elapsed());
         remainingW = HudChrome.tabularWidth(t.label(), t.labelPx(), card.remaining());
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a);
         HudChrome.tabular(dl, t.label(), t.labelPx(), left, top, color, card.elapsed(), guard);
         HudChrome.tabular(dl, t.label(), t.labelPx(), Math.round(left + column - remainingW), top, color, card.remaining(), guard);
      }

      float skip = Math.max(4.0F, (float)Math.round(8.0F * s));
      float play = Math.max(5.0F, (float)Math.round(10.0F * s));
      float spacing = Math.round(14.0F * s);
      float cluster = skip * 2.0F + play + spacing * 2.0F;
      float centre = left + column * 0.5F;
      float margin = 8.0F * s;
      if (!(centre - cluster * 0.5F < left + elapsedW + margin) && !(centre + cluster * 0.5F > left + column - remainingW - margin)) {
         float centerY = top + (this.capTop(t.label(), t.labelPx()) + HudChrome.ascent(t.label(), t.labelPx())) * 0.5F;
         float playLeft = Math.round(centre - play * 0.5F);
         float[] lefts = new float[]{playLeft - spacing - skip, playLeft, playLeft + play + spacing};
         float[] sizes = new float[]{skip, play, skip};
         float reach = Math.round(3.0F * s);
         float mouseX = ImGui.getMousePosX();
         float mouseY = ImGui.getMousePosY();

         for (int i = 0; i < 3; i++) {
            float l = lefts[i] - reach;
            float r = lefts[i] + sizes[i] + reach;
            float tp = Math.round(centerY - sizes[i] * 0.5F) - reach;
            float b = Math.round(centerY + sizes[i] * 0.5F) + reach;
            this.spotifyButtons[i * 4] = l;
            this.spotifyButtons[i * 4 + 1] = tp;
            this.spotifyButtons[i * 4 + 2] = r;
            this.spotifyButtons[i * 4 + 3] = b;
            boolean hovered = this.chatInput && mouseX >= l && mouseX <= r && mouseY >= tp && mouseY <= b;
            if (hovered) {
               dl.addRectFilled(l, tp, r, b, HudChrome.scaleAlpha(654311423, a), (r - l) * 0.5F);
            }

            int color = HudChrome.scaleAlpha(!hovered && i != 1 ? HudChrome.TEXT_LABEL : HudChrome.TEXT_NAME, a);
            if (i == 1) {
               drawPlayPauseGlyph(dl, lefts[i], centerY, play, card.track().playing(), color);
            } else {
               drawSkipGlyph(dl, lefts[i], centerY, skip, i == 2, color);
            }
         }

         this.spotifyButtonsDrawn = true;
      }
   }

   private int spotifyButtonAt(float x, float y) {
      for (int i = 0; i < SPOTIFY_BUTTONS.length; i++) {
         if (x >= this.spotifyButtons[i * 4]
            && x <= this.spotifyButtons[i * 4 + 2]
            && y >= this.spotifyButtons[i * 4 + 1]
            && y <= this.spotifyButtons[i * 4 + 3]) {
            return i;
         }
      }

      return -1;
   }

   private static void drawSkipGlyph(ImDrawList dl, float left, float centerY, float size, boolean next, int color) {
      float top = Math.round(centerY - size * 0.5F);
      float bottom = top + size;
      float mid = (top + bottom) * 0.5F;
      float bar = Math.max(1.0F, (float)Math.round(size * 0.2F));
      if (next) {
         dl.addTriangleFilled(left, top, left + size - bar, mid, left, bottom, color);
         dl.addRectFilled(left + size - bar, top, left + size, bottom, color);
      } else {
         dl.addRectFilled(left, top, left + bar, bottom, color);
         dl.addTriangleFilled(left + size, top, left + size, bottom, left + bar, mid, color);
      }
   }

   private static void drawPlayPauseGlyph(ImDrawList dl, float left, float centerY, float size, boolean playing, int color) {
      float top = Math.round(centerY - size * 0.5F);
      float bottom = top + size;
      if (playing) {
         float bar = Math.max(2.0F, (float)Math.round(size * 0.3F));
         float gap = Math.max(2.0F, (float)Math.round(size * 0.24F));
         float l = Math.round(left + (size - bar * 2.0F - gap) * 0.5F);
         dl.addRectFilled(l, top, l + bar, bottom, color, 1.0F);
         dl.addRectFilled(l + bar + gap, top, l + bar * 2.0F + gap, bottom, color, 1.0F);
      } else {
         float inset = Math.round(size * 0.08F);
         dl.addTriangleFilled(left + inset, top, left + inset + size * 0.86F, (top + bottom) * 0.5F, left + inset, bottom, color);
      }
   }

   private void drawProgress(ImDrawList dl, float left, float top, float width, float height, float progress, boolean playing, float a) {
      float r = height * 0.5F;
      dl.addRectFilled(left, top, left + width, top + height, HudChrome.scaleAlpha(452984831, a), r);
      float fill = Math.round(width * clamp01(progress));
      if (!(fill < 1.0F)) {
         dl.addRectFilled(left, top, left + fill, top + height, HudChrome.scaleAlpha(playing ? this.hudAccent() : HudChrome.TEXT_LABEL, a), r);
      }
   }

   private static float progressHeight(float s) {
      return Math.max(1.0F, (float)Math.round(2.0F * s));
   }

   private SpotifyHudRenderer.LyricView planLyrics(
      Lyrics lyrics,
      boolean wanted,
      boolean online,
      long position,
      int lines,
      SpotifyHud.LyricMotion motion,
      SpotifyHud.LyricHighlight highlight,
      SpotifyHudRenderer.Type t,
      float innerW,
      float s,
      float dt,
      long nowMs
   ) {
      List<SpotifyHudRenderer.LyricItem> items = List.of();
      String status = null;
      float content = -1.0F;
      boolean live = false;
      if (!wanted) {
         this.lyricLoadingSince = 0L;
         content = 0.0F;
      } else {
         if (lyrics == null || lyrics.status() != Lyrics.Status.LOADING) {
            this.lyricLoadingSince = 0L;
         } else if (this.lyricLoadingSince == 0L) {
            this.lyricLoadingSince = nowMs;
         }

         if (lyrics != null) {
            switch (lyrics.status()) {
               case LOADING:
                  status = nowMs - this.lyricLoadingSince < 700L ? null : "Loading lyrics";
                  break;
               case SYNCED:
                  this.prepareLyrics(lyrics, t.text());
                  if (this.lyricReadable) {
                     items = new ArrayList<>(16);
                     float viewH = this.lyricViewHeight(lyrics.synced(), lines, t, innerW, s);
                     this.syncedItems(items, lyrics.synced(), position, viewH, motion, highlight, t, innerW, s, dt);
                     content = viewH - (t.textPx() - HudChrome.ascent(t.text(), t.textPx()));
                     live = true;
                  } else {
                     status = "Lyrics in a script the HUD font lacks";
                  }
                  break;
               case PLAIN:
                  status = "No synced lyrics";
                  break;
               case INSTRUMENTAL:
                  status = "Instrumental";
                  break;
               case NOT_FOUND:
                  status = online ? "No lyrics found" : "No saved lyrics";
                  break;
               case UNAVAILABLE:
                  status = "Lyrics unavailable";
            }
         }
      }

      if (status != null) {
         content = Math.round((t.textPx() - t.labelPx()) * 0.5F) + HudChrome.ascent(t.label(), t.labelPx());
      }

      if (live) {
         this.lyricLastItems = items;
         this.lyricLeaveItems = null;
      } else {
         items = this.leavingItems(status == null, t, s, dt);
      }

      float target = content < 0.0F ? this.lyricBlockH : (content > 0.0F ? 12.0F * s + Math.round(content) : 0.0F);
      return new SpotifyHudRenderer.LyricView(items, status, this.easeLyricBlock(target, dt));
   }

   private List<SpotifyHudRenderer.LyricItem> leavingItems(boolean quiet, SpotifyHudRenderer.Type t, float s, float dt) {
      this.lyricSource = null;
      if (!this.lyricLastItems.isEmpty()) {
         this.lyricLeaveItems = this.lyricLastItems;
         this.lyricLeave = 0.0F;
         this.lyricLastItems = List.of();
      }

      if (this.lyricLeaveItems == null) {
         return List.of();
      } else {
         this.lyricLeave = Math.min(1.0F, this.lyricLeave + dt / 0.32F);
         if (quiet && !(this.lyricLeave >= 1.0F)) {
            List<SpotifyHudRenderer.LyricItem> items = new ArrayList<>(this.lyricLeaveItems.size());
            addFaded(items, this.lyricLeaveItems, smooth(this.lyricLeave), 8.0F * s);
            return items;
         } else {
            this.lyricLeaveItems = null;
            return List.of();
         }
      }
   }

   private static void addFaded(List<SpotifyHudRenderer.LyricItem> items, List<SpotifyHudRenderer.LyricItem> frozen, float fade, float rise) {
      for (SpotifyHudRenderer.LyricItem item : frozen) {
         float alpha = item.alpha() * (1.0F - fade);
         if (!(alpha <= 0.004F)) {
            items.add(new SpotifyHudRenderer.LyricItem(item.text(), item.top() - rise * fade, item.highlight(), alpha, item.progress(), item.fill()));
         }
      }
   }

   private void syncedItems(
      List<SpotifyHudRenderer.LyricItem> items,
      SyncedLyrics synced,
      long position,
      float viewH,
      SpotifyHud.LyricMotion motion,
      SpotifyHud.LyricHighlight highlight,
      SpotifyHudRenderer.Type t,
      float innerW,
      float s,
      float dt
   ) {
      float lineGap = 6.0F * s;
      int currentLast = synced.indexAt(position + 200L);
      int currentFirst = synced.firstWithTimeOf(currentLast);
      int start = Math.max(0, currentFirst);
      if (synced != this.lyricSource) {
         this.lyricSource = synced;
         this.lyricStart = start;
         this.lyricShifts.clear();
         this.lyricClock = 0.0F;
         this.lyricFadeItems = null;
         this.lyricFade = 1.0F;
         this.lyricShown = new float[synced.size() * 2];
         this.lyricBright = new float[synced.size()];
         this.lyricBrightLo = 0;
         this.lyricBrightHi = -1;

         for (int i = Math.max(0, currentFirst); i <= currentLast; i++) {
            this.lyricBright[i] = 1.0F;
         }

         if (currentLast >= 0) {
            this.lyricBrightLo = Math.max(0, currentFirst);
            this.lyricBrightHi = currentLast;
         }
      }

      this.lyricClock += dt;
      this.lyricBreath = (this.lyricBreath + dt) % 1.6F;
      this.lyricCurrentFirst = currentFirst;
      this.lyricCurrentLast = currentLast;
      float stagger = motion == SpotifyHud.LyricMotion.WAVE ? 0.045F : 0.0F;
      if (start != this.lyricStart) {
         float distance = start > this.lyricStart ? this.restDistance(this.lyricStart, start, t, innerW, s) : -1.0F;
         if (motion != SpotifyHud.LyricMotion.FADE && distance > 0.0F && distance <= viewH + lineGap + 0.5F) {
            if (this.lyricShifts.size() == 8) {
               this.lyricShifts.remove(0);
            }

            this.lyricShifts.add(new SpotifyHudRenderer.LyricShift(distance, this.lyricClock, start, stagger));
         } else {
            this.lyricFadeItems = this.lyricLastItems;
            this.lyricFade = 0.0F;
            this.lyricShifts.clear();
            this.showView(synced, start, viewH, t, innerW, s);
         }

         this.lyricStart = start;
      }

      this.updateBrightness(currentFirst, currentLast, dt);
      this.lyricFade = Math.min(1.0F, this.lyricFade + dt / 0.32F);
      if (this.lyricFade >= 1.0F) {
         this.lyricFadeItems = null;
      }

      float settled = 1.37F;
      this.lyricShifts.removeIf(shift -> this.lyricClock - shift.at() > settled);
      if (this.lyricShifts.isEmpty()) {
         this.lyricClock = 0.0F;
      }

      float rise = 8.0F * s;
      if (this.lyricFadeItems != null) {
         float fade = smooth(this.lyricFade);
         addFaded(items, this.lyricFadeItems, fade, rise);
         this.addLyricView(items, synced, this.lyricStart, fade, rise * (1.0F - fade), viewH, position, highlight, t, innerW, s, dt);
      } else {
         this.addLyricView(items, synced, this.lyricStart, 1.0F, 0.0F, viewH, position, highlight, t, innerW, s, dt);
      }
   }

   private void addLyricView(
      List<SpotifyHudRenderer.LyricItem> items,
      SyncedLyrics synced,
      int from,
      float alpha,
      float rise,
      float viewH,
      long position,
      SpotifyHud.LyricHighlight mode,
      SpotifyHudRenderer.Type t,
      float innerW,
      float s,
      float dt
   ) {
      ImFont font = t.text();
      int px = t.textPx();
      float rowGap = 6.0F * s;
      float lineGap = 6.0F * s;
      float slot = px + lineGap;
      boolean karaoke = mode == SpotifyHud.LyricHighlight.KARAOKE;
      float restTop = 0.0F;

      for (int i = from; i < synced.size() && restTop <= viewH + slot * 2.0F; i++) {
         String[] rows = this.rowsFor(i, font, px, innerW);
         int count = Math.max(1, rows.length);
         float lineTop = restTop + this.lyricOffset(i) + rise;
         float depth = 1.0F - 0.39999998F * clamp01(lineTop / slot - 1.0F);
         float highlight = this.lyricGlow(i, mode);
         float progress = rows.length == 0 ? this.breakProgress(synced, i, position) : 1.0F;

         for (int r = 0; r < count; r++) {
            float rowRest = restTop + r * (px + rowGap);
            float shown = this.updateShown(i, r, rowRest + px <= viewH + 0.5F, dt);
            float top = lineTop + r * (px + rowGap);
            float rowAlpha = alpha * depth * smooth(shown) * topFade(top, s) * bottomFade(top + px - viewH, px);
            if (!(rowAlpha <= 0.004F)) {
               float fill = karaoke && rows.length > 0 ? this.rowFill(synced, i, rows, r, font, px, position) : 0.0F;
               items.add(new SpotifyHudRenderer.LyricItem(rows.length == 0 ? null : rows[r], top, highlight, rowAlpha, progress, fill));
            }
         }

         restTop += lineHeight(rows, px, rowGap) + lineGap;
      }

      float lineRest = 0.0F;

      for (int i = from - 1; i >= 0; i--) {
         String[] rows = this.rowsFor(i, font, px, innerW);
         int count = Math.max(1, rows.length);
         lineRest -= lineHeight(rows, px, rowGap) + lineGap;
         float lineTop = lineRest + this.lyricOffset(i) + rise;
         if (topFade(lineTop + (count - 1) * (px + rowGap), s) <= 0.0F) {
            break;
         }

         float highlight = this.lyricGlow(i, mode);
         float progress = rows.length == 0 ? this.breakProgress(synced, i, position) : 1.0F;

         for (int rx = 0; rx < count; rx++) {
            float top = lineTop + rx * (px + rowGap);
            float rowAlpha = alpha * smooth(this.lyricShown[i * 2 + rx]) * topFade(top, s);
            if (!(rowAlpha <= 0.004F)) {
               float fill = karaoke && rows.length > 0 ? this.rowFill(synced, i, rows, rx, font, px, position) : 0.0F;
               items.add(new SpotifyHudRenderer.LyricItem(rows.length == 0 ? null : rows[rx], top, highlight, rowAlpha, progress, fill));
            }
         }
      }
   }

   private float lyricOffset(int index) {
      float offset = 0.0F;

      for (int k = 0; k < this.lyricShifts.size(); k++) {
         SpotifyHudRenderer.LyricShift shift = this.lyricShifts.get(k);
         float delay = Math.max(0, Math.min(6, index - shift.start())) * shift.stagger();
         float time = this.lyricClock - shift.at() - delay;
         offset += time <= 0.0F ? shift.distance() : shift.distance() * springLeft(time);
      }

      return offset;
   }

   private static float springLeft(float time) {
      float wt = 13.0F * time;
      return (1.0F + wt) * (float)Math.exp(-wt);
   }

   private static float topFade(float top, float s) {
      return top >= 0.0F ? 1.0F : smooth(1.0F + top / (12.0F * s * 0.75F));
   }

   private static float bottomFade(float below, int px) {
      return below <= 0.5F ? 1.0F : smooth(1.0F - below / (px * 0.6F));
   }

   private float updateShown(int index, int row, boolean fits, float dt) {
      int k = index * 2 + row;
      float step = dt / 0.3F;
      float shown = fits ? Math.min(1.0F, this.lyricShown[k] + step) : Math.max(0.0F, this.lyricShown[k] - step);
      this.lyricShown[k] = shown;
      return shown;
   }

   private void showView(SyncedLyrics synced, int from, float viewH, SpotifyHudRenderer.Type t, float innerW, float s) {
      Arrays.fill(this.lyricShown, 0.0F);
      int px = t.textPx();
      float rowGap = 6.0F * s;
      float restTop = 0.0F;

      for (int i = from; i < synced.size() && restTop < viewH; i++) {
         String[] rows = this.rowsFor(i, t.text(), px, innerW);
         int r = 0;

         for (int count = Math.max(1, rows.length); r < count; r++) {
            if (restTop + r * (px + rowGap) + px <= viewH + 0.5F) {
               this.lyricShown[i * 2 + r] = 1.0F;
            }
         }

         restTop += lineHeight(rows, px, rowGap) + 6.0F * s;
      }
   }

   private float restDistance(int from, int to, SpotifyHudRenderer.Type t, float innerW, float s) {
      float rowGap = 6.0F * s;
      float distance = 0.0F;

      for (int i = from; i < to; i++) {
         distance += lineHeight(this.rowsFor(i, t.text(), t.textPx(), innerW), t.textPx(), rowGap) + 6.0F * s;
      }

      return distance;
   }

   private void updateBrightness(int currentFirst, int currentLast, float dt) {
      int lo = this.lyricBrightHi >= 0 ? this.lyricBrightLo : Integer.MAX_VALUE;
      int hi = this.lyricBrightHi;
      if (currentLast >= 0) {
         lo = Math.min(lo, Math.max(0, currentFirst));
         hi = Math.max(hi, currentLast);
      }

      float step = dt / 0.35F;
      int lit = Integer.MAX_VALUE;
      int last = -1;

      for (int i = Math.max(0, lo); i <= hi && i < this.lyricBright.length; i++) {
         boolean sung = i >= currentFirst && i <= currentLast;
         float bright = sung ? Math.min(1.0F, this.lyricBright[i] + step) : Math.max(0.0F, this.lyricBright[i] - step);
         this.lyricBright[i] = bright;
         if (bright > 0.0F) {
            lit = Math.min(lit, i);
            last = i;
         }
      }

      this.lyricBrightLo = last < 0 ? 0 : lit;
      this.lyricBrightHi = last;
   }

   private float lyricGlow(int index, SpotifyHud.LyricHighlight mode) {
      if (mode != SpotifyHud.LyricHighlight.OFF && index >= 0 && index < this.lyricCurrentFirst) {
         return 1.0F;
      } else {
         return index >= 0 && index < this.lyricBright.length ? smooth(this.lyricBright[index]) : 0.0F;
      }
   }

   private float breakProgress(SyncedLyrics synced, int index, long position) {
      if (index >= this.lyricCurrentFirst && index <= this.lyricCurrentLast) {
         int next = synced.lastWithTimeOf(index) + 1;
         if (next >= synced.size()) {
            return 1.0F;
         } else {
            long begin = synced.line(index).timeMs();
            long end = synced.line(next).timeMs() - 200L;
            return end <= begin ? 1.0F : clamp01((float)(position - begin) / (float)(end - begin));
         }
      } else {
         return 1.0F;
      }
   }

   private float rowFill(SyncedLyrics synced, int index, String[] rows, int row, ImFont font, int px, long position) {
      if (index < this.lyricCurrentFirst) {
         return Float.POSITIVE_INFINITY;
      } else if (index > this.lyricCurrentLast) {
         return 0.0F;
      } else {
         String text = rows[row];
         int rowStart = 0;
         if (row > 0) {
            String line = this.lyricTexts[index];
            rowStart = rows[0].length();

            while (rowStart < line.length() && line.charAt(rowStart) == ' ') {
               rowStart++;
            }
         }

         float lit = this.litChars(synced, index, position) - rowStart;
         if (lit <= 0.0F) {
            return 0.0F;
         } else if (lit >= text.length()) {
            return Float.POSITIVE_INFINITY;
         } else {
            int whole = (int)lit;
            if (whole > 0 && Character.isLowSurrogate(text.charAt(whole))) {
               whole--;
            }

            int next = whole + Character.charCount(text.codePointAt(whole));
            float x0 = HudChrome.width(font, px, text.substring(0, whole));
            float x1 = HudChrome.width(font, px, text.substring(0, next));
            return x0 + (x1 - x0) * clamp01((lit - whole) / (next - whole));
         }
      }
   }

   private float litChars(SyncedLyrics synced, int index, long position) {
      SpotifyHudRenderer.LyricWords words = this.lyricWords[index];
      if (words == null) {
         this.timeWords(synced, synced.firstWithTimeOf(index));
         words = this.lyricWords[index];
      }

      float lit = 0.0F;
      long[] start = words.start();

      for (int k = 0; k < start.length && position >= start[k]; k++) {
         float swept = smooth((float)(position - start[k]) / (float)words.sweep()[k]);
         lit = words.from()[k] + (words.to()[k] - words.from()[k]) * swept;
      }

      return lit;
   }

   private void timeWords(SyncedLyrics synced, int first) {
      int last = synced.lastWithTimeOf(first);
      int next = last + 1;
      long begin = synced.line(first).timeMs();
      long handover = next < synced.size() ? synced.line(next).timeMs() - 200L : Long.MAX_VALUE;
      int letters = 0;

      for (int i = first; i <= last; i++) {
         letters += letterCount(this.lyricTexts[i]);
      }

      long window = next < synced.size()
         ? Math.min(handover - begin, (long)Math.round(letters * this.lyricCharMs * 2.0F))
         : Math.round(letters * this.lyricCharMs);
      window = Math.max(0L, window);
      int done = 0;

      for (int i = first; i <= last; i++) {
         String text = this.lyricTexts[i];
         int count = text.isEmpty() ? 0 : text.length() - letterCount(text) + 1;
         int[] from = new int[count];
         int[] to = new int[count];
         long[] start = new long[count];
         long[] sweep = new long[count];
         List<Long> timed = synced.line(i).wordTimes();
         boolean own = timed.size() == count;
         int k = 0;

         for (int c = 0; k < count; k++) {
            from[k] = c;
            c = text.indexOf(32, c);
            if (c < 0) {
               c = text.length();
            }

            to[k] = c++;
            long at = own ? timed.get(k) : begin + window * done / letters;
            done += to[k] - from[k];
            long until = own ? (k + 1 < count ? timed.get(k + 1) : Math.min(handover, at + 180L)) : begin + window * done / letters;
            start[k] = at;
            sweep[k] = Math.max(1L, Math.min(180L, until - at));
         }

         this.lyricWords[i] = new SpotifyHudRenderer.LyricWords(from, to, start, sweep);
      }
   }

   private float lyricPace(SyncedLyrics synced) {
      float[] paces = new float[synced.size()];
      int n = 0;
      int first = 0;

      while (first < synced.size()) {
         int last = synced.lastWithTimeOf(first);
         int letters = 0;

         for (int i = first; i <= last; i++) {
            letters += letterCount(this.lyricTexts[i]);
         }

         if (letters > 0 && last + 1 < synced.size()) {
            paces[n++] = (float)(synced.line(last + 1).timeMs() - synced.line(first).timeMs()) / letters;
         }

         first = last + 1;
      }

      if (n == 0) {
         return 90.0F;
      } else {
         Arrays.sort(paces, 0, n);
         return paces[n / 2];
      }
   }

   private static int letterCount(String text) {
      int letters = text.length();

      for (int i = 0; i < text.length(); i++) {
         if (text.charAt(i) == ' ') {
            letters--;
         }
      }

      return letters;
   }

   private float lyricViewHeight(SyncedLyrics synced, int lines, SpotifyHudRenderer.Type t, float innerW, float s) {
      int px = t.textPx();
      this.prepareRows(px, innerW);
      if (this.lyricViewH >= 0.0F && this.lyricViewLines == lines && this.lyricViewScale == s) {
         return this.lyricViewH;
      } else {
         int slots = lines;
         int start = 0;

         while (start < synced.size()) {
            int rows = 0;
            int i = start;

            for (int last = synced.lastWithTimeOf(start); i <= last; i++) {
               rows += Math.max(1, this.rowsFor(i, t.text(), px, innerW).length);
            }

            slots = Math.max(slots, Math.min(4, rows));
            start = synced.lastWithTimeOf(start) + 1;
         }

         this.lyricViewH = slots * px + (slots - 1) * 6.0F * s;
         this.lyricViewLines = lines;
         this.lyricViewScale = s;
         return this.lyricViewH;
      }
   }

   private static float lineHeight(String[] rows, int px, float rowGap) {
      return rows.length <= 1 ? px : rows.length * px + (rows.length - 1) * rowGap;
   }

   private float easeLyricBlock(float target, float dt) {
      float diff = target - this.lyricBlockH;
      if (Math.abs(diff) < 0.5F) {
         this.lyricBlockH = target;
      } else {
         float speed = diff > 0.0F ? 14.0F : 9.0F;
         this.lyricBlockH = this.lyricBlockH + diff * (1.0F - (float)Math.exp(-speed * dt));
      }

      return this.lyricBlockH;
   }

   private void prepareLyrics(Lyrics lyrics, ImFont font) {
      ImGuiManager fonts = ImGuiManager.getInstance();
      if (lyrics != this.lyricPrepared || fonts.fontGeneration() != this.lyricGeneration) {
         if (lyrics != this.lyricPrepared) {
            for (int i = 0; i < lyrics.synced().size(); i++) {
               fonts.requestGlyphs(lyrics.synced().line(i).text());
            }
         }

         this.lyricPrepared = lyrics;
         this.lyricGeneration = fonts.fontGeneration();
         this.lyricRowsPx = -1;
         this.lyricViewH = -1.0F;
         SyncedLyrics synced = lyrics.synced();
         this.lyricTexts = new String[synced.size()];
         this.lyricRows = new String[synced.size()][];
         this.lyricWords = new SpotifyHudRenderer.LyricWords[synced.size()];
         int letters = 0;
         int missing = 0;
         IntPredicate drawable = cpx -> this.hudHasGlyph(font, cpx);

         for (int i = 0; i < synced.size(); i++) {
            String text = DisplayText.fold(synced.line(i).text(), drawable);
            this.lyricTexts[i] = text;
            int k = 0;

            while (k < text.length()) {
               int cp = text.codePointAt(k);
               k += Character.charCount(cp);
               if (Character.isLetter(cp)) {
                  letters++;
                  if (cp >= 128 && !this.hudHasGlyph(font, cp)) {
                     missing++;
                  }
               }
            }
         }

         this.lyricReadable = missing <= letters * 0.25F;
         this.lyricCharMs = this.lyricPace(synced);
      }
   }

   private boolean hudHasGlyph(ImFont font, int cp) {
      if (cp < 128 || font == null) {
         return true;
      } else if (cp > 65535) {
         return false;
      } else {
         if (font != this.glyphFont) {
            this.glyphFont = font;
            this.glyphs.clear();
         }

         Boolean known = this.glyphs.get(cp);
         if (known == null) {
            if (this.glyphs.size() > 1024) {
               this.glyphs.clear();
            }

            known = font.findGlyphNoFallback(cp).isValidPtr();
            this.glyphs.put(cp, known);
         }

         return known;
      }
   }

   private void prepareRows(int px, float maxW) {
      int widthKey = (int)maxW;
      if (px != this.lyricRowsPx || widthKey != this.lyricRowsWidth) {
         Arrays.fill(this.lyricRows, null);
         this.lyricRowsPx = px;
         this.lyricRowsWidth = widthKey;
         this.lyricViewH = -1.0F;
      }
   }

   private String[] rowsFor(int index, ImFont font, int px, float maxW) {
      this.prepareRows(px, maxW);
      String[] rows = this.lyricRows[index];
      if (rows == null) {
         rows = wrapLyric(font, px, this.lyricTexts[index], this.lyricRowsWidth);
         this.lyricRows[index] = rows;
      }

      return rows;
   }

   private static String[] wrapLyric(ImFont font, int px, String text, float maxW) {
      if (text.isEmpty()) {
         return NO_ROWS;
      } else if (HudChrome.width(font, px, text) <= maxW) {
         return new String[]{text};
      } else {
         int best = -1;
         float bestW = Float.MAX_VALUE;

         for (int i = text.indexOf(32); i > 0; i = text.indexOf(32, i + 1)) {
            float firstW = HudChrome.width(font, px, text.substring(0, i).stripTrailing());
            if (firstW > maxW) {
               break;
            }

            float secondW = HudChrome.width(font, px, text.substring(i + 1).stripLeading());
            if (secondW <= maxW && Math.max(firstW, secondW) < bestW) {
               bestW = Math.max(firstW, secondW);
               best = i;
            }
         }

         if (best > 0) {
            return new String[]{text.substring(0, best).stripTrailing(), text.substring(best + 1).stripLeading()};
         } else {
            int cut = fitPrefix(font, px, text, maxW);
            String first = text.substring(0, cut).stripTrailing();
            String rest = text.substring(cut).stripLeading();
            return rest.isEmpty() ? new String[]{first} : new String[]{first, HudChrome.width(font, px, rest) <= maxW ? rest : ellipsize(font, px, rest, maxW)};
         }
      }
   }

   private static int fitPrefix(ImFont font, int px, String text, float maxW) {
      int lastBreak = -1;

      for (int i = text.indexOf(32); i > 0 && !(HudChrome.width(font, px, text.substring(0, i)) > maxW); i = text.indexOf(32, i + 1)) {
         lastBreak = i;
      }

      if (lastBreak > 0) {
         return lastBreak + 1;
      } else {
         int lo = 1;
         int hi = text.length();

         while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (HudChrome.width(font, px, text.substring(0, mid)) <= maxW) {
               lo = mid;
            } else {
               hi = mid - 1;
            }
         }

         if (lo > 1 && lo < text.length() && Character.isHighSurrogate(text.charAt(lo - 1))) {
            lo--;
         }

         return lo;
      }
   }

   private void drawLyrics(
      ImDrawList dl,
      SpotifyHudRenderer.Type t,
      SpotifyHudRenderer.LyricView view,
      SpotifyHudRenderer.SpotifyColors colors,
      float left,
      float top,
      float width,
      float bottom,
      float s,
      float a,
      boolean guard
   ) {
      float clipTop = top - 12.0F * s + Math.max(1.0F, 2.0F * s);
      if (!(bottom <= clipTop) && !(a <= 0.001F)) {
         float clipLeft = (float)Math.floor(left - 2.0F);
         float clipRight = (float)Math.ceil(left + width + 2.0F);
         dl.pushClipRect(clipLeft, clipTop, clipRight, bottom, true);
         if (view.status() != null) {
            float statusTop = Math.round(top + (t.textPx() - t.labelPx()) * 0.5F);
            HudChrome.text(dl, t.label(), t.labelPx(), left, statusTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a * 0.85F), view.status(), guard);
         }

         for (SpotifyHudRenderer.LyricItem item : view.items()) {
            float alpha = a * item.alpha();
            if (!(alpha <= 0.004F)) {
               float rowTop = top + item.top();
               if (item.text() == null) {
                  float centerY = rowTop + (this.capTop(t.text(), t.textPx()) + HudChrome.ascent(t.text(), t.textPx())) * 0.5F;
                  this.drawLyricBreak(dl, colors, left, centerY, s, item.highlight(), item.progress(), alpha);
               } else {
                  int color = HudChrome.scaleAlpha(GuiPalette.lerpColor(colors.upcoming(), colors.lyric(), item.highlight()), alpha);
                  int lit = HudChrome.scaleAlpha(colors.lit(), alpha);
                  float edge = Math.min(clipRight, (float)Math.floor(left) + Math.round(item.fill()));
                  if (item.fill() <= 0.0F) {
                     HudChrome.text(dl, t.text(), t.textPx(), left, Math.round(rowTop), color, item.text(), guard);
                  } else if (edge >= clipRight) {
                     HudChrome.text(dl, t.text(), t.textPx(), left, Math.round(rowTop), lit, item.text(), guard);
                  } else {
                     dl.pushClipRect(clipLeft, clipTop, edge, bottom, true);
                     HudChrome.text(dl, t.text(), t.textPx(), left, Math.round(rowTop), lit, item.text(), guard);
                     dl.popClipRect();
                     dl.pushClipRect(edge, clipTop, clipRight, bottom, true);
                     HudChrome.text(dl, t.text(), t.textPx(), left, Math.round(rowTop), color, item.text(), guard);
                     dl.popClipRect();
                  }
               }
            }
         }

         dl.popClipRect();
      }
   }

   private void drawLyricBreak(
      ImDrawList dl, SpotifyHudRenderer.SpotifyColors colors, float left, float centerY, float s, float highlight, float progress, float alpha
   ) {
      float r = Math.max(1.0F, 1.6F * s);
      float step = Math.max(4.0F, (float)Math.round(6.0F * s));
      float breath = 0.5F - 0.5F * (float)Math.cos(this.lyricBreath / 1.6F * 2.0 * Math.PI);

      for (int i = 0; i < 3; i++) {
         float lit = smooth(progress * 3.0F - i) * highlight;
         int color = HudChrome.scaleAlpha(GuiPalette.lerpColor(colors.upcoming(), colors.lit(), lit), alpha);
         float radius = r * (1.0F + 0.3F * lit + 0.12F * breath * highlight);
         dl.addCircleFilled(left + r + i * step, centerY, radius, color, 12);
      }
   }

   private void foldTrack(NowPlaying now, ImFont font) {
      ImGuiManager fonts = ImGuiManager.getInstance();
      fonts.requestGlyphs(now.title());
      fonts.requestGlyphs(now.artist());
      if (now != this.spotifyFolded || fonts.fontGeneration() != this.foldedGeneration) {
         this.spotifyFolded = now;
         this.foldedGeneration = fonts.fontGeneration();
         IntPredicate drawable = cp -> this.hudHasGlyph(font, cp);
         String title = DisplayText.fold(now.title(), drawable);
         this.spotifyTitle = title.isEmpty() ? now.title() : title;
         this.spotifyArtist = DisplayText.fold(now.artist(), drawable);
      }
   }

   private float[] marquee(
      String title, String artist, float titleW, float artistW, float avail, boolean scroll, boolean advancing, float loopGap, float s, float dt
   ) {
      if (!title.equals(this.marqueeTitle) || !artist.equals(this.marqueeArtist)) {
         this.marqueeTitle = title;
         this.marqueeArtist = artist;
         this.marqueeTime = 0.0F;
      }

      float titleOffset = 0.0F;
      float artistOffset = 0.0F;
      float longest = Math.max(titleW > avail ? titleW : 0.0F, artistW > avail ? artistW : 0.0F);
      if (scroll && longest > 0.0F) {
         float speed = 30.0F * s;
         float period = 2.4F + (longest + loopGap) / speed;
         this.marqueeTime %= period;
         if (advancing || this.marqueeTime > 2.4F) {
            this.marqueeTime = (this.marqueeTime + dt) % period;
         }

         float travelled = Math.max(0.0F, this.marqueeTime - 2.4F) * speed;
         if (titleW > avail) {
            titleOffset = Math.min(travelled, titleW + loopGap);
         }

         if (artistW > avail) {
            artistOffset = Math.min(travelled, artistW + loopGap);
         }
      } else {
         this.marqueeTime = 0.0F;
      }

      return new float[]{titleOffset, artistOffset};
   }

   private static void marqueeLine(
      ImDrawList dl,
      ImFont font,
      int px,
      float x,
      float top,
      float avail,
      float textW,
      float offset,
      float loopGap,
      boolean scroll,
      String text,
      int color,
      boolean guard
   ) {
      if (textW <= avail + 0.5F) {
         HudChrome.text(dl, font, px, x, top, color, text, guard);
      } else if (!scroll) {
         HudChrome.text(dl, font, px, x, top, color, ellipsize(font, px, text, avail), guard);
      } else {
         dl.pushClipRect(x, top - 2.0F, x + avail, top + px + 3.0F, true);
         HudChrome.text(dl, font, px, x - offset, top, color, text, guard);
         if (offset > 0.0F) {
            HudChrome.text(dl, font, px, x - offset + textW + loopGap, top, color, text, guard);
         }

         dl.popClipRect();
      }
   }

   private static String ellipsize(ImFont font, int px, String text, float maxW) {
      float room = maxW - HudChrome.width(font, px, "...");
      int lo = 0;
      int hi = text.length();

      while (lo < hi) {
         int mid = lo + hi + 1 >>> 1;
         if (HudChrome.width(font, px, text.substring(0, mid)) <= room) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      return text.substring(0, lo).stripTrailing() + "...";
   }

   private static String formatSeconds(long totalSeconds) {
      long seconds = Math.max(0L, totalSeconds);
      long hours = seconds / 3600L;
      long minutes = seconds / 60L % 60L;
      StringBuilder out = new StringBuilder(8);
      if (hours > 0L) {
         out.append(hours).append(':').append(minutes < 10L ? "0" : "");
      }

      out.append(minutes).append(':');
      if (seconds % 60L < 10L) {
         out.append('0');
      }

      return out.append(seconds % 60L).toString();
   }

   private float capTop(ImFont font, int px) {
      if (font != null && !(font.getFontSize() <= 0.0F)) {
         long key = (long)System.identityHashCode(font) << 16 ^ px;
         Float cached = this.capTops.get(key);
         if (cached != null) {
            return cached;
         } else {
            ImFontGlyph glyph = font.findGlyphNoFallback(72);
            float top = glyph.isValidPtr() ? glyph.getY0() * px / font.getFontSize() : px * 0.2F;
            float value = Math.max(0.0F, (float)Math.round(top));
            if (this.capTops.size() > 64) {
               this.capTops.clear();
            }

            this.capTops.put(key, value);
            return value;
         }
      } else {
         return Math.round(px * 0.2F);
      }
   }

   private HudTexture spotifyArtTexture(AlbumArt art) {
      if (art == this.spotifyArt) {
         return this.spotifyArtTexture;
      } else {
         this.releaseSpotifyArt();
         if (art == null) {
            return null;
         } else {
            this.spotifyArt = art;
            ByteBuffer pixels = MemoryUtil.memAlloc(art.byteSize());

            try {
               art.copyTo(pixels);
               pixels.flip();
               this.spotifyArtTexture = HudTexture.fromPixels(pixels, art.size(), art.size(), false);
            } catch (RuntimeException var7) {
               this.spotifyArtTexture = null;
            } finally {
               MemoryUtil.memFree(pixels);
            }

            return this.spotifyArtTexture;
         }
      }
   }

   private void releaseSpotifyArt() {
      if (this.spotifyArtTexture != null) {
         GL11.glDeleteTextures(this.spotifyArtTexture.id());
      }

      this.spotifyArtTexture = null;
      this.spotifyArt = null;
   }

   private int hudAccent() {
      return GuiPalette.abgr(SpotifyHud.get().accentColor());
   }

   private static float easeOutCubic(float value) {
      float t = clamp01(value);
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static float smooth(float value) {
      float t = clamp01(value);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float elementScale(HudElement state) {
      return !Float.isNaN(state.scale) && !Float.isInfinite(state.scale) ? Math.max(0.5F, Math.min(3.0F, state.scale)) : 1.0F;
   }

   static float scaleOf(HudElement state) {
      return elementScale(state) * ImGuiManager.getInstance().uiScale();
   }


   @Environment(EnvType.CLIENT)
   private record DrawTransform(float xOffset, float yOffset, float alpha) {
      private static SpotifyHudRenderer.DrawTransform identity() {
         return new SpotifyHudRenderer.DrawTransform(0.0F, 0.0F, 1.0F);
      }

      private SpotifyHudRenderer.DrawTransform down(float dy) {
         return dy == 0.0F ? this : new SpotifyHudRenderer.DrawTransform(this.xOffset, this.yOffset + dy, this.alpha);
      }
   }


   @Environment(EnvType.CLIENT)
   private record LyricItem(String text, float top, float highlight, float alpha, float progress, float fill) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricShift(float distance, float at, int start, float stagger) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricView(List<SpotifyHudRenderer.LyricItem> items, String status, float height) {
   }

   @Environment(EnvType.CLIENT)
   private record LyricWords(int[] from, int[] to, long[] start, long[] sweep) {
   }


   @Environment(EnvType.CLIENT)
   private record SpotifyCard(
      NowPlaying track,
      boolean sample,
      boolean art,
      float header,
      float textTop,
      float column,
      boolean timeline,
      String elapsed,
      String remaining,
      float progress,
      boolean progressBar,
      SpotifyHudRenderer.LyricView lyrics,
      float lyricAlpha
   ) {
   }

   @Environment(EnvType.CLIENT)
   private record SpotifyColors(int title, int artist, int lyric, int upcoming, int lit) {
      static final SpotifyHudRenderer.SpotifyColors DEFAULT = new SpotifyHudRenderer.SpotifyColors(
         HudChrome.TEXT_NAME, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE
      );

      static SpotifyHudRenderer.SpotifyColors of(SpotifyHud module) {
         if (module == null) {
            return DEFAULT;
         } else {
            SpotifyHud.LyricHighlight mode = module.lyricHighlight();
            int lyric = GuiPalette.abgr(module.lyricsColor());
            int highlight = GuiPalette.abgr(module.highlightColor());
            return new SpotifyHudRenderer.SpotifyColors(
               GuiPalette.abgr(module.titleColor()),
               GuiPalette.abgr(module.artistColor()),
               mode == SpotifyHud.LyricHighlight.LINE ? highlight : lyric,
               GuiPalette.abgr(module.upcomingLyricsColor()),
               mode == SpotifyHud.LyricHighlight.OFF ? lyric : highlight
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyGlyph {
      NONE,
      PLAYING,
      PAUSED;
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyLook {
      HIDDEN,
      IDLE,
      COMPACT,
      CARD;
   }

   @Environment(EnvType.CLIENT)
   private record SpotifyPlan(
      SpotifyHudRenderer.Type type,
      SpotifyHudRenderer.SpotifyLook look,
      SpotifyHudRenderer.SpotifyGlyph glyph,
      String title,
      String artist,
      float titleW,
      float artistW,
      float titleOffset,
      float artistOffset,
      float loopGap,
      boolean scroll,
      SpotifyHudRenderer.SpotifyCard card,
      SpotifyHudRenderer.SpotifyColors colors,
      float width,
      float height
   ) {
   }


   @Environment(EnvType.CLIENT)
   private record Type(ImFont value, int valuePx, ImFont text, int textPx, ImFont label, int labelPx) {
      static SpotifyHudRenderer.Type of(float scale) {
         ImGuiManager m = ImGuiManager.getInstance();
         return new SpotifyHudRenderer.Type(
            m.hudValueFont(), HudChrome.px(18.0F, scale), m.hudTextFont(), HudChrome.px(17.0F, scale), m.hudLabelFont(), HudChrome.px(14.0F, scale)
         );
      }
   }


}
