package dev.venti.modules.streamhider;

import com.mojang.blaze3d.platform.Window;
import com.sun.jna.Platform;
import net.minecraft.client.Minecraft;

/** Swaps exclusive fullscreen for a borderless window while Stream Only wants it, so the overlay windows stay visible. */
public final class BorderlessFullscreen {
   private BorderlessFullscreen() {
   }

   public static boolean wanted() {
      return Platform.isWindows() && StreamHiderModule.wantsBorderless();
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return;
      }

      //? if >=26.3 {
      /*// 26.3 already makes fullscreen a borderless window on Windows unless exclusive fullscreen is chosen;
      // the mixin only overrides that choice while Stream Only wants it.
      Window window = mc.getWindow();
      if (window != null) {
         ((Switchable)(Object)window).venti$sync(wanted());
      }
      *///?} else {
      Window window = mc.getWindow();
      if (window != null && window.isFullscreen()) {
         Switchable switchable = (Switchable)(Object)window;
         if (wanted() != switchable.venti$isBorderless()) {
            switchable.venti$reapplyMode();
         }
      }
      //?}
   }

   /** Implemented on {@link Window} by {@code dev.venti.mixin.streamhider.BorderlessWindowMixin}. */
   public interface Switchable {
      boolean venti$isBorderless();

      void venti$reapplyMode();
      //? if >=26.3 {

      /*// 26.3: reapplies the window mode when the wanted borderless override changed.
      void venti$sync(boolean wanted);
      *///?}
   }
}
