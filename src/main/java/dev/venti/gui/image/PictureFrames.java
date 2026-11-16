package dev.venti.gui.image;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Decodes a picked picture into ARGB frames on the CPU, for textures that are not ImGui ones (sky, totem). */
public final class PictureFrames {
   private PictureFrames() {
   }

   public record Frame(int[] argb, int width, int height, int delayMs) {
   }

   /** Loads a file from the Venti images folder; a still image is one frame. Never larger than maxSide on either edge. */
   public static List<Frame> load(String fileName, int maxSide, int maxFrames) throws IOException {
      Path path = ThemeImages.dir().resolve(fileName);
      byte[] bytes = Files.readAllBytes(path);
      if (ThemeImages.isGif(fileName)) {
         GifDecoder gif = GifDecoder.open(bytes);
         int[] size = fit(gif.width(), gif.height(), maxSide);
         List<Frame> frames = new ArrayList<>();
         gif.decode(size[0], size[1], maxFrames, frame -> {
            frames.add(new Frame(frame.argb(), frame.width(), frame.height(), Math.max(20, frame.delayMs())));
            return true;
         });
         if (frames.isEmpty()) {
            throw new IOException(fileName + " has no frames");
         } else {
            return frames;
         }
      } else {
         return List.of(still(bytes, fileName, maxSide));
      }
   }

   private static Frame still(byte[] bytes, String name, int maxSide) throws IOException {
      ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         encoded.put(bytes).flip();
         IntBuffer w = stack.mallocInt(1);
         IntBuffer h = stack.mallocInt(1);
         IntBuffer channels = stack.mallocInt(1);
         ByteBuffer rgba = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);
         if (rgba == null) {
            throw new IOException("Can't decode " + name + ": " + STBImage.stbi_failure_reason());
         }

         try {
            int width = w.get(0);
            int height = h.get(0);
            int[] argb = new int[width * height];
            for (int i = 0; i < argb.length; i++) {
               int r = rgba.get(i * 4) & 0xFF;
               int g = rgba.get(i * 4 + 1) & 0xFF;
               int b = rgba.get(i * 4 + 2) & 0xFF;
               int a = rgba.get(i * 4 + 3) & 0xFF;
               argb[i] = a << 24 | r << 16 | g << 8 | b;
            }

            int[] size = fit(width, height, maxSide);
            if (size[0] != width || size[1] != height) {
               argb = GifDecoder.downscale(argb, width, height, size[0], size[1]);
            }

            return new Frame(argb, size[0], size[1], 0);
         } finally {
            STBImage.stbi_image_free(rgba);
         }
      } finally {
         MemoryUtil.memFree(encoded);
      }
   }

   private static int[] fit(int width, int height, int maxSide) {
      float scale = Math.min(1.0F, (float)maxSide / Math.max(width, height));
      return new int[]{Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale))};
   }
}
