package dev.venti.modules.streamhider;

import com.sun.jna.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/** Swaps exclusive fullscreen for a borderless window while Stream Only wants it, so the overlay windows stay visible. */
public final class BorderlessFullscreen {
   private BorderlessFullscreen() {
   }

   public static boolean wanted() {
      return Platform.isWindows() && StreamHiderModule.wantsBorderless();
   }

   public static void tick() {
      MinecraftClient mc = MinecraftClient.getInstance();
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
