package dev.venti.modules.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.venti.compat.Mc;
import dev.venti.gui.ClickGuiScreen;
import dev.venti.gui.image.ThemeImages;
import dev.venti.imgui.ImGuiManager;
import dev.venti.mixin.hud.ToastEntryAccessor;
import dev.venti.mixin.hud.ToastManagerAccessor;
import dev.venti.module.Module;
import dev.venti.module.Modules;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

/**
 * The Anubis HUD overlay, cut down to the info bar, coordinates, module list, potions, hotkeys, armor and item cooldowns.
 * Edit mode (drag to move, scroll to resize, eye chips to hide) is on while the HUD or Hotkeys page is open in the click GUI.
 */
public final class HudRenderer {
   private static final HudRenderer INSTANCE = new HudRenderer();
   private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private static final String[] SECONDS = IntStream.range(0, 60).mapToObj(i -> String.format(":%02d", i)).toArray(String[]::new);
   private static final String[] COORD_SLOTS = new String[]{"coords.0", "coords.1", "coords.2"};
   private static volatile boolean resetRequested;
   private static volatile float[] infoBarBox;
   private static volatile long infoBarDrawnAt;
   private final HudLayoutStore layout = HudLayoutStore.getInstance();
   private final ModuleListRows moduleRows = new ModuleListRows();
   private final ArmorStatus armorStatus = new ArmorStatus();
   private final CooldownTray cooldownTray = new CooldownTray();
   private final Map<String, Int2ObjectOpenHashMap<StableWidth>> slots = new HashMap<>();
   private final Map<String, Float> coordAnim = new HashMap<>();
   private final Map<Long, Float> capTops = new HashMap<>();
   private int slotCount;
   private float moduleListW;
   private float toastShift;
   private boolean layoutDirty;
   private long lastWheelNanos;
   private HudModule hudModule;
   private float introTime;
   private boolean hudVisibleLastFrame;
   private String draggingId;
   private float dragOffsetX;
   private float dragOffsetY;
   private Float snapGuideX;
   private Float snapGuideY;
   private boolean overUi;
   private List<float[]> lastChips = List.of();
   private long infoSecond = Long.MIN_VALUE;
   private int infoFps = -1;
   private int infoPing = -1;
   private List<HudRenderer.InfoItem> infoItems = List.of();
   private List<HudRenderer.HotkeyLine> hotkeyLines = List.of();
   private long hotkeyRevision = Long.MIN_VALUE;
   private ImFont hotkeyFont;
   private int hotkeyTextPx = -1;
   private int hotkeyKeyPx = -1;
   private float hotkeyNameColumn;
   private float hotkeyKeyColumn;
   private float hotkeyHeaderW;
   private float hotkeyCapMid;

   private HudRenderer() {
   }

   public static HudRenderer getInstance() {
      return INSTANCE;
   }

   /** The info bar's screen box while it is drawn, so toasts can sit below it. */
   static float[] infoBarBox() {
      return System.nanoTime() - infoBarDrawnAt < 250000000L ? infoBarBox : null;
   }

   static void requestLayoutReset() {
      resetRequested = true;
   }

   /** Edit mode: the HUD or Hotkeys page is open in the click GUI. */
   public static boolean editing() {
      return ClickGuiScreen.editingHud();
   }

   /** True when anything needs drawing, so ImGui can stay idle otherwise. */
   public boolean hasWork() {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         this.resetCombatElements();
         return false;
      } else if (editing()) {
         return true;
      } else {
         HudModule hud = HudModule.get();
         return hud != null && hud.isEnabled() || HotkeysModule.shouldShowHud() || this.hudVisibleLastFrame;
      }
   }

   public void render() {
      Minecraft client = Minecraft.getInstance();
      this.hudModule = HudModule.get();
      if (this.hudModule == null) {
         return;
      }

      if (client.level == null) {
         this.resetCombatElements();
         return;
      }

      boolean editMode = editing();
      float dt = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
      float screenW = ImGui.getIO().getDisplaySizeX();
      float screenH = ImGui.getIO().getDisplaySizeY();
      if (resetRequested) {
         resetRequested = false;
         this.layout.resetToDefaults(screenW, screenH);
         this.clearSlots();
      }

      boolean guiOpen = Mc.screen() instanceof ClickGuiScreen;
      boolean hudOn = this.hudModule.isEnabled();
      boolean hotkeysOn = HotkeysModule.shouldShowHud();
      if (!editMode && (guiOpen || Mc.hudHidden() || !hudOn && !hotkeysOn)) {
         if (this.draggingId != null || this.layoutDirty) {
            this.saveLayout();
         }

         this.draggingId = null;
         this.snapGuideX = null;
         this.snapGuideY = null;
         this.hudVisibleLastFrame = false;
         this.introTime = 0.0F;
         this.clearSlots();
         this.moduleListW = 0.0F;
         this.toastShift = 0.0F;
         this.resetCombatElements();
         return;
      }

      boolean chatEditMode = Mc.screen() instanceof ChatScreen;
      boolean acceptInput = editMode || chatEditMode;
      this.flushLayoutIfIdle();
      boolean showPing = editMode || this.hudModule.ping();
      boolean showFps = editMode || this.hudModule.fps();
      boolean showClock = editMode || this.hudModule.clock();
      boolean shadow = this.hudModule.shadow();
      this.updateIntro(editMode, dt);
      this.moduleRows.update(dt);
      if (this.slotCount > 256) {
         this.clearSlots();
      }

      HudLayoutStore.HudElementState infoBar = this.layout.element("info_bar");
      HudLayoutStore.HudElementState coordinates = this.layout.element("coordinates");
      HudLayoutStore.HudElementState moduleList = this.layout.element("module_list");
      HudLayoutStore.HudElementState potionTracker = this.layout.element("potion_tracker");
      HudLayoutStore.HudElementState hotkeys = this.layout.element("hotkeys");
      HudLayoutStore.HudElementState armor = this.layout.element("armor_status");
      HudLayoutStore.HudElementState cooldowns = this.layout.element("item_cooldowns");
      HudRenderer.InfoPlan infoPlan = this.planInfoBar(infoBar, this.buildInfoItems(showFps, showClock, showPing, client), dt);
      this.layout.updateBounds(infoBar.id, infoPlan.width(), infoPlan.height(), screenW, screenH);
      HudRenderer.CoordPlan coordsPlan = this.planCoordinates(coordinates, this.coordinateRows(client, editMode, dt), editMode, dt);
      this.layout.updateBounds(coordinates.id, coordsPlan.width(), coordsPlan.height(), screenW, screenH);
      HudRenderer.ListPlan listPlan = this.planModuleList(moduleList, editMode, dt);
      this.layout.updateBounds(moduleList.id, listPlan.width(), listPlan.height(), screenW, screenH);
      HudRenderer.PotionPlan potionPlan = this.planPotions(potionTracker, this.buildPotionRows(client), editMode, dt);
      this.layout.updateBounds(potionTracker.id, potionPlan.width(), potionPlan.height(), screenW, screenH);
      HudRenderer.HotkeyPlan hotkeyPlan = this.planHotkeys(hotkeys, editMode, dt);
      this.layout.updateBounds(hotkeys.id, hotkeyPlan.width(), hotkeyPlan.height(), screenW, screenH);
      boolean combatHud = hudOn && !Mc.hudHidden();
      boolean armorWanted = combatHud && armor.visible && this.hudModule.armorStatus();
      boolean cooldownWanted = combatHud && cooldowns.visible && this.hudModule.itemCooldowns();
      float armorScale = scaleOf(armor);
      float cooldownScale = scaleOf(cooldowns);
      this.armorStatus.update(client, this.hudModule, armorWanted, editMode, dt);
      this.layout.updateBounds(armor.id, this.armorStatus.planWidth(armorScale), this.armorStatus.planHeight(armorScale), screenW, screenH);
      this.cooldownTray.update(client, this.hudModule, cooldownWanted, editMode, dt);
      this.layout.updateBounds(cooldowns.id, this.cooldownTray.planWidth(cooldownScale), this.cooldownTray.planHeight(cooldownScale), screenW, screenH);
      // The click GUI window is hovered (as of the last frame): leave the mouse to it.
      this.overUi = guiOpen && ImGui.isWindowHovered(1 | 2 | 4 | 8 | 32);
      if (acceptInput) {
         this.updateEditorInput(screenW, screenH);
      } else {
         if (this.draggingId != null || this.layoutDirty) {
            this.saveLayout();
         }

         this.draggingId = null;
         this.snapGuideX = null;
         this.snapGuideY = null;
      }

      float listShift = this.updateToastShift(client, moduleList, editMode, dt);
      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(screenW, screenH);
      ImGui.pushStyleVar(4, 0.0F);
      // NoDecoration | NoMove | NoSavedSettings | NoFocusOnAppearing | NoBringToFrontOnFocus | NoNav | NoBackground | NoInputs
      boolean open = ImGui.begin("##venti_hud", 205215 | 197120 | 128);
      ImGui.popStyleVar();
      if (open) {
         ImDrawList dl = ImGui.getBackgroundDrawList();
         boolean gameHidden = Mc.hudHidden() || client.debugEntries.isOverlayVisible();
         boolean infoBarShown = hudOn && infoBar.visible && (this.hudModule.fps() || this.hudModule.clock() || this.hudModule.ping());
         boolean coordsShown = hudOn && coordinates.visible;
         boolean listShown = hudOn && moduleList.visible && this.hudModule.moduleList();
         boolean potionsShown = hudOn && potionTracker.visible && this.hudModule.potions();
         boolean hotkeysShown = hotkeysOn && hotkeys.visible && !gameHidden;
         if (infoBarShown && !infoPlan.segments().isEmpty() || editMode) {
            this.drawInfoBar(dl, infoBar, infoPlan, shadow, this.transform(infoBar, editMode, infoBarShown));
         }

         if (infoBarShown && !infoPlan.segments().isEmpty() && !editMode) {
            infoBarBox = new float[]{infoBar.x, infoBar.y, infoBar.x + infoBar.width, infoBar.y + infoBar.height};
            infoBarDrawnAt = System.nanoTime();
         }

         if (coordsShown && !coordsPlan.rows().isEmpty() || editMode) {
            this.drawCoordinates(dl, coordinates, coordsPlan, shadow, this.transform(coordinates, editMode, coordsShown), screenW);
         }

         if (listShown && !listPlan.rows().isEmpty() || editMode) {
            this.drawModuleList(dl, moduleList, listPlan, shadow, this.transform(moduleList, editMode, listShown).down(listShift), screenW);
         }

         if (potionsShown && !potionPlan.lines().isEmpty() || editMode) {
            this.drawPotions(dl, potionTracker, potionPlan, shadow, this.transform(potionTracker, editMode, potionsShown));
         }

         if (hotkeysShown && !hotkeyPlan.lines().isEmpty() || editMode) {
            this.drawHotkeys(dl, hotkeys, hotkeyPlan, shadow, this.transform(hotkeys, editMode, hotkeysShown));
         }

         if (this.armorStatus.visible()) {
            HudRenderer.DrawTransform tr = this.transform(armor, editMode, armorWanted);
            this.armorStatus.draw(dl, armor, Math.round(armor.x + tr.xOffset()), Math.round(armor.y + tr.yOffset()), tr.alpha(), shadow, client);
         }

         if (this.cooldownTray.visible()) {
            HudRenderer.DrawTransform tr = this.transform(cooldowns, editMode, cooldownWanted);
            this.cooldownTray
               .draw(dl, cooldowns, Math.round(cooldowns.x + tr.xOffset()), Math.round(cooldowns.y + tr.yOffset()), tr.alpha(), shadow, this.hudAccent());
         }

         if (editMode) {
            this.drawEditorChrome(dl, screenW, screenH);
         } else {
            this.lastChips = List.of();
            this.drawSnapGuides(dl, screenW, screenH);
         }
      }

      ImGui.end();
   }

   private void resetCombatElements() {
      this.armorStatus.reset();
      this.cooldownTray.reset();
   }

   private boolean overChip(float x, float y) {
      for (float[] chip : this.lastChips) {
         if (x >= chip[0] && x <= chip[0] + chip[2] && y >= chip[1] && y <= chip[1] + chip[3]) {
            return true;
         }
      }

      return false;
   }

   /** Changes whenever a module's name, key bind or on/off state changes, so the hotkey rows are rebuilt only then. */
   private static long hotkeySignature() {
      long hash = 1469598103934665603L;
      for (Module module : Modules.all()) {
         int key = module.settings().bind;
         if (key > 0) {
            hash = (hash ^ module.name().hashCode()) * 1099511628211L;
            hash = (hash ^ key) * 1099511628211L;
            hash = (hash ^ (module.isEnabled() ? 1 : 2)) * 1099511628211L;
         }
      }

      return hash;
   }

   private static String keyName(int key) {
      //? if >=26.3 {
      /*return key <= 0 ? "NONE" : dev.venti.compat.Keys.name(key);
      *///?} else {
      return key <= 0 ? "NONE" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
      //?}
   }

   private HudRenderer.DrawTransform transform(HudLayoutStore.HudElementState state, boolean editMode, boolean shownInGame) {
      HudRenderer.DrawTransform tr = this.introTransform(state, editMode);
      return editMode && !shownInGame ? new HudRenderer.DrawTransform(tr.xOffset(), tr.yOffset(), tr.alpha() * 0.35F) : tr;
   }


   private void saveLayout() {
      this.layout.save();
      this.layoutDirty = false;
   }

   private void flushLayoutIfIdle() {
      if (this.layoutDirty && System.nanoTime() - this.lastWheelNanos > 400000000L) {
         this.saveLayout();
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

   private HudRenderer.DrawTransform introTransform(HudLayoutStore.HudElementState state, boolean editMode) {
      if (editMode) {
         return HudRenderer.DrawTransform.identity();
      } else {
         float t = clamp01((this.introTime - introDelay(state.id)) / 0.36F);
         float eased = easeOutCubic(t);
         float slide = 6.0F * scaleOf(state) * (1.0F - eased);

         return switch (state.anchorX) {
            case 1 -> new HudRenderer.DrawTransform(0.0F, -slide, eased);
            case 2 -> new HudRenderer.DrawTransform(slide, 0.0F, eased);
            default -> new HudRenderer.DrawTransform(-slide, 0.0F, eased);
         };
      }
   }

   private static float introDelay(String id) {
      return switch (id) {
         case "info_bar" -> 0.04F;
         case "coordinates" -> 0.08F;
         case "module_list" -> 0.16F;
         case "hotkeys" -> 0.16F;
         case "potion_tracker" -> 0.19999999F;
         case "armor_status" -> 0.16F;
         case "item_cooldowns" -> 0.12F;
         default -> 0.0F;
      };
   }

   private float slot(String key, int px, float target, float dt) {
      Int2ObjectOpenHashMap<StableWidth> bySize = this.slots.computeIfAbsent(key, k -> new Int2ObjectOpenHashMap());
      StableWidth width = (StableWidth)bySize.get(px);
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

   private void clearSlots() {
      this.slots.clear();
      this.slotCount = 0;
   }

   private static String number(long value) {
      return value < 0L ? "−" + -value : Long.toString(value);
   }

   private List<HudRenderer.InfoItem> buildInfoItems(boolean showFps, boolean showClock, boolean showPing, Minecraft client) {
      long second = showClock ? System.currentTimeMillis() / 1000L : -1L;
      int fps = showFps ? Math.max(0, client.getFps()) : -1;
      int ping = -1;
      if (showPing) {
         ClientPacketListener conn = client.getConnection();
         int latency = 0;
         if (conn != null && client.player != null) {
            latency = readTabLatency(client, conn);
            if (latency <= 0) {
               latency = readServerPing(conn);
            }
         }

         ping = Math.max(0, latency);
      }

      if (second == this.infoSecond && fps == this.infoFps && ping == this.infoPing) {
         return this.infoItems;
      } else {
         this.infoSecond = second;
         this.infoFps = fps;
         this.infoPing = ping;
         List<HudRenderer.InfoItem> items = new ArrayList<>(3);
         if (showClock) {
            LocalTime now = LocalTime.now();
            items.add(new HudRenderer.InfoItem("info.time", now.format(CLOCK_FORMAT), SECONDS[now.getSecond()], null));
         }

         if (showFps) {
            items.add(new HudRenderer.InfoItem("info.fps", String.valueOf(fps), null, "fps"));
         }

         if (showPing) {
            items.add(new HudRenderer.InfoItem("info.ping", String.valueOf(ping), null, "ms"));
         }

         this.infoItems = List.copyOf(items);
         return this.infoItems;
      }
   }

   private HudRenderer.InfoPlan planInfoBar(HudLayoutStore.HudElementState state, List<HudRenderer.InfoItem> items, float dt) {
      float s = scaleOf(state);
      HudRenderer.Type t = HudRenderer.Type.of(s);
      List<HudRenderer.Segment> segments = new ArrayList<>(items.size());
      float total = 12.0F * s * 2.0F;

      for (int i = 0; i < items.size(); i++) {
         HudRenderer.InfoItem item = items.get(i);
         float valueW = HudChrome.tabularWidth(t.value(), t.valuePx(), item.value());
         float dimW = item.dim() != null ? HudChrome.tabularWidth(t.value(), t.valuePx(), item.dim()) : 0.0F;
         float content = valueW + dimW;
         if (item.unit() != null) {
            content += 4.0F * s + HudChrome.width(t.label(), t.labelPx(), item.unit());
         }

         float slotW = this.slot(item.slot(), t.valuePx(), (float)Math.ceil(content), dt);
         segments.add(new HudRenderer.Segment(item, valueW, dimW, content, slotW));
         total += slotW;
      }

      if (segments.size() > 1) {
         total += (segments.size() - 1) * (10.0F * s * 2.0F + 1.0F);
      }

      float emblemW = 0.0F;
      ThemeImages.Texture emblem = this.hudModule != null && this.hudModule.logo() && !segments.isEmpty() ? HudEmblem.forHeight(Math.round(24.0F * s)) : null;
      if (emblem != null) {
         emblemW = emblem.width();
         total += emblemW + 10.0F * s * 2.0F + 1.0F;
      }

      return new HudRenderer.InfoPlan(segments, t, (float)Math.round(total), (float)Math.round(28.0F * s), emblemW);
   }

   private void drawInfoBar(ImDrawList dl, HudLayoutStore.HudElementState state, HudRenderer.InfoPlan plan, boolean shadow, HudRenderer.DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         int surface = fill(state);
         boolean guard = HudChrome.needsTextGuard(surface);
         HudChrome.card(dl, surface, x, y, w, h, hudRounding() * s, a, shadow, s);
         HudRenderer.Type t = plan.type();
         float valueTop = y + (float)Math.floor((h - t.valuePx()) * 0.5F);
         float labelTop = valueTop + HudChrome.baselineOffset(t.value(), t.valuePx(), t.label(), t.labelPx());
         float cx = x + 12.0F * s;
         ThemeImages.Texture emblem = plan.emblemW() > 0.0F ? HudEmblem.forHeight(Math.round(24.0F * s)) : null;
         if (emblem != null) {
            float ex = Math.round(cx);
            float ey = Math.round(y + (h - emblem.height()) * 0.5F);
            dl.addImage(emblem.id(), ex, ey, ex + emblem.width(), ey + emblem.height(), 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(this.hudAccent(), a));
            cx += plan.emblemW();
            HudChrome.divider(dl, cx + 10.0F * s, y + h * 0.5F, 12.0F * s, a);
            cx += 10.0F * s * 2.0F + 1.0F;
         }

         List<HudRenderer.Segment> segments = plan.segments();

         for (int i = 0; i < segments.size(); i++) {
            HudRenderer.Segment seg = segments.get(i);
            HudRenderer.InfoItem item = seg.item();
            float vx = item.unit() != null ? cx + seg.slotW() - seg.contentW() : cx;
            HudChrome.tabular(dl, t.value(), t.valuePx(), vx, valueTop, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, a), item.value(), guard);
            vx += seg.valueW();
            if (item.dim() != null) {
               HudChrome.tabular(dl, t.value(), t.valuePx(), vx, valueTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), item.dim(), guard);
               vx += seg.dimW();
            }

            if (item.unit() != null) {
               HudChrome.text(dl, t.label(), t.labelPx(), vx + 4.0F * s, labelTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), item.unit(), guard);
            }

            cx += seg.slotW();
            if (i < segments.size() - 1) {
               HudChrome.divider(dl, cx + 10.0F * s, y + h * 0.5F, 12.0F * s, a);
               cx += 10.0F * s * 2.0F + 1.0F;
            }
         }
      }
   }

   private static float rowsRadius(float s) {
      return Math.min(hudRounding(), 6.0F) * s;
   }

   private void drawRows(
      ImDrawList dl, List<HudRenderer.Step> steps, int surface, float x, float y, float w, float h, float radius, float s, boolean shadow
   ) {
      if (shadow) {
         drawRowsShadow(dl, steps, radius, s);
      }

      int fill = surface;

      for (int i = 0; i < steps.size(); i++) {
         HudRenderer.Step q = steps.get(i);
         HudRenderer.Step up = i > 0 ? steps.get(i - 1) : null;
         HudRenderer.Step dn = i < steps.size() - 1 ? steps.get(i + 1) : null;
         int corners = convexCorners(q, up, dn);
         int color = HudChrome.scaleAlpha(fill, q.alpha());
         if (corners == 256) {
            dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), color);
         } else {
            dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), color, radius, corners);
         }

         float stepH = q.b() - q.t();
         if (up != null && up.l() < q.l() - 0.5F) {
            fillet(dl, q.l(), q.t(), -1, 1, Math.min(radius, Math.min(q.l() - up.l(), stepH)), color);
         }

         if (up != null && up.r() > q.r() + 0.5F) {
            fillet(dl, q.r(), q.t(), 1, 1, Math.min(radius, Math.min(up.r() - q.r(), stepH)), color);
         }

         if (dn != null && dn.l() < q.l() - 0.5F) {
            fillet(dl, q.l(), q.b(), -1, -1, Math.min(radius, Math.min(q.l() - dn.l(), stepH)), color);
         }

         if (dn != null && dn.r() > q.r() + 0.5F) {
            fillet(dl, q.r(), q.b(), 1, -1, Math.min(radius, Math.min(dn.r() - q.r(), stepH)), color);
         }
      }
   }

   private static int convexCorners(HudRenderer.Step q, HudRenderer.Step up, HudRenderer.Step dn) {
      int flags = 0;
      if (up == null || up.l() > q.l() + 0.5F) {
         flags |= 16;
      }

      if (up == null || up.r() < q.r() - 0.5F) {
         flags |= 32;
      }

      if (dn == null || dn.l() > q.l() + 0.5F) {
         flags |= 64;
      }

      if (dn == null || dn.r() < q.r() - 0.5F) {
         flags |= 128;
      }

      return flags == 0 ? 256 : flags;
   }

   private static void fillet(ImDrawList dl, float px, float py, int dirX, int dirY, float r, int color) {
      if (!(r < 1.0F)) {
         float cx = px + dirX * r;
         float cy = py + dirY * r;
         float a0 = dirX < 0 ? 0.0F : (float) Math.PI;
         float a1 = dirY > 0 ? (dirX < 0 ? (float) (-Math.PI / 2) : (float) (Math.PI * 3.0 / 2.0)) : (float) (Math.PI / 2);
         dl.pathClear();
         dl.pathLineTo(px, py);
         dl.pathArcTo(cx, cy, r, a0, a1, 8);
         dl.pathFillConcave(color);
      }
   }

   private static void drawRowsShadow(ImDrawList dl, List<HudRenderer.Step> steps, float radius, float s) {
      for (int k = 3; k >= 1; k--) {
         float spread = k * Math.max(1.0F, s);

         for (int i = 0; i < steps.size(); i++) {
            HudRenderer.Step q = steps.get(i);
            boolean first = i == 0;
            boolean last = i == steps.size() - 1;
            int corners = convexCorners(q, first ? null : steps.get(i - 1), last ? null : steps.get(i + 1));
            float top = q.t() - (first ? spread : 0.0F);
            float bottom = q.b() + (last ? spread + Math.max(1.0F, s) : 0.0F);
            int color = HudChrome.scaleAlpha(218103808, q.alpha());
            if (corners == 256) {
               dl.addRectFilled(q.l() - spread, top, q.r() + spread, bottom, color);
            } else {
               dl.addRectFilled(q.l() - spread, top, q.r() + spread, bottom, color, radius + spread, corners);
            }
         }
      }
   }

   private static boolean alignsRight(HudLayoutStore.HudElementState state, float screenW) {
      return switch (state.anchorX) {
         case 0 -> false;
         case 2 -> true;
         default -> state.x + state.width * 0.5F >= screenW * 0.5F;
      };
   }

   private List<HudRenderer.CoordRow> coordinateRows(Minecraft client, boolean editMode, float dt) {
      long x = 0L;
      long y = 0L;
      long z = 0L;
      if (client.player != null) {
         x = (long)Math.floor(client.player.getX());
         y = (long)Math.floor(client.player.getY());
         z = (long)Math.floor(client.player.getZ());
      }

      boolean inNether = client.level != null && client.level.dimension() == Level.NETHER;
      boolean inEnd = client.level != null && client.level.dimension() == Level.END;
      boolean overworldOn = this.hudModule.coordinates();
      boolean netherOn = this.hudModule.netherCoordinates();
      float k = 1.0F - (float)Math.exp(-12.0F * dt);
      List<HudRenderer.CoordRow> rows = new ArrayList<>(2);
      if (inEnd) {
         this.addCoordRow(rows, "end", "end_stone", true, overworldOn || netherOn, editMode, new long[]{x, y, z}, k);
      } else {
         long[] overworld = inNether ? new long[]{x * 8L, y, z * 8L} : new long[]{x, y, z};
         long[] nether = inNether ? new long[]{x, y, z} : new long[]{Math.floorDiv(x, 8), y, Math.floorDiv(z, 8)};
         this.addCoordRow(rows, "overworld", "grass_block_side", !inNether, overworldOn, editMode, overworld, k);
         this.addCoordRow(rows, "nether", "netherrack", inNether, netherOn, editMode, nether, k);
      }

      return rows;
   }

   private void addCoordRow(List<HudRenderer.CoordRow> rows, String key, String block, boolean here, boolean enabled, boolean editMode, long[] xyz, float k) {
      float target = !enabled && !editMode ? 0.0F : 1.0F;
      float anim = this.coordAnim.getOrDefault(key, target);
      anim += (target - anim) * k;
      if (Math.abs(anim - target) < 0.005F) {
         anim = target;
      }

      this.coordAnim.put(key, anim);
      if (!(anim <= 0.005F)) {
         rows.add(new HudRenderer.CoordRow(block, here, enabled, new String[]{number(xyz[0]), number(xyz[1]), number(xyz[2])}, anim));
      }
   }

   private static float coordIconSize(float s) {
      return s >= 1.0F ? 16.0F * Math.max(1.0F, (float)Math.floor(s + 0.25F)) : Math.round(16.0F * s);
   }

   private HudRenderer.CoordPlan planCoordinates(HudLayoutStore.HudElementState state, List<HudRenderer.CoordRow> rows, boolean editMode, float dt) {
      float s = scaleOf(state);
      HudRenderer.Type t = HudRenderer.Type.of(s);
      float rowFull = 20.0F * s;
      float[] valueW = new float[3];
      float height = 0.0F;

      for (HudRenderer.CoordRow row : rows) {
         for (int i = 0; i < 3; i++) {
            valueW[i] = Math.max(valueW[i], HudChrome.tabularWidth(t.text(), t.textPx(), row.values()[i]));
         }

         height += rowFull * smooth(row.anim());
      }

      float[] slotW = new float[3];
      float total = 7.0F * s * 2.0F + Math.min(coordIconSize(s), rowFull - 2.0F) + 8.0F * s + 12.0F * s * 2.0F;

      for (int i = 0; i < 3; i++) {
         slotW[i] = this.slot(COORD_SLOTS[i], t.textPx(), (float)Math.ceil(valueW[i]), dt);
         total += slotW[i];
      }

      if (editMode) {
         height = Math.max(height, rowFull);
      }

      return new HudRenderer.CoordPlan(rows, t, slotW, (float)Math.round(total), (float)Math.max(1, Math.round(height)));
   }

   private void drawCoordinates(
      ImDrawList dl, HudLayoutStore.HudElementState state, HudRenderer.CoordPlan plan, boolean shadow, HudRenderer.DrawTransform tr, float screenW
   ) {
      float a = tr.alpha();
      if (!(a <= 0.001F) && !plan.rows().isEmpty()) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         HudRenderer.Type t = plan.type();
         boolean right = alignsRight(state, screenW);
         float rowFull = 20.0F * s;
         List<HudRenderer.Step> steps = new ArrayList<>(plan.rows().size());
         float cy = y;

         for (int i = 0; i < plan.rows().size(); i++) {
            HudRenderer.CoordRow row = plan.rows().get(i);
            float open = smooth(row.anim());
            float rowH = rowFull * open;
            float top = Math.round(cy);
            float bottom = Math.round(cy + rowH);
            cy += rowH;
            if (!(bottom <= top)) {
               float slide = Math.round(w * (1.0F - open));
               float l = right ? x + slide : x - slide;
               float rowAlpha = a * (row.enabled() ? 1.0F : 0.35F) * Math.min(1.0F, row.anim() * 1.5F);
               steps.add(new HudRenderer.Step(i, l, top, l + w, bottom, rowAlpha));
            }
         }

         this.drawRows(dl, steps, fill(state), x, y, w, h, rowsRadius(s), s, shadow);
         float icon = Math.min(coordIconSize(s), rowFull - 2.0F);
         float iconR = 2.5F * s;
         float textInset = (float)Math.floor((rowFull - t.textPx()) * 0.5F);

         for (HudRenderer.Step q : steps) {
            HudRenderer.CoordRow row = plan.rows().get(q.item());
            float stepH = q.b() - q.t();
            float shift = (rowFull - stepH) * 0.5F;
            float contentAlpha = q.alpha() * row.anim();
            boolean clip = stepH < rowFull - 0.5F;
            if (clip) {
               dl.pushClipRect(q.l(), q.t(), q.r(), q.b(), true);
            }

            float ix = Math.round(q.l() + 7.0F * s);
            float iy = Math.round(q.t() + (rowFull - icon) * 0.5F - shift);
            ThemeImages.Texture texture = GameIcons.block(row.block());
            if (texture != null) {
               dl.addImageRounded(texture.id(), ix, iy, ix + icon, iy + icon, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, contentAlpha), iconR, 240);
            }

            float cx = ix + icon + 8.0F * s;
            float valueTop = Math.round(q.t() + textInset - shift);
            int color = HudChrome.scaleAlpha(-1, contentAlpha * (row.here() ? 1.0F : 0.62F));

            for (int ixx = 0; ixx < 3; ixx++) {
               HudChrome.tabular(dl, t.text(), t.textPx(), cx, valueTop, color, row.values()[ixx], true);
               cx += plan.slotW()[ixx] + 12.0F * s;
            }

            if (clip) {
               dl.popClipRect();
            }
         }
      }
   }

   private static float rowWidth(float nameW, float s) {
      return Math.round(12.0F * s + nameW);
   }

   private HudRenderer.ListPlan planModuleList(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      HudRenderer.Type t = HudRenderer.Type.of(s);
      float rowFull = 18.0F * s;
      List<ModuleListRows.Row> rows = this.moduleRows.ordered(t.text(), t.textPx(), HudChrome::width, state.anchorBottom);
      float widest = 0.0F;
      float height = 0.0F;

      for (int i = 0; i < rows.size(); i++) {
         ModuleListRows.Row row = rows.get(i);
         widest = Math.max(widest, row.nameW());
         height += rowFull * smooth(row.anim());
      }

      if (rows.isEmpty()) {
         this.moduleListW = editMode ? rowWidth(HudChrome.width(t.label(), t.labelPx(), "No active modules"), s) : 0.0F;
         return new HudRenderer.ListPlan(rows, t, (float)Math.max(1, Math.round(this.moduleListW)), (float)Math.round(rowFull));
      } else {
         if (editMode) {
            height = Math.max(height, rowFull);
         }

         float target = rowWidth(widest, s);
         if (!(target >= this.moduleListW) && !(Math.abs(target - this.moduleListW) < 0.5F)) {
            this.moduleListW = this.moduleListW + (target - this.moduleListW) * (1.0F - (float)Math.exp(-16.0F * dt));
         } else {
            this.moduleListW = target;
         }

         return new HudRenderer.ListPlan(rows, t, (float)Math.round(this.moduleListW), (float)Math.max(1, Math.round(height)));
      }
   }

   private void drawModuleList(
      ImDrawList dl, HudLayoutStore.HudElementState state, HudRenderer.ListPlan plan, boolean shadow, HudRenderer.DrawTransform tr, float screenW
   ) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         HudRenderer.Type t = plan.type();
         boolean right = alignsRight(state, screenW);
         float rowFull = 18.0F * s;
         List<HudRenderer.Step> steps = new ArrayList<>(Math.max(1, plan.rows().size()));
         if (plan.rows().isEmpty()) {
            float rowW = rowWidth(HudChrome.width(t.label(), t.labelPx(), "No active modules"), s);
            float l = right ? x + w - rowW : x;
            steps.add(new HudRenderer.Step(-1, l, y, l + rowW, (float)Math.round(y + rowFull), a));
         } else {
            float cy = y;

            for (int i = 0; i < plan.rows().size(); i++) {
               ModuleListRows.Row row = plan.rows().get(i);
               float open = smooth(row.anim());
               float rowH = rowFull * open;
               float top = Math.round(cy);
               float bottom = Math.round(cy + rowH);
               cy += rowH;
               if (!(bottom <= top)) {
                  float rowW = rowWidth(row.nameW(), s);
                  float slide = Math.round(rowW * (1.0F - open));
                  float l = right ? x + w - rowW + slide : x - slide;
                  steps.add(new HudRenderer.Step(i, l, top, l + rowW, bottom, a * Math.min(1.0F, row.anim() * 1.5F)));
               }
            }
         }

         this.drawRows(dl, steps, fill(state), x, y, w, h, rowsRadius(s), s, shadow);
         float textInset = (float)Math.floor((rowFull - t.textPx()) * 0.5F);
         float pad = 6.0F * s;

         for (HudRenderer.Step q : steps) {
            if (q.item() < 0) {
               float labelW = HudChrome.width(t.label(), t.labelPx(), "No active modules");
               float tx = right ? q.r() - pad - labelW : q.l() + pad;
               HudChrome.text(
                  dl,
                  t.label(),
                  t.labelPx(),
                  tx,
                  q.t() + (float)Math.floor((rowFull - t.labelPx()) * 0.5F),
                  HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
                  "No active modules",
                  true
               );
            } else {
               ModuleListRows.Row row = plan.rows().get(q.item());
               float stepH = q.b() - q.t();
               float tx = right ? q.r() - pad - row.nameW() : q.l() + pad;
               float ty = q.t() + textInset - (rowFull - stepH) * 0.5F;
               boolean clip = stepH < rowFull - 0.5F;
               if (clip) {
                  dl.pushClipRect(q.l(), q.t(), q.r(), q.b(), true);
               }

               HudChrome.text(
                  dl, t.text(), t.textPx(), Math.round(tx), Math.round(ty), HudChrome.scaleAlpha(-1, a * row.anim() * row.anim()), row.module().name(), true
               );
               if (clip) {
                  dl.popClipRect();
               }
            }
         }
      }
   }

   private float updateToastShift(Minecraft client, HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float target = !editMode && this.draggingId == null ? toastClearance(client, state) : 0.0F;
      if (!editMode && !(Math.abs(target - this.toastShift) < 0.5F)) {
         this.toastShift = this.toastShift + (target - this.toastShift) * (1.0F - (float)Math.exp(-12.0F * dt));
      } else {
         this.toastShift = target;
      }

      return this.toastShift;
   }

   private static float toastClearance(Minecraft client, HudLayoutStore.HudElementState state) {
      if (state.anchorX == 2 && !state.anchorBottom && !Mc.hudHidden()) {
         Window window = client.getWindow();
         int gui = window.getGuiScale();
         int guiW = window.getGuiScaledWidth();
         float bottom = 0.0F;

         for (Object entry : ((ToastManagerAccessor)Mc.toasts()).venti$VisibleToasts()) {
            ToastEntryAccessor instance = (ToastEntryAccessor)entry;
            Toast toast = instance.venti$Toast();
            float shown = instance.venti$Visibility() == Visibility.HIDE ? instance.venti$VisiblePortion() : 1.0F;
            float left = toast.xPos(guiW, shown) * gui;
            if (!(left >= state.x + state.width) && !(left + toast.width() * gui <= state.x)) {
               bottom = Math.max(bottom, (toast.yPos(instance.venti$FirstSlot()) + toast.height()) * gui);
            }
         }

         return bottom <= 0.0F ? 0.0F : Mth.clamp(bottom + 10.0F * ImGuiManager.getInstance().uiScale() - state.y, 0.0F, bottom);
      } else {
         return 0.0F;
      }
   }

   private static float iconSize(float s) {
      return s >= 1.0F ? 18.0F * Math.max(1.0F, (float)Math.floor(s + 0.25F)) : Math.round(18.0F * s);
   }

   private HudRenderer.PotionPlan planPotions(HudLayoutStore.HudElementState state, List<HudRenderer.PotionRow> rows, boolean editMode, float dt) {
      float s = scaleOf(state);
      HudRenderer.Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      List<HudRenderer.PotionRow> sorted = new ArrayList<>(rows);
      sorted.sort(Comparator.comparingInt(HudRenderer.PotionRow::ticks).thenComparing(HudRenderer.PotionRow::name));
      if (state.anchorBottom) {
         Collections.reverse(sorted);
      }

      List<HudRenderer.PotionLine> lines = new ArrayList<>(sorted.size());
      float target = 0.0F;
      float fixed = 6.0F * s * 2.0F + Math.min(iconSize(s), rowFull - 2.0F) + 6.0F * s + 14.0F * s;

      for (HudRenderer.PotionRow row : sorted) {
         float nameW = HudChrome.width(t.text(), t.textPx(), row.name());
         float ampW = row.amplifier().isEmpty() ? 0.0F : HudChrome.width(t.text(), t.textPx(), row.amplifier());
         float durationW = row.infinite() ? HudChrome.width(t.text(), t.textPx(), "∞") : HudChrome.tabularWidth(t.text(), t.textPx(), row.duration());
         lines.add(new HudRenderer.PotionLine(row, nameW, ampW, durationW));
         target = Math.max(target, fixed + nameW + (ampW > 0.0F ? 4.0F * s + ampW : 0.0F) + durationW);
      }

      if (lines.isEmpty()) {
         this.removeSlot("potions", t.textPx());
         float w = editMode ? 6.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No active effects") : 1.0F;
         return new HudRenderer.PotionPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         float w = this.slot("potions", t.textPx(), (float)Math.ceil(target), dt);
         return new HudRenderer.PotionPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull * lines.size()));
      }
   }

   private void drawPotions(ImDrawList dl, HudLayoutStore.HudElementState state, HudRenderer.PotionPlan plan, boolean shadow, HudRenderer.DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         HudRenderer.Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 6.0F * s;
         int count = Math.max(1, plan.lines().size());
         List<HudRenderer.Step> steps = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            steps.add(
               new HudRenderer.Step(plan.lines().isEmpty() ? -1 : i, x, (float)Math.round(y + rowFull * i), x + w, (float)Math.round(y + rowFull * (i + 1)), a)
            );
         }

         this.drawRows(dl, steps, fill(state), x, y, w, h, rowsRadius(s), s, shadow);
         if (plan.lines().isEmpty()) {
            HudChrome.text(
               dl,
               t.label(),
               t.labelPx(),
               x + pad,
               y + (float)Math.floor((rowFull - t.labelPx()) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
               "No active effects",
               true
            );
         } else {
            float icon = Math.min(iconSize(s), rowFull - 2.0F);
            float textInset = (float)Math.floor((rowFull - t.textPx()) * 0.5F);

            for (HudRenderer.Step q : steps) {
               HudRenderer.PotionLine line = plan.lines().get(q.item());
               HudRenderer.PotionRow row = line.row();
               float ix = Math.round(q.l() + pad);
               float iy = Math.round(q.t() + (rowFull - icon) * 0.5F);
               float iconAlpha = a;
               if (!row.infinite() && row.ticks() <= 200) {
                  iconAlpha = a * (0.55F + 0.45F * Math.abs((float)Math.cos(row.ticks() * Math.PI / 10.0)));
               }

               ThemeImages.Texture texture = GameIcons.effect(row.effect());
               if (texture != null) {
                  dl.addImage(texture.id(), ix, iy, ix + icon, iy + icon, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, iconAlpha));
               } else {
                  int color = HudChrome.abgr(0xFF000000 | ((MobEffect)row.effect().value()).getColor());
                  dl.addCircleFilled(ix + icon * 0.5F, iy + icon * 0.5F, icon * 0.3F, HudChrome.scaleAlpha(color, iconAlpha), 16);
               }

               float ty = q.t() + textInset;
               float tx = ix + icon + 6.0F * s;
               HudChrome.text(dl, t.text(), t.textPx(), tx, ty, HudChrome.scaleAlpha(-1, a), row.name(), true);
               if (line.ampW() > 0.0F) {
                  HudChrome.text(
                     dl, t.text(), t.textPx(), tx + line.nameW() + 4.0F * s, ty, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), row.amplifier(), true
                  );
               }

               float dx = q.r() - pad - line.durationW();
               if (row.infinite()) {
                  HudChrome.text(dl, t.text(), t.textPx(), dx, ty, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "∞", true);
               } else {
                  int color = row.expiringSoon() ? HudChrome.WARN : HudChrome.TEXT_LABEL;
                  HudChrome.tabular(dl, t.text(), t.textPx(), dx, ty, HudChrome.scaleAlpha(color, a), row.duration(), true);
               }
            }
         }
      }
   }


   private HudRenderer.HotkeyPlan planHotkeys(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      HudRenderer.Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      List<HudRenderer.HotkeyLine> lines = !editMode && !HotkeysModule.shouldShowHud() ? List.of() : this.hotkeyLinesFor(t);
      if (lines.isEmpty()) {
         this.removeSlot("hotkeys", t.textPx());
         float w = editMode ? 8.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No keys bound") : 1.0F;
         return new HudRenderer.HotkeyPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         float rows = this.hotkeyNameColumn + 16.0F * s + keycapWidth(this.hotkeyKeyColumn, s);
         float target = 8.0F * s * 2.0F + Math.max(this.hotkeyHeaderW, rows);
         float w = this.slot("hotkeys", t.textPx(), (float)Math.ceil(target), dt);
         return new HudRenderer.HotkeyPlan(lines, t, (float)Math.round(w), (float)Math.round(20.0F * s + rowFull * lines.size()));
      }
   }

   private List<HudRenderer.HotkeyLine> hotkeyLinesFor(HudRenderer.Type t) {
      long revision = hotkeySignature();
      if (revision == this.hotkeyRevision && t.text() == this.hotkeyFont && t.textPx() == this.hotkeyTextPx && t.labelPx() == this.hotkeyKeyPx) {
         return this.hotkeyLines;
      } else {
         this.hotkeyRevision = revision;
         this.hotkeyFont = t.text();
         this.hotkeyTextPx = t.textPx();
         this.hotkeyKeyPx = t.labelPx();
         List<HudRenderer.HotkeyLine> lines = new ArrayList<>();
         float nameColumn = 0.0F;
         float keyColumn = 0.0F;

         for (Module module : Modules.all()) {
            int key = module.settings().bind;
            if (key > 0) {
               String label = keyName(key);
               float nameW = HudChrome.width(t.text(), t.textPx(), module.name());
               float keyW = HudChrome.width(t.text(), t.labelPx(), label);
               lines.add(new HudRenderer.HotkeyLine(module.name(), label, module.isEnabled(), nameW, keyW));
               nameColumn = Math.max(nameColumn, nameW);
               keyColumn = Math.max(keyColumn, keyW);
            }
         }

         this.hotkeyNameColumn = nameColumn;
         this.hotkeyKeyColumn = keyColumn;
         this.hotkeyHeaderW = HudChrome.width(t.label(), t.labelPx(), "HOTKEYS");
         this.hotkeyCapMid = (this.capTop(t.text(), t.labelPx()) + HudChrome.ascent(t.text(), t.labelPx())) * 0.5F;
         this.hotkeyLines = List.copyOf(lines);
         return this.hotkeyLines;
      }
   }

   private void drawHotkeys(ImDrawList dl, HudLayoutStore.HudElementState state, HudRenderer.HotkeyPlan plan, boolean shadow, HudRenderer.DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = Math.round(state.x + tr.xOffset());
         float y = Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         HudRenderer.Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 8.0F * s;
         if (plan.lines().isEmpty()) {
            List<HudRenderer.Step> empty = List.of(new HudRenderer.Step(-1, x, y, x + w, (float)Math.round(y + rowFull), a));
            this.drawRows(dl, empty, fill(state), x, y, w, h, rowsRadius(s), s, shadow);
            HudChrome.text(
               dl,
               t.label(),
               t.labelPx(),
               x + pad,
               y + (float)Math.floor((rowFull - t.labelPx()) * 0.5F),
               HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
               "No keys bound",
               true
            );
         } else {
            float headerH = 20.0F * s;
            List<HudRenderer.Step> steps = new ArrayList<>(plan.lines().size() + 1);
            steps.add(new HudRenderer.Step(-1, x, y, x + w, (float)Math.round(y + headerH), a));

            for (int i = 0; i < plan.lines().size(); i++) {
               steps.add(new HudRenderer.Step(i, x, (float)Math.round(y + headerH + rowFull * i), x + w, (float)Math.round(y + headerH + rowFull * (i + 1)), a));
            }

            this.drawRows(dl, steps, fill(state), x, y, w, h, rowsRadius(s), s, shadow);
            HudRenderer.Step head = steps.get(0);
            float countTop = head.t() + (float)Math.floor((head.b() - head.t() - t.textPx()) * 0.5F);
            float labelTop = countTop + HudChrome.baselineOffset(t.text(), t.textPx(), t.label(), t.labelPx());
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, labelTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "HOTKEYS", true);
            float capW = keycapWidth(this.hotkeyKeyColumn, s);
            float textInset = (float)Math.floor((rowFull - t.textPx()) * 0.5F);

            for (int i = 1; i < steps.size(); i++) {
               HudRenderer.Step q = steps.get(i);
               HudRenderer.HotkeyLine line = plan.lines().get(q.item());
               HudChrome.text(
                  dl, t.text(), t.textPx(), q.l() + pad, q.t() + textInset, HudChrome.scaleAlpha(line.on() ? -1 : HudChrome.TEXT_LABEL, a), line.name(), true
               );
               this.drawKeycap(dl, t, line, q.r() - pad, (q.t() + q.b()) * 0.5F, capW, s, a);
            }
         }
      }
   }

   private void drawKeycap(ImDrawList dl, HudRenderer.Type t, HudRenderer.HotkeyLine line, float right, float centerY, float capW, float s, float a) {
      float capH = keycapHeight(s);
      float l = Math.round(right - capW);
      float top = Math.round(centerY - capH * 0.5F);
      float r = Math.min(hudRounding(), 4.0F) * s;
      dl.addRectFilled(l, top, l + capW, top + capH, HudChrome.scaleAlpha(452984831, a), r);
      dl.addRect(l, top, l + capW, top + capH, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
      HudChrome.text(
         dl,
         t.text(),
         t.labelPx(),
         Math.round(l + (capW - line.keyW()) * 0.5F),
         Math.round(top + capH * 0.5F - this.hotkeyCapMid),
         HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a),
         line.key(),
         true
      );
   }

   private static float keycapHeight(float s) {
      return Math.round(16.0F * s);
   }

   private static float keycapWidth(float keyW, float s) {
      return Math.max(keycapHeight(s), (float)Math.ceil(keyW + 5.0F * s * 2.0F));
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


   private List<HudRenderer.PotionRow> buildPotionRows(Minecraft client) {
      if (client.player == null) {
         return List.of();
      } else {
         List<HudRenderer.PotionRow> rows = new ArrayList<>();

         for (MobEffectInstance instance : client.player.getActiveEffects()) {
            Holder<MobEffect> effect = instance.getEffect();
            String name = I18n.get(((MobEffect)effect.value()).getDescriptionId(), new Object[0]);
            int amp = instance.getAmplifier();
            String amplifier = amp > 0 ? toRomanNumeral(amp + 1) : "";
            int ticks = instance.getDuration();
            if (ticks >= 0 && ticks != Integer.MAX_VALUE && !instance.isInfiniteDuration()) {
               int seconds = Math.max(0, ticks / 20);
               String formatted = seconds / 60 + ":" + String.format("%02d", seconds % 60);
               rows.add(new HudRenderer.PotionRow(effect, name, amplifier, formatted, false, seconds < 30, ticks));
            } else {
               rows.add(new HudRenderer.PotionRow(effect, name, amplifier, "", true, false, Integer.MAX_VALUE));
            }
         }

         return rows;
      }
   }

   private static String toRomanNumeral(int n) {
      return switch (n) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(n);
      };
   }

   private int hudAccent() {
      return HudChrome.abgr(this.hudModule != null ? this.hudModule.accentArgb() : HudModule.themeAccent());
   }

   /** ABGR background of an element: the info bar is a card, everything else uses the lighter row fill. */
   static int fill(HudLayoutStore.HudElementState state) {
      HudModule module = HudModule.get();
      return module != null ? module.fill("info_bar".equals(state.id)) : HudChrome.abgr(0x9916181D);
   }

   static float hudRounding() {
      HudModule module = HudModule.get();
      return module != null ? module.rounding() : 8.0F;
   }

   private void updateEditorInput(float screenW, float screenH) {
      float mouseX = ImGui.getMousePosX();
      float mouseY = ImGui.getMousePosY();
      if (this.draggingId != null) {
         if (ImGui.isMouseDown(0)) {
            HudLayoutStore.HudElementState dragging = this.layout.element(this.draggingId);
            float rawX = mouseX - this.dragOffsetX;
            float rawY = mouseY - this.dragOffsetY;
            HudRenderer.Snap xSnap = bestSnap(rawX, dragging.width, this.snapAnchorsX(this.draggingId, screenW));
            HudRenderer.Snap ySnap = bestSnap(rawY, dragging.height, this.snapAnchorsY(this.draggingId, screenH));
            this.snapGuideX = xSnap != null ? xSnap.guideAt() : null;
            this.snapGuideY = ySnap != null ? ySnap.guideAt() : null;
            this.layout.moveTo(this.draggingId, xSnap != null ? xSnap.position() : rawX, ySnap != null ? ySnap.position() : rawY, screenW, screenH);
         } else {
            this.draggingId = null;
            this.snapGuideX = null;
            this.snapGuideY = null;
            this.saveLayout();
         }
      } else {

         if (!this.overUi && !this.overChip(mouseX, mouseY)) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0.0F) {
               List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();

               for (int i = states.size() - 1; i >= 0; i--) {
                  HudLayoutStore.HudElementState s = states.get(i);
                  if (contains(s, mouseX, mouseY)) {
                     float next = Math.max(0.5F, Math.min(3.0F, s.scale + wheel * 0.05F));
                     if (next != s.scale) {
                        s.scale = next;
                        this.layoutDirty = true;
                        this.lastWheelNanos = System.nanoTime();
                     }
                     break;
                  }
               }
            }

            if (ImGui.isMouseClicked(0)) {
               List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();

               for (int ix = states.size() - 1; ix >= 0; ix--) {
                  HudLayoutStore.HudElementState s = states.get(ix);
                  if (contains(s, mouseX, mouseY)) {
                     this.draggingId = s.id;
                     this.dragOffsetX = mouseX - s.x;
                     this.dragOffsetY = mouseY - s.y;
                     break;
                  }
               }
            }
         }
      }
   }

   private List<Float> snapAnchorsX(String excludeId, float screenW) {
      List<Float> anchors = new ArrayList<>(List.of(0.0F, screenW, screenW * 0.5F));

      for (HudLayoutStore.HudElementState s : this.layout.orderedElements()) {
         if (!s.id.equals(excludeId) && s.positioned) {
            anchors.add(s.x);
            anchors.add(s.x + s.width);
            anchors.add(s.x + s.width * 0.5F);
         }
      }

      return anchors;
   }

   private List<Float> snapAnchorsY(String excludeId, float screenH) {
      List<Float> anchors = new ArrayList<>(List.of(0.0F, screenH, screenH * 0.5F));

      for (HudLayoutStore.HudElementState s : this.layout.orderedElements()) {
         if (!s.id.equals(excludeId) && s.positioned) {
            anchors.add(s.y);
            anchors.add(s.y + s.height);
            anchors.add(s.y + s.height * 0.5F);
         }
      }

      return anchors;
   }

   private static HudRenderer.Snap bestSnap(float raw, float size, List<Float> anchors) {
      float[] offsets = new float[]{0.0F, size * 0.5F, size};
      float bestDelta = 6.0F;
      HudRenderer.Snap best = null;

      for (float anchor : anchors) {
         for (float offset : offsets) {
            float delta = Math.abs(raw + offset - anchor);
            if (delta < bestDelta) {
               bestDelta = delta;
               best = new HudRenderer.Snap(anchor - offset, anchor);
            }
         }
      }

      return best;
   }

   private void drawSnapGuides(ImDrawList dl, float screenW, float screenH) {
      if (this.draggingId != null) {
         int guideColor = HudChrome.scaleAlpha(this.hudAccent(), 0.8F);
         if (this.snapGuideX != null) {
            dl.addLine(this.snapGuideX, 0.0F, this.snapGuideX, screenH, guideColor, 1.0F);
         }

         if (this.snapGuideY != null) {
            dl.addLine(0.0F, this.snapGuideY, screenW, this.snapGuideY, guideColor, 1.0F);
         }
      }
   }

   private void drawEditorChrome(ImDrawList dl, float screenW, float screenH) {
      float mouseX = ImGui.getMousePosX();
      float mouseY = ImGui.getMousePosY();
      float u = ImGuiManager.getInstance().uiScale();
      ImFont font = ImGuiManager.getInstance().hudLabelFont();
      int size = HudChrome.px(14.0F, u);
      this.drawSnapGuides(dl, screenW, screenH);
      float e = Math.round(Math.max(2.0F, 3.0F * u));
      List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();
      List<float[]> placed = new ArrayList<>(states.size());

      for (HudLayoutStore.HudElementState state : states) {
         boolean active = state.id.equals(this.draggingId);
         boolean hovered = contains(state, mouseX, mouseY);
         float emphasis = active ? 1.0F : (hovered ? 0.85F : 0.4F);
         float ox = Math.round(state.x);
         float oy = Math.round(state.y);
         dl.addRect(
            ox - e,
            oy - e,
            ox + state.width + e,
            oy + state.height + e,
            HudChrome.scaleAlpha(this.hudAccent(), emphasis),
            hudRounding() * scaleOf(state) + e,
            0,
            active ? 1.5F : 1.0F
         );
         this.drawEditorLabel(dl, font, size, u, state, active || hovered, states, placed, screenW, screenH);
      }

      this.lastChips = placed;
      drawEditorHint(dl, font, size, u, screenW, screenH);
   }

   private static void drawEditorHint(ImDrawList dl, ImFont font, int size, float u, float screenW, float screenH) {
      String hint = "Drag to move  ·  Scroll to resize  ·  Click the eye to hide";
      float w = Math.round(HudChrome.width(font, size, hint) + 12.0F * u * 2.0F);
      float h = Math.round(28.0F * u);
      float x = Math.round((screenW - w) * 0.5F);
      float y = Math.round(screenH - h - 96.0F * u);
      float r = hudRounding() * u;
      dl.addRectFilled(x, y, x + w, y + h, HudChrome.CHIP_FILL, r);
      dl.addRect(x, y, x + w, y + h, 402653183, r, 0, 1.0F);
      HudChrome.text(dl, font, size, x + 12.0F * u, y + (float)Math.floor((h - size) * 0.5F), HudChrome.TEXT_LABEL, hint, false);
   }

   private void drawEditorLabel(
      ImDrawList dl,
      ImFont font,
      int size,
      float u,
      HudLayoutStore.HudElementState state,
      boolean highlighted,
      List<HudLayoutStore.HudElementState> states,
      List<float[]> placed,
      float screenW,
      float screenH
   ) {
      String label = editorLabel(state.id);
      float a = highlighted ? 1.0F : 0.6F;
      float padX = 8.0F * u;
      float eyeSize = 12.0F * u;
      float chipH = Math.round(20.0F * u);
      float textW = HudChrome.width(font, size, label);
      float chipW = Math.round(padX + textW + 8.0F * u + eyeSize + padX);
      float[] chip = placeChip(state, chipW, chipH, 7.0F * u, states, placed, screenW, screenH);
      placed.add(chip);
      float chipX = chip[0];
      float chipY = chip[1];
      float r = 6.0F * u;
      dl.addRectFilled(chipX, chipY, chipX + chipW, chipY + chipH, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, a), r);
      dl.addRect(chipX, chipY, chipX + chipW, chipY + chipH, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
      int textColor = HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, a);
      HudChrome.text(dl, font, size, chipX + padX, chipY + (float)Math.floor((chipH - size) * 0.5F), textColor, label, false);
      float eyeX = chipX + padX + textW + 8.0F * u;
      float eyeY = chipY + (chipH - eyeSize) * 0.5F;
      float pad = 3.0F * u;
      float mx = ImGui.getMousePosX();
      float my = ImGui.getMousePosY();
      boolean eyeHovered = !this.overUi && this.draggingId == null && mx >= eyeX - pad && mx <= eyeX + eyeSize + pad && my >= eyeY - pad && my <= eyeY + eyeSize + pad;
      if (eyeHovered && ImGui.isMouseClicked(0)) {
         state.visible = !state.visible;
         this.saveLayout();
      }
      float ecx = eyeX + eyeSize * 0.5F;
      float ecy = eyeY + eyeSize * 0.5F;
      float er = eyeSize * 0.34F;
      int eyeColor = eyeHovered ? HudChrome.TEXT_VALUE : HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, state.visible ? a : a * 0.6F);
      float et = Math.max(1.0F, 1.5F * u);
      dl.addBezierQuadratic(ecx - er, ecy, ecx, ecy - er * 1.15F, ecx + er, ecy, eyeColor, et, 16);
      dl.addBezierQuadratic(ecx - er, ecy, ecx, ecy + er * 1.15F, ecx + er, ecy, eyeColor, et, 16);
      dl.addCircleFilled(ecx, ecy, er * 0.3F, eyeColor, 16);
      if (!state.visible) {
         dl.addLine(ecx - er, ecy - er, ecx + er, ecy + er, eyeColor, Math.max(1.0F, 1.4F * u));
      }
   }

   private static float[] placeChip(
      HudLayoutStore.HudElementState state,
      float w,
      float h,
      float gap,
      List<HudLayoutStore.HudElementState> states,
      List<float[]> placed,
      float screenW,
      float screenH
   ) {
      float inset = 4.0F;
      float left = state.x;
      float right = state.x + state.width - w;
      float alignX = alignsRight(state, screenW) ? right : left;
      float midY = state.y + (state.height - h) * 0.5F;
      float[][] candidates = new float[][]{
         {alignX, state.y - gap - h},
         {alignX, state.y + state.height + gap},
         {state.x + state.width + gap, Math.min(midY, state.y)},
         {state.x - gap - w, Math.min(midY, state.y)}
      };

      for (float[] c : candidates) {
         float x = Math.round(c[0]);
         float y = Math.round(c[1]);
         if (!(x < inset) && !(y < inset) && !(x + w > screenW - inset) && !(y + h > screenH - inset) && !overlapsAny(x, y, w, h, state, states, placed, gap)) {
            return new float[]{x, y, w, h};
         }
      }

      float x = Math.round(Math.max(inset, Math.min(alignX, screenW - inset - w)));
      float y = candidates[0][1] >= inset ? candidates[0][1] : candidates[1][1];
      return new float[]{x, Math.round(Math.max(inset, Math.min(y, screenH - inset - h))), w, h};
   }

   private static boolean overlapsAny(
      float x, float y, float w, float h, HudLayoutStore.HudElementState self, List<HudLayoutStore.HudElementState> states, List<float[]> placed, float margin
   ) {
      for (HudLayoutStore.HudElementState other : states) {
         if (other != self
            && other.positioned
            && overlaps(x, y, w, h, other.x - margin * 0.5F, other.y - margin * 0.5F, other.width + margin, other.height + margin)) {
            return true;
         }
      }

      for (float[] chip : placed) {
         if (overlaps(x, y, w, h, chip[0], chip[1], chip[2], chip[3])) {
            return true;
         }
      }

      return false;
   }

   private static boolean overlaps(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
      return ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
   }

   private static boolean contains(HudLayoutStore.HudElementState state, float x, float y) {
      return !state.positioned ? false : x >= state.x && x <= state.x + state.width && y >= state.y && y <= state.y + state.height;
   }

   static String editorLabel(String id) {
      return switch (id) {
         case "info_bar" -> "Info Bar";
         case "coordinates" -> "Coordinates";
         case "module_list" -> "Module List";
         case "potion_tracker" -> "Potions";
         case "hotkeys" -> "Hotkeys";
         case "armor_status" -> "Armor HUD";
         case "item_cooldowns" -> "Cooldown";
         default -> id;
      };
   }

   private static int readTabLatency(Minecraft client, ClientPacketListener connection) {
      PlayerInfo info = connection.getPlayerInfo(connection.getLocalGameProfile().id());
      if (info == null) {
         info = connection.getPlayerInfo(client.player.getUUID());
      }

      if (info == null) {
         info = connection.getPlayerInfoIgnoreCase(connection.getLocalGameProfile().name());
      }

      if (info == null) {
         info = connection.getPlayerInfoIgnoreCase(client.player.getName().getString());
      }

      return info == null ? 0 : Math.max(0, info.getLatency());
   }

   private static int readServerPing(ClientPacketListener connection) {
      ServerData serverData = connection.getServerData();
      return serverData != null && serverData.ping > 0L && serverData.ping <= 60000L ? (int)serverData.ping : 0;
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

   private static float elementScale(HudLayoutStore.HudElementState state) {
      return !Float.isNaN(state.scale) && !Float.isInfinite(state.scale) ? Math.max(0.5F, Math.min(3.0F, state.scale)) : 1.0F;
   }

   static float scaleOf(HudLayoutStore.HudElementState state) {
      return elementScale(state) * ImGuiManager.getInstance().uiScale();
   }


   private record CoordPlan(List<HudRenderer.CoordRow> rows, HudRenderer.Type type, float[] slotW, float width, float height) {
   }

   private record CoordRow(String block, boolean here, boolean enabled, String[] values, float anim) {
   }

   private record DrawTransform(float xOffset, float yOffset, float alpha) {
      private static HudRenderer.DrawTransform identity() {
         return new HudRenderer.DrawTransform(0.0F, 0.0F, 1.0F);
      }

      private HudRenderer.DrawTransform down(float dy) {
         return dy == 0.0F ? this : new HudRenderer.DrawTransform(this.xOffset, this.yOffset + dy, this.alpha);
      }
   }

   private record HotkeyLine(String name, String key, boolean on, float nameW, float keyW) {
   }

   private record HotkeyPlan(List<HudRenderer.HotkeyLine> lines, HudRenderer.Type type, float width, float height) {
   }

   private record InfoItem(String slot, String value, String dim, String unit) {
   }

   private record InfoPlan(List<HudRenderer.Segment> segments, HudRenderer.Type type, float width, float height, float emblemW) {
   }

   private record ListPlan(List<ModuleListRows.Row> rows, HudRenderer.Type type, float width, float height) {
   }

   private record PotionLine(HudRenderer.PotionRow row, float nameW, float ampW, float durationW) {
   }

   private record PotionPlan(List<HudRenderer.PotionLine> lines, HudRenderer.Type type, float width, float height) {
   }

   private record PotionRow(Holder<MobEffect> effect, String name, String amplifier, String duration, boolean infinite, boolean expiringSoon, int ticks) {
   }

   private record Segment(HudRenderer.InfoItem item, float valueW, float dimW, float contentW, float slotW) {
   }

   private record Snap(float position, float guideAt) {
   }

   private record Step(int item, float l, float t, float r, float b, float alpha) {
   }

   private record Type(ImFont value, int valuePx, ImFont text, int textPx, ImFont label, int labelPx) {
      static HudRenderer.Type of(float scale) {
         ImGuiManager m = ImGuiManager.getInstance();
         return new HudRenderer.Type(
            m.hudValueFont(), HudChrome.px(18.0F, scale), m.hudTextFont(), HudChrome.px(17.0F, scale), m.hudLabelFont(), HudChrome.px(14.0F, scale)
         );
      }
   }
}
