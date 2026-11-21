package dev.venti;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import dev.venti.compat.Fapi;
import dev.venti.compat.Mc;
import dev.venti.gui.ClickGuiScreen;
import dev.venti.gui.Theme;
import dev.venti.imgui.ImGuiManager;
import dev.venti.module.Modules;
import dev.venti.modules.character.AvatarModule;
import dev.venti.modules.discord.DiscordPresenceModule;
import dev.venti.modules.emoji.EmojiModule;
import dev.venti.modules.hand.CustomGlintModule;
import dev.venti.modules.hand.HandGlowModule;
import dev.venti.modules.hand.SwingAnimationModule;
import dev.venti.modules.hand.SwingSpeedModule;
import dev.venti.modules.hand.ViewmodelArmModule;
import dev.venti.modules.hand.ViewmodelModule;
import dev.venti.modules.hitfx.HitFxModule;
import dev.venti.modules.hud.FpsCounterModule;
import dev.venti.modules.hud.HotkeysModule;
import dev.venti.modules.hud.HudModule;
import dev.venti.modules.hud.HudRenderer;
import dev.venti.modules.hud.LowHealthWarningModule;
import dev.venti.modules.hud.WeatherNotifierModule;
import dev.venti.modules.sky.CustomSkyModule;
import dev.venti.modules.spotify.SpotifyHud;
import dev.venti.modules.spotify.hud.SpotifyHudRenderer;
import dev.venti.modules.spotify.spotify.SpotifyWindow;
import dev.venti.modules.streamhider.StreamHiderModule;
import dev.venti.modules.totem.CustomTotemModule;
import dev.venti.modules.view.CopyCoordsModule;
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
import dev.venti.modules.waypoints.WaypointsModule;
import dev.venti.modules.world.AuroraSkyModule;
import dev.venti.modules.world.BlockOverlayModule;
import dev.venti.modules.world.ColorGradingModule;
import dev.venti.modules.world.EffectParticlesModule;
import dev.venti.modules.world.JumpCirclesModule;
import dev.venti.modules.world.MotionBlurModule;
import dev.venti.modules.world.OrbitOrbsModule;
import dev.venti.modules.world.TimeChangerModule;
import dev.venti.modules.world.TotemGhostModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class VentiClient implements ClientModInitializer {
   private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("venti", "keys"));
   private KeyMapping openGui;
   private KeyMapping playPause;
   private KeyMapping next;
   private KeyMapping previous;
   private boolean openRequested;
   /** Dev runs pass -Dventi.openGui=true to open the GUI on the title screen once, for screenshots. */
   private boolean devOpenPending = Boolean.getBoolean("venti.openGui");

   @Override
   public void onInitializeClient() {
      Modules.register(new Theme());
      Modules.register(new SpotifyHud());
      Modules.register(new AvatarModule());
      Modules.register(new HitFxModule());
      Modules.register(new DiscordPresenceModule());
      Modules.register(new CustomSkyModule());
      Modules.register(new CustomTotemModule()).prepare();
      Modules.register(new ViewmodelArmModule());
      Modules.register(new HandGlowModule());
      Modules.register(new ViewmodelModule());
      Modules.register(new SwingAnimationModule());
      Modules.register(new SwingSpeedModule());
      Modules.register(new CustomGlintModule());
      Modules.register(new AuroraSkyModule());
      Modules.register(new ColorGradingModule());
      Modules.register(new MotionBlurModule());
      Modules.register(new BlockOverlayModule());
      Modules.register(new JumpCirclesModule());
      Modules.register(new OrbitOrbsModule());
      Modules.register(new EffectParticlesModule());
      Modules.register(new TotemGhostModule());
      Modules.register(new TimeChangerModule());
      Modules.register(new StreamHiderModule());
      Modules.register(new HudModule());
      Modules.register(new HotkeysModule());
      Modules.register(new FpsCounterModule());
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
      Modules.register(new CopyCoordsModule());
      Modules.register(new NameProtectModule());
      Modules.register(new SkinChangeModule());
      Modules.register(new WaypointsModule());
      Modules.register(new EmojiModule());
      // VENTI-MODULES: further modules are registered here.
      this.openGui = register("key.venti.gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
      this.playPause = register("key.venti.play_pause", GLFW.GLFW_KEY_UNKNOWN);
      this.next = register("key.venti.next", GLFW.GLFW_KEY_UNKNOWN);
      this.previous = register("key.venti.previous", GLFW.GLFW_KEY_UNKNOWN);
      SpotifyHudRenderer spotify = SpotifyHudRenderer.getInstance();
      ImGuiManager.getInstance().addHudRenderer(LowHealthWarningModule::renderOverlay, LowHealthWarningModule::hasOverlayWork);
      ImGuiManager.getInstance().addHudRenderer(HudRenderer.getInstance()::render, HudRenderer.getInstance()::hasWork);
      FpsCounterModule fps = FpsCounterModule.get();
      ImGuiManager.getInstance().addHudRenderer(() -> fps.render(ClickGuiScreen.editingHud()), () -> fps.hasWork(ClickGuiScreen.editingHud()));
      ImGuiManager.getInstance().addHudRenderer(() -> spotify.render(ClickGuiScreen.editingHud()), () -> spotify.hasWork(ClickGuiScreen.editingHud()));
      //? if >=26.2 {
      dev.venti.compat.LevelHooks.register();
      //?}
      ClientLifecycleEvents.CLIENT_STARTED.register(client -> Modules.startAll());
      ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Modules.stopAll());
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
      // Opening a screen from inside a chat command gets undone when chat closes, so the command only asks for it.
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
         Fapi.literal("venti").executes(context -> {
            this.openRequested = true;
            return Command.SINGLE_SUCCESS;
         })
      ));
   }

   private static KeyMapping register(String name, int key) {
      return Fapi.registerKey(new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY));
   }

   private void tick(Minecraft client) {
      while (this.openGui.consumeClick()) {
         this.openRequested = true;
      }

      if (this.devOpenPending && Mc.screen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
         this.devOpenPending = false;
         ImGuiManager.getInstance().requestGlyphs("가나다 あいう 漢字");
         Mc.setScreen(new ClickGuiScreen());
      }

      if (this.openRequested && Mc.screen() == null) {
         this.openRequested = false;
         if (Mc.openGl()) {
            Mc.setScreen(new ClickGuiScreen());
         } else if (client.player != null) {
            // 26.x on the Vulkan renderer: the ImGui menu needs OpenGL.
            Mc.chatMessage(net.minecraft.network.chat.Component.literal("[Venti] The menu needs the OpenGL renderer (Video Settings > Graphics API)."));
         }
      }

      press(this.playPause, SpotifyWindow.MediaKey.PLAY_PAUSE);
      press(this.next, SpotifyWindow.MediaKey.NEXT);
      press(this.previous, SpotifyWindow.MediaKey.PREVIOUS);
      Modules.tick(client);
      CustomTotemModule.get().tickReload();
   }

   private static void press(KeyMapping binding, SpotifyWindow.MediaKey key) {
      while (binding.consumeClick()) {
         SpotifyHud.hudButton(key);
      }
   }
}
