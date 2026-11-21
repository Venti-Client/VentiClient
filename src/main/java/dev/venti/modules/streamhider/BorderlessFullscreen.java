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

      Window window = mc.getWindow();
      if (window != null && window.isFullscreen()) {
         Switchable switchable = (Switchable)(Object)window;
         if (wanted() != switchable.venti$isBorderless()) {
            switchable.venti$reapplyMode();
         }
      }
   }

   /** Implemented on {@link Window} by {@code dev.venti.mixin.streamhider.BorderlessWindowMixin}. */
   public interface Switchable {
      boolean venti$isBorderless();

      void venti$reapplyMode();
   }
}
