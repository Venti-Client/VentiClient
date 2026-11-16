package dev.venti.modules.discord;

import dev.venti.config.ActionSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.StringListSetting;
import dev.venti.config.StringSetting;
import dev.venti.gui.ClickGuiScreen;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.ChatOptionsScreen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.util.Pair;

/**
 * Shows Venti as your Discord Rich Presence.
 * Ported from Meteor Client's {@code DiscordPresence} module (GPL-3.0, MeteorDevelopment), using Meteor's discord-ipc library.
 * Starscript messages are replaced by the simpler {@link Placeholders}.
 */
public final class DiscordPresenceModule extends Module {
   /** Meteor Client's Discord application ID, the default until you point it at your own "Venti" application. */
   private static final String DEFAULT_APP_ID = "835240968533049424";
   private static final String SEQUENTIAL = "Sequential";
   private static final String RANDOM = "Random";
   private static final String SMALL_ALTERNATE = "Alternate";
   private static final String SMALL_FIRST = "First only";
   private static final String SMALL_NONE = "None";
   /** Ticks between small image swaps, as in Meteor. */
   private static final int SMALL_IMAGE_TICKS = 200;
   /** Ticks between reconnect attempts while Discord is not reachable. */
   private static final int RECONNECT_TICKS = 600;
   /** Ticks to wait after the application ID was edited before reconnecting, so typing does not reconnect per key. */
   private static final int APP_ID_SETTLE_TICKS = 40;
   private static final List<Pair<String, String>> CUSTOM_STATES = new ArrayList<>();

   private final StringSetting applicationId;
   private final StringSetting largeImageKey;
   private final StringSetting largeImageText;
   private final ModeSetting smallImageMode;
   private final StringSetting smallImage1Key;
   private final StringSetting smallImage1Text;
   private final StringSetting smallImage2Key;
   private final StringSetting smallImage2Text;
   private final StringListSetting line1Strings;
   private final NumberSetting line1UpdateDelay;
   private final ModeSetting line1SelectMode;
   private final StringListSetting line2Strings;
   private final NumberSetting line2UpdateDelay;
   private final ModeSetting line2SelectMode;

   private final RichPresence rpc = new RichPresence();
   private boolean smallImageSecond;
   private int ticks;
   private boolean forceUpdate;
   private boolean lastWasInMainMenu;
   private int line1Ticks;
   private int line1I;
   private int line2Ticks;
   private int line2I;
   private int reconnectTicks;
   private int restartIn = -1;
   private Screen lastScreen;

   static {
      registerCustomState("com.terraformersmc.modmenu.gui", "Browsing mods");
      registerCustomState("me.jellysquid.mods.sodium.client", "Changing options");
      registerCustomState("net.caffeinemc.mods.sodium.client", "Changing options");
   }

   public DiscordPresenceModule() {
      super("discord_presence", "Discord Presence", "Displays Venti as your presence on Discord.", ModuleCategory.MISC);
      this.applicationId = this.add("application_id", new StringSetting("Application ID", DEFAULT_APP_ID));
      this.applicationId.onChange(() -> this.restartIn = APP_ID_SETTLE_TICKS);
      this.largeImageKey = this.add("large_image_key", new StringSetting("Large Image Key", "venti"));
      this.largeImageKey.onChange(this::refreshImages);
      this.largeImageText = this.add("large_image_text", new StringSetting("Large Image Text", "Venti Client"));
      this.largeImageText.onChange(this::refreshImages);
      this.smallImageMode = this.add("small_image_mode", new ModeSetting("Small Image", SMALL_NONE, SMALL_ALTERNATE, SMALL_FIRST, SMALL_NONE));
      this.smallImageMode.onChange(this::refreshImages);
      this.smallImage1Key = this.add("small_image_1_key", new StringSetting("Small Image 1 Key", ""));
      this.smallImage1Key.visibleWhen(() -> !this.smallImageMode.is(SMALL_NONE)).onChange(this::refreshImages);
      this.smallImage1Text = this.add("small_image_1_text", new StringSetting("Small Image 1 Text", ""));
      this.smallImage1Text.visibleWhen(() -> !this.smallImageMode.is(SMALL_NONE)).onChange(this::refreshImages);
      this.smallImage2Key = this.add("small_image_2_key", new StringSetting("Small Image 2 Key", ""));
      this.smallImage2Key.visibleWhen(() -> this.smallImageMode.is(SMALL_ALTERNATE)).onChange(this::refreshImages);
      this.smallImage2Text = this.add("small_image_2_text", new StringSetting("Small Image 2 Text", ""));
      this.smallImage2Text.visibleWhen(() -> this.smallImageMode.is(SMALL_ALTERNATE)).onChange(this::refreshImages);

      this.line1Strings = this.add("line_1_messages", new StringListSetting("Line 1 Messages", "{player}", "{server}"));
      this.line1Strings.onChange(this::messagesChanged);
      this.line1UpdateDelay = this.add("line_1_update_delay", new NumberSetting("Line 1 Update Delay", 200.0F, 10.0F, 200.0F).step(1.0F));
      this.line1SelectMode = this.add("line_1_select_mode", new ModeSetting("Line 1 Select Mode", SEQUENTIAL, RANDOM, SEQUENTIAL));

      this.line2Strings = this.add(
         "line_2_messages",
         new StringListSetting("Line 2 Messages", "Venti Client", "Playing on {server.difficulty} difficulty.", "{server.player_count} Players online")
      );
      this.line2Strings.onChange(this::messagesChanged);
      this.line2UpdateDelay = this.add("line_2_update_delay", new NumberSetting("Line 2 Update Delay", 60.0F, 10.0F, 200.0F).step(1.0F));
      this.line2SelectMode = this.add("line_2_select_mode", new ModeSetting("Line 2 Select Mode", SEQUENTIAL, RANDOM, SEQUENTIAL));

      this.add("reconnect", new ActionSetting("Reconnect to Discord", () -> this.restartIn = 0));
      this.add("placeholders", new ActionSetting("Copy placeholder list", () -> MinecraftClient.getInstance().keyboard.setClipboard(Placeholders.SUPPORTED)));
   }

   /** Lets other mods name their screens, like Meteor's {@code DiscordPresence.registerCustomState}. */
   public static void registerCustomState(String packageName, String state) {
      for (Pair<String, String> pair : CUSTOM_STATES) {
         if (pair.getLeft().equals(packageName)) {
            pair.setRight(state);
            return;
         }
      }

      CUSTOM_STATES.add(new Pair<>(packageName, state));
   }

   public static void unregisterCustomState(String packageName) {
      CUSTOM_STATES.removeIf(pair -> pair.getLeft().equals(packageName));
   }

   @Override
   protected void onEnable() {
      DiscordIPC.setOnError((code, message) -> LOGGER.warn("[Venti] Discord IPC error {}: {}", code, message));
      this.rpc.setStart(System.currentTimeMillis() / 1000L);
      this.smallImageSecond = true;
      this.ticks = 0;
      this.line1Ticks = 0;
      this.line2Ticks = 0;
      this.line1I = 0;
      this.line2I = 0;
      this.lastWasInMainMenu = false;
      this.lastScreen = null;
      this.restartIn = -1;
      this.connect();
   }

   @Override
   protected void onDisable() {
      DiscordIPC.stop();
   }

   private void connect() {
      DiscordIPC.stop();
      this.reconnectTicks = 0;
      long appId;
      try {
         appId = Long.parseLong(this.applicationId.get().trim());
      } catch (NumberFormatException e) {
         LOGGER.warn("[Venti] Discord Presence: \"{}\" is not a valid application ID", this.applicationId.get());
         return;
      }

      if (DiscordIPC.start(appId, null)) {
         this.applyImages();
         this.forceUpdate = true;
         this.lastWasInMainMenu = false;
      }
   }

   private void messagesChanged() {
      this.forceUpdate = true;
   }

   private void refreshImages() {
      this.applyImages();
      this.forceUpdate = true;
   }

   private void applyImages() {
      String largeKey = this.largeImageKey.get().trim();
      this.rpc.setLargeImage(largeKey.isEmpty() ? null : largeKey, Placeholders.apply(this.largeImageText.get()));
      if (this.smallImageMode.is(SMALL_NONE)) {
         this.rpc.setSmallImage(null, null);
      } else if (this.smallImageMode.is(SMALL_ALTERNATE) && this.smallImageSecond) {
         this.setSmallImage(this.smallImage2Key.get(), this.smallImage2Text.get());
      } else {
         this.setSmallImage(this.smallImage1Key.get(), this.smallImage1Text.get());
      }
   }

   private void setSmallImage(String key, String text) {
      String trimmed = key.trim();
      this.rpc.setSmallImage(trimmed.isEmpty() ? null : trimmed, trimmed.isEmpty() ? null : Placeholders.apply(text));
   }

   @Override
   protected void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (this.restartIn >= 0 && this.restartIn-- == 0) {
         this.connect();
      }

      if (!DiscordIPC.isConnected()) {
         if (++this.reconnectTicks >= RECONNECT_TICKS) {
            this.connect();
         }

         if (!DiscordIPC.isConnected()) {
            return;
         }
      }

      // Meteor resets this from its OpenScreenEvent so main menu screen changes refresh the state line.
      if (mc.currentScreen != this.lastScreen) {
         this.lastScreen = mc.currentScreen;
         if (!canUpdate(mc)) {
            this.lastWasInMainMenu = false;
         }
      }

      boolean update = false;
      if (this.ticks < SMALL_IMAGE_TICKS && !this.forceUpdate) {
         this.ticks++;
      } else {
         this.smallImageSecond = !this.smallImageSecond;
         this.applyImages();
         update = true;
         this.ticks = 0;
      }

      if (canUpdate(mc)) {
         if (this.line1Ticks < this.line1UpdateDelay.get().intValue() && !this.forceUpdate) {
            this.line1Ticks++;
         } else {
            List<String> lines = this.line1Strings.get();
            if (!lines.isEmpty()) {
               if (this.line1I >= lines.size()) {
                  this.line1I = 0;
               }

               int i = this.line1SelectMode.is(SEQUENTIAL) ? this.line1I++ : ThreadLocalRandom.current().nextInt(lines.size());
               this.rpc.setDetails(Placeholders.apply(lines.get(i)));
            }

            update = true;
            this.line1Ticks = 0;
         }

         if (this.line2Ticks < this.line2UpdateDelay.get().intValue() && !this.forceUpdate) {
            this.line2Ticks++;
         } else {
            List<String> lines = this.line2Strings.get();
            if (!lines.isEmpty()) {
               if (this.line2I >= lines.size()) {
                  this.line2I = 0;
               }

               int i = this.line2SelectMode.is(SEQUENTIAL) ? this.line2I++ : ThreadLocalRandom.current().nextInt(lines.size());
               this.rpc.setState(Placeholders.apply(lines.get(i)));
            }

            update = true;
            this.line2Ticks = 0;
         }
      } else if (!this.lastWasInMainMenu) {
         this.rpc.setDetails(Placeholders.apply("Venti Client {version}"));
         this.rpc.setState(menuState(mc.currentScreen));
         update = true;
      }

      if (update) {
         DiscordIPC.setActivity(this.rpc);
      }

      this.forceUpdate = false;
      this.lastWasInMainMenu = !canUpdate(mc);
   }

   private static boolean canUpdate(MinecraftClient mc) {
      return mc.world != null && mc.player != null;
   }

   private static String menuState(Screen screen) {
      if (screen instanceof TitleScreen) {
         return "Looking at title screen";
      } else if (screen instanceof SelectWorldScreen) {
         return "Selecting world";
      } else if (screen instanceof CreateWorldScreen || screen instanceof EditGameRulesScreen) {
         return "Creating world";
      } else if (screen instanceof EditWorldScreen) {
         return "Editing world";
      } else if (screen instanceof LevelLoadingScreen) {
         return "Loading world";
      } else if (screen instanceof MultiplayerScreen) {
         return "Selecting server";
      } else if (screen instanceof AddServerScreen) {
         return "Adding server";
      } else if (screen instanceof ConnectScreen || screen instanceof DirectConnectScreen) {
         return "Connecting to server";
      } else if (screen instanceof ClickGuiScreen) {
         return "Browsing Venti's GUI";
      } else if (screen instanceof OptionsScreen
         || screen instanceof SkinOptionsScreen
         || screen instanceof SoundOptionsScreen
         || screen instanceof VideoOptionsScreen
         || screen instanceof ControlsOptionsScreen
         || screen instanceof LanguageOptionsScreen
         || screen instanceof ChatOptionsScreen
         || screen instanceof PackScreen
         || screen instanceof AccessibilityOptionsScreen) {
         return "Changing options";
      } else if (screen instanceof CreditsScreen) {
         return "Reading credits";
      } else if (screen instanceof RealmsScreen) {
         return "Browsing Realms";
      }

      if (screen != null) {
         String className = screen.getClass().getName();
         for (Pair<String, String> pair : CUSTOM_STATES) {
            if (className.startsWith(pair.getLeft())) {
               return pair.getRight();
            }
         }
      }

      return "In main menu";
   }
}
