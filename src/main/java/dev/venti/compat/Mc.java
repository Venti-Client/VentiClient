package dev.venti.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Version-neutral access to client state that moved between Minecraft versions
 * (26.2 moved screens, toasts and chat into {@code Gui}/{@code Hud}, and render targets into {@code GameRenderer}).
 */
@Environment(EnvType.CLIENT)
public final class Mc {
   private Mc() {
   }

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   @Nullable
   public static Screen screen() {
      //? if >=26.2 {
      return mc().gui == null ? null : mc().gui.screen();
      //?} else {
      /*return mc().screen;
      *///?}
   }

   public static void setScreen(@Nullable Screen screen) {
      //? if >=26.2 {
      mc().gui.setScreen(screen);
      //?} else {
      /*mc().setScreen(screen);
      *///?}
   }

   @Nullable
   public static Overlay overlay() {
      //? if >=26.2 {
      return mc().gui == null ? null : mc().gui.overlay();
      //?} else {
      /*return mc().getOverlay();
      *///?}
   }

   public static ToastManager toasts() {
      //? if >=26.2 {
      return mc().gui.toastManager();
      //?} else {
      /*return mc().getToastManager();
      *///?}
   }

   public static ChatComponent chat() {
      //? if >=26.2 {
      return mc().gui.hud.getChat();
      //?} else {
      /*return mc().gui.getChat();
      *///?}
   }

   /** The level's time of day in ticks (26.x: the overworld clock). */
   public static long dayTime(Level level) {
      //? if >=26.2 {
      return level.getOverworldClockTime();
      //?} else {
      /*return level.getDayTime();
      *///?}
   }

   /** Adds a client-side system line to the chat. */
   public static void chatMessage(Component message) {
      //? if >=26.2 {
      chat().addClientSystemMessage(message);
      //?} else {
      /*chat().addMessage(message);
      *///?}
   }

   /** Whether the HUD is hidden (F1). */
   public static boolean hudHidden() {
      //? if >=26.2 {
      return mc().gui != null && mc().gui.hud.isHidden();
      //?} else {
      /*return mc().options.hideGui;
      *///?}
   }

   public static RenderTarget mainTarget() {
      //? if >=26.2 {
      return mc().gameRenderer.mainRenderTarget();
      //?} else {
      /*return mc().getMainRenderTarget();
      *///?}
   }

   /** Whether the game is rendering a panorama screenshot. */
   public static boolean panoramic() {
      //? if >=26.2 {
      return mc().gameRenderer.mainCamera().isPanoramicMode();
      //?} else {
      /*return mc().gameRenderer.isPanoramicMode();
      *///?}
   }

   /**
    * Whether the game renders with OpenGL. 26.x can also run on Vulkan; the ImGui interface and the Stream Hider
    * overlay draw with OpenGL directly and stay off there.
    */
   public static boolean openGl() {
      //? if >=26.2 {
      return mc().getWindow() != null && mc().getWindow().backend() instanceof com.mojang.blaze3d.opengl.GlBackend;
      //?} else {
      /*return true;
      *///?}
   }

   public static Camera camera() {
      //? if >=26.2 {
      return mc().gameRenderer.mainCamera();
      //?} else {
      /*return mc().gameRenderer.getMainCamera();
      *///?}
   }
}
