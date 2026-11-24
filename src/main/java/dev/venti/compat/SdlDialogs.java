//? if >=26.3 {
/*package dev.venti.compat;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

// Native file dialogs on 26.3, through SDL3 (Minecraft 26.3 no longer ships LWJGL's tinyfd).
// The dialogs are asynchronous: call from the render (main) thread; the result arrives on any thread.
@Environment(EnvType.CLIENT)
public final class SdlDialogs {
   private SdlDialogs() {
   }

   // Asks for a file to open. done gets the chosen path, or null when cancelled or failed.
   public static void open(String filterName, String extensions, @Nullable String startIn, Consumer<String> done) {
      show(false, filterName, extensions, startIn, done);
   }

   // Asks where to save a file. done gets the chosen path, or null when cancelled or failed.
   public static void save(String filterName, String extensions, @Nullable String startIn, Consumer<String> done) {
      show(true, filterName, extensions, startIn, done);
   }

   // extensions: semicolon separated, without dots ("png;jpg").
   private static void show(boolean save, String filterName, String extensions, @Nullable String startIn, Consumer<String> done) {
      // SDL may read the filters and the start location until the callback runs, so they live on the heap until then.
      ByteBuffer name = MemoryUtil.memUTF8(filterName);
      ByteBuffer pattern = MemoryUtil.memUTF8(extensions);
      ByteBuffer location = startIn == null ? null : MemoryUtil.memUTF8(startIn);
      SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
      filters.get(0).set(name, pattern);
      SDL_DialogFileCallback[] callback = new SDL_DialogFileCallback[1];
      Runnable free = () -> {
         filters.free();
         MemoryUtil.memFree(name);
         MemoryUtil.memFree(pattern);
         MemoryUtil.memFree(location);
         callback[0].free();
      };
      callback[0] = SDL_DialogFileCallback.create((userdata, fileList, filter) -> {
         String result = null;
         try {
            // fileList is NULL on error and starts with NULL when the dialog was cancelled.
            long first = fileList == 0L ? 0L : MemoryUtil.memGetAddress(fileList);
            result = first == 0L ? null : MemoryUtil.memUTF8(first);
         } finally {
            // Not inside the callback itself: free once it has returned.
            Minecraft.getInstance().execute(free);
         }

         done.accept(result);
      });
      long window = Minecraft.getInstance().getWindow().handle();
      if (save) {
         SDLDialog.SDL_ShowSaveFileDialog(callback[0], 0L, window, filters, location);
      } else {
         SDLDialog.SDL_ShowOpenFileDialog(callback[0], 0L, window, filters, location, false);
      }
   }
}
*///?}
