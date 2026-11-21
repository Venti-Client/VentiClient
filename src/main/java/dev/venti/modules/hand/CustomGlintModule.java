package dev.venti.modules.hand;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.venti.compat.Mc;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
//?} else {
/*import net.minecraft.client.renderer.entity.ItemRenderer;
*///?}
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

/**
 * Custom Glint: recolours the enchantment glint textures in place and changes the glint's scroll speed and
 * pattern size (ported from Anubis by 4ldenz).
 */
public final class CustomGlintModule extends Module {
   public static volatile CustomGlintModule active;
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private final ModeSetting colorMode = this.add("color-mode", new ModeSetting("Color", COLOR_CUSTOM, COLOR_CUSTOM, COLOR_ACCENT));
   private final ColorSetting itemColor = this.add("item-color", new ColorSetting("Item Color", -9524994));
   private final BooleanSetting separateArmor = this.add("separate-armor", new BooleanSetting("Separate Armor", false));
   private final ColorSetting armorColor = this.add("armor-color", new ColorSetting("Armor Color", -3697942));
   private final NumberSetting intensity = this.add("intensity", new NumberSetting("Intensity", 1.0F, 0.05F, 1.0F).step(0.05F));
   private final NumberSetting speed = this.add("speed", new NumberSetting("Speed", 1.0F, 0.1F, 4.0F).step(0.1F));
   private final NumberSetting patternSize = this.add("pattern-size", new NumberSetting("Pattern Size", 1.0F, 0.25F, 4.0F).step(0.25F));
   //? if >=26.2 {
   private final GlintTexture items = new GlintTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM);
   private final GlintTexture armor = new GlintTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR);
   //?} else {
   /*private final GlintTexture items = new GlintTexture(ItemRenderer.ENCHANTED_GLINT_ITEM);
   private final GlintTexture armor = new GlintTexture(ItemRenderer.ENCHANTED_GLINT_ARMOR);
   *///?}
   private double glintClock = Double.NaN;
   private long glintClockReal;

   public CustomGlintModule() {
      super("custom-glint", "Custom Glint", "Recolours the enchantment glint and changes its speed and pattern size.", ModuleCategory.VISUALS);
      this.itemColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
      this.separateArmor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
      this.armorColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.separateArmor.get());
   }

   @Override
   protected void onEnable() {
      this.glintClock = Double.NaN;
      active = this;
   }

   @Override
   protected void onTick() {
      this.refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      Minecraft mc = Minecraft.getInstance();
      if (mc.isSameThread()) {
         this.restore();
      } else {
         mc.execute(() -> {
            if (active == null) {
               this.restore();
            }
         });
      }
   }

   public long glintMillis(long realMillis) {
      if (Double.isNaN(this.glintClock)) {
         this.glintClock = realMillis;
      } else {
         this.glintClock = this.glintClock + (double)Math.max(0L, realMillis - this.glintClockReal) * this.speed.get();
      }

      this.glintClockReal = realMillis;
      return (long)this.glintClock;
   }

   public float uvScaleMultiplier() {
      return 1.0F / this.patternSize.get();
   }

   private void refresh() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && Mc.overlay() == null && RenderSystem.tryGetDevice() != null) {
         int itemRgb = this.colorMode.is(COLOR_ACCENT) ? HandAccent.argb() & 0xFFFFFF : this.itemColor.argb() & 0xFFFFFF;
         int armorRgb = this.colorMode.is(COLOR_CUSTOM) && this.separateArmor.get() ? this.armorColor.argb() & 0xFFFFFF : itemRgb;
         float strength = this.intensity.get();
         this.items.tint(mc, itemRgb, strength);
         this.armor.tint(mc, armorRgb, strength);
      }
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();
      this.items.restore(mc);
      this.armor.restore(mc);
   }

   private static final class GlintTexture {
      private final Identifier id;
      private int width;
      private int height;
      private int[] original;
      private int[] pattern;
      private GpuTexture written;
      private long writtenKey;
      private GpuTexture unusable;

      GlintTexture(Identifier id) {
         this.id = id;
      }

      void tint(Minecraft mc, int rgb, float intensity) {
         GpuTexture gpu = this.gpuTexture(mc);
         if (gpu == null || gpu.isClosed() || gpu == this.unusable) {
            return;
         }

         long key = (long)(rgb & 0xFFFFFF) << 8 | Math.round(intensity * 255.0F);
         if (gpu == this.written && key == this.writtenKey) {
            return;
         }

         if (gpu != this.written) {
            this.written = null;
            // Usage bit 1 = COPY_DST: the texture must accept writes.
            if ((gpu.usage() & 1) == 0 || !this.readShipped(mc, gpu)) {
               this.unusable = gpu;
               return;
            }
         }

         if (!this.upload(gpu, this.tinted(rgb, intensity))) {
            this.unusable = gpu;
         } else {
            this.written = gpu;
            this.writtenKey = key;
         }
      }

      void restore(Minecraft mc) {
         GpuTexture gpu = this.written != null ? this.gpuTexture(mc) : null;
         if (gpu != null && gpu == this.written && !gpu.isClosed() && this.original != null) {
            this.upload(gpu, this.original);
         }

         this.written = null;
         this.unusable = null;
         this.original = null;
         this.pattern = null;
      }

      private GpuTexture gpuTexture(Minecraft mc) {
         try {
            AbstractTexture texture = mc.getTextureManager().getTexture(this.id);
            return texture == null ? null : texture.getTexture();
         } catch (IllegalStateException e) {
            return null;
         }
      }

      private boolean readShipped(Minecraft mc, GpuTexture gpu) {
         Optional<Resource> resource = mc.getResourceManager().getResource(this.id);
         if (resource.isEmpty()) {
            return false;
         }

         try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w != gpu.getWidth(0) || h != gpu.getHeight(0)) {
               return false;
            }

            int[] pixels = new int[w * h];
            int[] luma = new int[w * h];
            int i = 0;
            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++, i++) {
                  int argb = image.getPixel(x, y);
                  pixels[i] = argb;
                  luma[i] = Math.max(ARGB.red(argb), Math.max(ARGB.green(argb), ARGB.blue(argb)));
               }
            }

            this.width = w;
            this.height = h;
            this.original = pixels;
            this.pattern = luma;
            return true;
         } catch (RuntimeException | IOException e) {
            LOGGER.warn("[Venti] Custom Glint could not read {}", this.id, e);
            return false;
         }
      }

      private int[] tinted(int rgb, float intensity) {
         float r = ARGB.red(rgb) * intensity / 255.0F;
         float g = ARGB.green(rgb) * intensity / 255.0F;
         float b = ARGB.blue(rgb) * intensity / 255.0F;
         int[] out = new int[this.pattern.length];
         for (int i = 0; i < out.length; i++) {
            int l = this.pattern[i];
            out[i] = ARGB.color(ARGB.alpha(this.original[i]), Math.round(l * r), Math.round(l * g), Math.round(l * b));
         }

         return out;
      }

      private boolean upload(GpuTexture gpu, int[] argb) {
         try (NativeImage image = new NativeImage(this.width, this.height, false)) {
            int i = 0;
            for (int y = 0; y < this.height; y++) {
               for (int x = 0; x < this.width; x++, i++) {
                  image.setPixel(x, y, argb[i]);
               }
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, image);
            return true;
         } catch (RuntimeException e) {
            LOGGER.warn("[Venti] Custom Glint could not write {}", this.id, e);
            return false;
         }
      }
   }
}
