package dev.venti;

import com.mojang.brigadier.Command;
import dev.venti.gui.ClickGuiScreen;
import dev.venti.gui.Theme;
import dev.venti.imgui.ImGuiManager;
import dev.venti.module.Modules;
import dev.venti.modules.character.CharacterModule;
import dev.venti.modules.hand.CustomGlintModule;
import dev.venti.modules.hand.HandAuraModule;
import dev.venti.modules.hand.HandModelModule;
import dev.venti.modules.hand.HandPoseModule;
import dev.venti.modules.hand.SlowSwingModule;
import dev.venti.modules.hand.SwingAnimationModule;
import dev.venti.modules.discord.DiscordPresenceModule;
import dev.venti.modules.hitfx.HitFxModule;
import dev.venti.modules.hud.HotkeysModule;
import dev.venti.modules.hud.HudModule;
import dev.venti.modules.hud.HudRenderer;
import dev.venti.modules.hud.LowHealthWarningModule;
import dev.venti.modules.hud.WeatherNotifierModule;
import dev.venti.modules.noinvleak.NoInvLeakModule;
import dev.venti.modules.sky.CustomSkyModule;
import dev.venti.modules.totem.CustomTotemModule;
import dev.venti.modules.view.CoordSnapperModule;
import dev.venti.modules.view.CustomCrosshairModule;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.FreelookModule;
import dev.venti.modules.view.FullbrightModule;
import dev.venti.modules.view.NameProtectModule;
import dev.venti.modules.view.NametagsModule;
import dev.venti.modules.view.NoRenderModule;
import dev.venti.modules.view.SkinChangeModule;
import dev.venti.modules.view.SprintModule;
import dev.venti.modules.view.ZoomModule;
import dev.venti.modules.world.AtmosphereModule;
import dev.venti.modules.world.BlockOverlayModule;
import dev.venti.modules.world.ColorGradingModule;
import dev.venti.modules.world.FloatingRelicsModule;
import dev.venti.modules.world.JumpCirclesModule;
import dev.venti.modules.world.MotionBlurModule;
import dev.venti.modules.world.RelicParticlesModule;
import dev.venti.modules.world.TimeChangerModule;
import dev.venti.modules.world.TotemSpiritModule;
import dev.venti.modules.waypoints.WaypointsModule;
import dev.venti.modules.spotify.SpotifyHud;
import dev.venti.modules.spotify.hud.SpotifyHudRenderer;
import dev.venti.modules.spotify.spotify.SpotifyWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class VentiClient implements ClientModInitializer {
   private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("venti", "keys"));
   private KeyBinding openGui;
   private KeyBinding playPause;
   private KeyBinding next;
   private KeyBinding previous;
   private boolean openRequested;
   /** Dev runs pass -Dventi.openGui=true to open the GUI on the title screen once, for screenshots. */
   private boolean devOpenPending = Boolean.getBoolean("venti.openGui");

   @Override
   public void onInitializeClient() {
      Modules.register(new Theme());
      Modules.register(new SpotifyHud());
      Modules.register(new CharacterModule());
      Modules.register(new HitFxModule());
      Modules.register(new DiscordPresenceModule());
      Modules.register(new CustomSkyModule());
      Modules.register(new CustomTotemModule()).prepare();
      Modules.register(new HandModelModule());
      Modules.register(new HandAuraModule());
      Modules.register(new HandPoseModule());
      Modules.register(new SwingAnimationModule());
      Modules.register(new SlowSwingModule());
      Modules.register(new CustomGlintModule());
      Modules.register(new AtmosphereModule());
      Modules.register(new ColorGradingModule());
      Modules.register(new MotionBlurModule());
      Modules.register(new BlockOverlayModule());
      Modules.register(new JumpCirclesModule());
      Modules.register(new FloatingRelicsModule());
      Modules.register(new RelicParticlesModule());
      Modules.register(new TotemSpiritModule());
      Modules.register(new TimeChangerModule());
      Modules.register(new NoInvLeakModule());
      Modules.register(new HudModule());
      Modules.register(new HotkeysModule());
      Modules.register(new LowHealthWarningModule());
      Modules.register(new WeatherNotifierModule());
      Modules.register(new ZoomModule());
      Modules.register(new CustomFovModule());
      Modules.register(new FreelookModule());
      Modules.register(new FullbrightModule());
      Modules.register(new NoRenderModule());
      Modules.register(new CustomCrosshairModule());
      Modules.register(new NametagsModule());
      Modules.register(new SprintModule());
      Modules.register(new CoordSnapperModule());
      Modules.register(new NameProtectModule());
      Modules.register(new SkinChangeModule());
      Modules.register(new WaypointsModule());
      // VENTI-MODULES: further modules are registered here.
      this.openGui = register("key.venti.gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
      this.playPause = register("key.venti.play_pause", GLFW.GLFW_KEY_UNKNOWN);
      this.next = register("key.venti.next", GLFW.GLFW_KEY_UNKNOWN);
      this.previous = register("key.venti.previous", GLFW.GLFW_KEY_UNKNOWN);
      SpotifyHudRenderer spotify = SpotifyHudRenderer.getInstance();
      ImGuiManager.getInstance().addHudRenderer(LowHealthWarningModule::renderOverlay, LowHealthWarningModule::hasOverlayWork);
      ImGuiManager.getInstance().addHudRenderer(HudRenderer.getInstance()::render, HudRenderer.getInstance()::hasWork);
      ImGuiManager.getInstance().addHudRenderer(() -> spotify.render(ClickGuiScreen.editingHud()), () -> spotify.hasWork(ClickGuiScreen.editingHud()));
      ClientLifecycleEvents.CLIENT_STARTED.register(client -> Modules.startAll());
      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Modules.stopAll());
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
      // Opening a screen from inside a chat command gets undone when chat closes, so the command only asks for it.
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
         ClientCommandManager.literal("venti").executes(context -> {
            this.openRequested = true;
            return Command.SINGLE_SUCCESS;
         })
      ));
   }

   private static KeyBinding register(String name, int key) {
      return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, CATEGORY));
   }

   private void tick(MinecraftClient client) {
      while (this.openGui.wasPressed()) {
         this.openRequested = true;
      }

      if (this.devOpenPending && client.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen) {
         this.devOpenPending = false;
         ImGuiManager.getInstance().requestGlyphs("가나다 あいう 漢字");
         client.setScreen(new ClickGuiScreen());
      }

      if (this.openRequested && client.currentScreen == null) {
         this.openRequested = false;
         client.setScreen(new ClickGuiScreen());
      }

      press(this.playPause, SpotifyWindow.MediaKey.PLAY_PAUSE);
      press(this.next, SpotifyWindow.MediaKey.NEXT);
      press(this.previous, SpotifyWindow.MediaKey.PREVIOUS);
      Modules.tick(client);
      CustomTotemModule.get().tickReload();
   }

   private static void press(KeyBinding binding, SpotifyWindow.MediaKey key) {
      while (binding.wasPressed()) {
         SpotifyHud.hudButton(key);
      }
   }
}
