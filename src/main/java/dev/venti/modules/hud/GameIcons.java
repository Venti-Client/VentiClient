package dev.venti.modules.hud;

import dev.venti.gui.image.ThemeImages;
import imgui.ImDrawList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Vanilla sprites (mob effects, blocks, items) loaded from the active resource packs as ImGui textures.
 * Anubis renders items into a GPU atlas through the item renderer; Venti draws the flat inventory sprite instead.
 */
final class GameIcons {
   private static final Map<Identifier, ThemeImages.Texture> TEXTURES = new HashMap<>();
   private static final Set<Identifier> MISSING = new HashSet<>();
   private static final Map<Item, ThemeImages.Texture> ITEMS = new HashMap<>();
   private static final Set<Item> NO_SPRITE = new HashSet<>();
   private static final Map<String, String> ITEM_ALIASES = Map.of(
      "crossbow", "crossbow_standby",
      "clock", "clock_00",
      "compass", "compass_00",
      "recovery_compass", "recovery_compass_00",
      "light", "light_15"
   );

   private GameIcons() {
   }

   static ThemeImages.Texture effect(Holder<MobEffect> effect) {
      Identifier id = effect.unwrapKey().<Identifier>map(ResourceKey::identifier).orElse(null);
      return id == null ? null : texture(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png"));
   }

   static ThemeImages.Texture block(String name) {
      return texture(Identifier.withDefaultNamespace("textures/block/" + name + ".png"));
   }

   static ThemeImages.Texture item(Item item) {
      if (item == null || NO_SPRITE.contains(item)) {
         return null;
      }

      ThemeImages.Texture cached = ITEMS.get(item);
      if (cached != null) {
         return cached;
      }

      Identifier id = BuiltInRegistries.ITEM.getKey(item);
      String path = ITEM_ALIASES.getOrDefault(id.getPath(), id.getPath());
      ThemeImages.Texture texture = texture(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + path + ".png"));
      if (texture == null) {
         texture = texture(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/block/" + path + ".png"));
      }

      if (texture == null) {
         NO_SPRITE.add(item);
      } else {
         ITEMS.put(item, texture);
      }

      return texture;
   }

   /** Icons are square; snaps to whole multiples of 16 px above 16 so pixel art stays crisp (ItemIcons.pixels in Anubis). */
   static float pixels(float size) {
      float snapped = size < 16.0F ? Math.round(size) : 16.0F * (float)Math.floor(size / 16.0F + 0.25F);
      return Math.max(8.0F, Math.min(96.0F, snapped));
   }

   static void draw(ImDrawList dl, Item item, float x, float y, float size, float alpha) {
      if (item != null && !(alpha <= 0.004F)) {
         float x0 = Math.round(x);
         float y0 = Math.round(y);
         float px = pixels(size);
         ThemeImages.Texture texture = item(item);
         if (texture == null) {
            float r = Math.max(1.0F, px * 0.18F);
            dl.addRectFilled(x0 + 1.0F, y0 + 1.0F, x0 + px - 1.0F, y0 + px - 1.0F, HudChrome.scaleAlpha(HudChrome.TRACK, alpha), r);
         } else {
            // Animated sprites are vertical strips; show the first frame.
            float v1 = texture.height() > texture.width() ? (float)texture.width() / texture.height() : 1.0F;
            dl.addImage(texture.id(), x0, y0, x0 + px, y0 + px, 0.0F, 0.0F, 1.0F, v1, HudChrome.scaleAlpha(-1, alpha));
         }
      }
   }

   static void draw(ImDrawList dl, ItemStack stack, float x, float y, float size, float alpha) {
      if (stack != null && !stack.isEmpty()) {
         draw(dl, stack.getItem(), x, y, size, alpha);
      }
   }

   /** Drops cached textures, e.g. after a resource reload. */
   static void clear() {
      for (ThemeImages.Texture texture : TEXTURES.values()) {
         ThemeImages.get().retireTexture(texture);
      }

      TEXTURES.clear();
      MISSING.clear();
      ITEMS.clear();
      NO_SPRITE.clear();
   }

   private static ThemeImages.Texture texture(Identifier path) {
      ThemeImages.Texture texture = TEXTURES.get(path);
      if (texture == null && !MISSING.contains(path)) {
         try (InputStream in = Minecraft.getInstance().getResourceManager().open(path)) {
            texture = decode(in.readAllBytes());
         } catch (RuntimeException | IOException e) {
            texture = null;
         }

         if (texture == null) {
            MISSING.add(path);
         } else {
            TEXTURES.put(path, texture);
         }
      }

      return texture;
   }

   private static ThemeImages.Texture decode(byte[] bytes) {
      ByteBuffer file = MemoryUtil.memAlloc(bytes.length);
      ByteBuffer pixels = null;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         file.put(bytes).flip();
         IntBuffer w = stack.mallocInt(1);
         IntBuffer h = stack.mallocInt(1);
         IntBuffer channels = stack.mallocInt(1);
         pixels = STBImage.stbi_load_from_memory(file, w, h, channels, 4);
         if (pixels == null || w.get(0) <= 0 || h.get(0) <= 0 || w.get(0) > 1024 || h.get(0) > 8192) {
            return null;
         }

         return ThemeImages.fromPixels(pixels, w.get(0), h.get(0), true);
      } finally {
         if (pixels != null) {
            STBImage.stbi_image_free(pixels);
         }

         MemoryUtil.memFree(file);
      }
   }
}
