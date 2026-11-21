package dev.venti.imgui;

import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGuiIO;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryUtil;

/**
 * Hands ImGui its glyph ranges in native memory.
 *
 * imgui-java stores a pointer to the Java {@code short[]} passed as glyph ranges and ImGui only reads it when the
 * atlas is built. A garbage collection in between can move the array, and the build then reads garbage
 * ("src_range[0] <= src_range[1]" assertion, which ends the game). Here the ranges are copied to native memory and
 * that pointer is written into the font config instead; the memory lives until the next atlas build.
 */
final class NativeGlyphRanges {
   private static final long UNKNOWN = -2L;
   private static final long UNAVAILABLE = -1L;
   private static final int SCAN_BYTES = 96;
   private static long offset = UNKNOWN;
   private static final List<Long> LIVE = new ArrayList<>();
   private static final List<Long> RETIRED = new ArrayList<>();

   private NativeGlyphRanges() {
   }

   /** Call before adding the fonts of a new atlas build (after the atlas was cleared). */
   static void beginAtlas() {
      for (long address : RETIRED) {
         MemoryUtil.nmemFree(address);
      }

      RETIRED.clear();
      RETIRED.addAll(LIVE);
      LIVE.clear();
   }

   static ImFont addFont(ImGuiIO io, byte[] data, int sizePixels, ImFontConfig config, short[] ranges) {
      long slot = ranges == null ? UNAVAILABLE : offset();
      if (slot < 0L) {
         return io.getFonts().addFontFromMemoryTTF(data, sizePixels, config, ranges);
      }

      long address = MemoryUtil.nmemAllocChecked(ranges.length * 2L);
      for (int i = 0; i < ranges.length; i++) {
         MemoryUtil.memPutShort(address + i * 2L, ranges[i]);
      }

      LIVE.add(address);
      MemoryUtil.memPutAddress(config.ptr + slot, address);
      return io.getFonts().addFontFromMemoryTTF(data, sizePixels, config);
   }

   /** Where ImFontConfig keeps its GlyphRanges pointer, found by watching which pointer slot setGlyphRanges changes. */
   private static long offset() {
      if (offset == UNKNOWN) {
         offset = UNAVAILABLE;
         ImFontConfig probe = new ImFontConfig();
         try {
            long[] before = snapshot(probe.ptr);
            probe.setGlyphRanges(new short[]{32, 126, 0});
            long[] after = snapshot(probe.ptr);
            int changed = -1;
            for (int i = 0; i < before.length; i++) {
               if (before[i] != after[i]) {
                  if (changed >= 0 || before[i] != 0L) {
                     changed = -2;
                     break;
                  }

                  changed = i;
               }
            }

            if (changed >= 0) {
               offset = changed * 8L;
            } else {
               System.err.println("[Venti] Couldn't locate ImGui glyph range slot; using Java arrays");
            }
         } catch (RuntimeException e) {
            System.err.println("[Venti] Couldn't locate ImGui glyph range slot: " + e.getMessage());
         } finally {
            probe.destroy();
         }
      }

      return offset;
   }

   private static long[] snapshot(long struct) {
      long[] words = new long[SCAN_BYTES / 8];
      for (int i = 0; i < words.length; i++) {
         words[i] = MemoryUtil.memGetLong(struct + i * 8L);
      }

      return words;
   }
}
