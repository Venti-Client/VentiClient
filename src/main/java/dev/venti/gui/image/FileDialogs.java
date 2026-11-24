package dev.venti.gui.image;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
//? if >=26.3 {
/*import dev.venti.compat.SdlDialogs;
*///?} else {
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class FileDialogs {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final String PATTERN = "*.json";
   private static final String DESCRIPTION = "Venti profile (*.json)";
   private static volatile boolean open;
   private static volatile Path lastFolder;

   private FileDialogs() {
   }

   static boolean busy() {
      return open;
   }

   static void openProfile(String title, Consumer<Path> picked) {
      //? if >=26.3 {
      /*showSdl(false, null, picked);
      *///?} else {
      show(stack -> TinyFileDialogs.tinyfd_openFileDialog(title, startIn(null), filters(stack), "Venti profile (*.json)", false), picked);
      //?}
   }

   static void saveProfile(String title, String fileName, Consumer<Path> picked) {
      //? if >=26.3 {
      /*showSdl(true, fileName, picked);
      *///?} else {
      show(stack -> TinyFileDialogs.tinyfd_saveFileDialog(title, startIn(fileName), filters(stack), "Venti profile (*.json)"), picked);
      //?}
   }

   //? if >=26.3 {
   /*// 26.3: SDL3's asynchronous file dialog (tinyfd is gone). SDL's simple dialogs take no title.
   private static void showSdl(boolean save, String fileName, Consumer<Path> picked) {
      if (open) {
         return;
      }

      open = true;
      Consumer<String> done = result -> {
         open = false;
         if (result != null && !result.isBlank()) {
            Path path;
            try {
               path = Path.of(result).toAbsolutePath();
            } catch (InvalidPathException e) {
               return;
            }

            if (path.getParent() != null) {
               lastFolder = path.getParent();
            }

            Minecraft.getInstance().execute(() -> picked.accept(path));
         }
      };
      try {
         if (save) {
            SdlDialogs.save(DESCRIPTION, "json", startIn(fileName), done);
         } else {
            SdlDialogs.open(DESCRIPTION, "json", startIn(null), done);
         }
      } catch (Throwable e) {
         open = false;
         LOGGER.warn("[Venti] File dialog failed", e);
      }
   }

   *///?}
   private static PointerBuffer filters(MemoryStack stack) {
      return (PointerBuffer)stack.mallocPointer(1).put(stack.UTF8("*.json")).flip();
   }

   private static String startIn(String fileName) {
      Path folder = lastFolder;
      if (folder == null || !Files.isDirectory(folder)) {
         folder = defaultFolder();
      }

      return fileName == null ? folder + File.separator : folder.resolve(fileName).toString();
   }

   private static Path defaultFolder() {
      try {
         Path home = Path.of(System.getProperty("user.home", "."));
         Path downloads = home.resolve("Downloads");
         return Files.isDirectory(downloads) ? downloads : home;
      } catch (InvalidPathException var2) {
         return Path.of(".").toAbsolutePath();
      }
   }

   private static void show(Function<MemoryStack, String> dialog, Consumer<Path> picked) {
      if (!open) {
         open = true;
         Thread thread = new Thread(() -> {
            String result = null;

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  result = dialog.apply(stack);
               } catch (Throwable var14) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var15) {
               LOGGER.warn("[Venti] File dialog failed", var15);
            } finally {
               open = false;
            }

            if (result != null && !result.isBlank()) {
               Path path;
               try {
                  path = Path.of(result).toAbsolutePath();
               } catch (InvalidPathException var12) {
                  return;
               }

               if (path.getParent() != null) {
                  lastFolder = path.getParent();
               }

               Minecraft.getInstance().execute(() -> picked.accept(path));
            }
         }, "Venti file dialog");
         thread.setDaemon(true);
         thread.start();
      }
   }
}
