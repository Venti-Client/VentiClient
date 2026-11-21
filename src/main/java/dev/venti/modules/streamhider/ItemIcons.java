package dev.venti.modules.streamhider;

import com.mojang.blaze3d.ProjectionType;
import dev.venti.compat.Gpu;
//? if >=26.2 {
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
//?} else {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
*///?}
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.imgui.ImGuiManager;
import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.fog.FogRenderer.FogMode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItemIcons {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final int SHEET_SIDE = 512;
   private static final int MIN_PX = 8;
   private static final int MAX_PX = 96;
   private static final int MAX_SHEETS = 8;
   private static final int BUSY_FRAMES = 2;
   private static final int IDLE_FRAMES = 600;
   private static final int PAINTS_PER_FRAME = 6;
   private static final int CHECK_SIDE = 32;
   static final int AUDIT_FRAMES = 30;
   private static final ItemIcons.Cell BLANK = new ItemIcons.Cell();
   private static final Int2ObjectOpenHashMap<ItemIcons.Sheet> SHEETS = new Int2ObjectOpenHashMap();
   private static final List<ItemIcons.Sheet> RETIRED = new ArrayList<>();
   private static final Reference2ObjectOpenHashMap<Item, ItemStack> PLAIN = new Reference2ObjectOpenHashMap();
   private static final ReferenceOpenHashSet<Item> BROKEN = new ReferenceOpenHashSet();
   private static final ItemLook MODEL = new ItemLook();
   private static final int[] VIEWPORT = new int[4];
   private static GpuTexture depth;
   private static GpuTextureView depthView;
   private static SubmitNodeStorage submits;
   //? if >=26.2 {
   private static ProjectionMatrixBuffer projection;
   private static final Projection ORTHO = new Projection();
   //?} else {
   /*private static CachedOrthoProjectionMatrixBuffer projection;
   private static FeatureRenderDispatcher features;
   private static BufferSource buffers;
   *///?}
   private static GpuBuffer readback;
   private static ByteBuffer straightened;
   private static boolean straightenFailed;
   private static int verdict;
   private static GpuTexture itemAtlas;
   private static int frame = Integer.MIN_VALUE;
   private static int retiredFrame;
   private static int auditedFrame;
   private static int paintsLeft;
   private static boolean disabled;

   private ItemIcons() {
   }

   private static int scaleAlpha(int color, float alphaScale) {
      int alpha = color >>> 24 & 0xFF;
      int scaled = Math.round(alpha * Math.max(0.0F, Math.min(1.0F, alphaScale)));
      return color & 16777215 | scaled << 24;
   }

   static float pixels(float size) {
      float snapped = size < 16.0F ? Math.round(size) : 16.0F * (float)Math.floor(size / 16.0F + 0.25F);
      return Math.max(8.0F, Math.min(96.0F, snapped));
   }

   public static void draw(ImDrawList dl, ItemStack stack, float x, float y, float size, float alpha) {
      if (stack != null && !stack.isEmpty() && !(alpha <= 0.004F)) {
         startFrame();
         int px = (int)pixels(size);
         float x0 = Math.round(x);
         float y0 = Math.round(y);

         ItemIcons.Cell cell;
         try {
            cell = cell(stack, px);
         } finally {
            MODEL.clear();
         }

         if (cell != null) {
            if (cell.texture == 0) {
               float r = Math.max(1.0F, px * 0.18F);
               dl.addRectFilled(x0 + 1.0F, y0 + 1.0F, x0 + px - 1.0F, y0 + px - 1.0F, scaleAlpha(452984831, alpha), r);
            } else {
               dl.addImage(cell.texture, x0, y0, x0 + px, y0 + px, cell.u0, cell.v0, cell.u1, cell.v1, scaleAlpha(-1, alpha));
            }
         }
      }
   }

   private static void startFrame() {
      int now = ImGuiManager.getInstance().frameCount();
      if (now != frame) {
         frame = now;
         paintsLeft = PAINTS_PER_FRAME;
         if (!RETIRED.isEmpty() && now != retiredFrame) {
            for (ItemIcons.Sheet sheet : RETIRED) {
               sheet.release();
            }

            RETIRED.clear();
         }

         GpuTexture atlas = currentAtlas();
         boolean lost = now - auditedFrame >= 30 && audit();
         if (atlas == itemAtlas && !lost) {
            Iterator<ItemIcons.Sheet> it = SHEETS.values().iterator();

            while (it.hasNext()) {
               ItemIcons.Sheet sheet = it.next();
               if (now - sheet.lastUsed > 600) {
                  sheet.release();
                  it.remove();
               }
            }
         } else {
            itemAtlas = atlas;
            ObjectIterator it = SHEETS.values().iterator();

            while (it.hasNext()) {
               ItemIcons.Sheet sheet = (ItemIcons.Sheet)it.next();
               sheet.release();
            }

            SHEETS.clear();
            BROKEN.clear();
         }
      }
   }

   private static boolean audit() {
      auditedFrame = frame;
      boolean lost = false;
      if (depth != null && !alive(depth)) {
         depthView.close();
         depth = null;
         depthView = null;
         lost = true;
      }

      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet sheet = (ItemIcons.Sheet)var1.next();
         lost |= sheet.color != null && !alive(sheet.color);
      }

      if (lost) {
         LOGGER.warn("[Venti] HUD item icon textures were lost; drawing them again");
      }

      return lost;
   }

   private static GpuTexture currentAtlas() {
      try {
         return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getTexture();
      } catch (RuntimeException var1) {
         return null;
      }
   }

   private static ItemIcons.Cell cell(ItemStack stack, int px) {
      ItemIcons.Sheet sheet = sheet(px);
      if (sheet == null) {
         return null;
      } else {
         sheet.lastUsed = frame;
         if (!resolve(stack)) {
            return BLANK;
         } else {
            ItemIcons.Cell cell = (ItemIcons.Cell)sheet.cells.get(MODEL.identity);
            if (cell != null) {
               return cell;
            } else if (disabled) {
               return BLANK;
            } else if (paintsLeft <= 0) {
               return null;
            } else {
               paintsLeft--;
               if (sheet.full()) {
                  retire(sheet);
                  sheet = new ItemIcons.Sheet(px);
                  sheet.lastUsed = frame;
                  SHEETS.put(px, sheet);
               }

               cell = paint(sheet, seeThrough(stack.getItem()));
               sheet.cells.put(Arrays.asList(MODEL.identity.toArray()), cell);
               return cell;
            }
         }
      }
   }

   private static ItemIcons.Sheet sheet(int px) {
      ItemIcons.Sheet sheet = (ItemIcons.Sheet)SHEETS.get(px);
      if (sheet != null) {
         return sheet;
      } else {
         if (SHEETS.size() >= 8) {
            ItemIcons.Sheet oldest = stalest();
            if (oldest == null || frame - oldest.lastUsed < 2) {
               return null;
            }

            retire(oldest);
         }

         sheet = new ItemIcons.Sheet(px);
         SHEETS.put(px, sheet);
         return sheet;
      }
   }

   private static ItemIcons.Sheet stalest() {
      ItemIcons.Sheet oldest = null;
      ObjectIterator var1 = SHEETS.values().iterator();

      while (var1.hasNext()) {
         ItemIcons.Sheet sheet = (ItemIcons.Sheet)var1.next();
         if (oldest == null || sheet.lastUsed < oldest.lastUsed) {
            oldest = sheet;
         }
      }

      return oldest;
   }

   private static void retire(ItemIcons.Sheet sheet) {
      SHEETS.remove(sheet.px);
      RETIRED.add(sheet);
      retiredFrame = frame;
   }

   private static boolean resolve(ItemStack stack) {
      if (BROKEN.contains(stack.getItem())) {
         return false;
      } else if (MODEL.resolve(stack)) {
         return true;
      } else {
         BROKEN.add(stack.getItem());
         return false;
      }
   }

   private static boolean seeThrough(Item item) {
      //? if >=26.2 {
      // 26.x picks render layers per model quad; translucent block items are the glass-like blocks
      // (straightening an icon that is opaque after all changes nothing).
      return item instanceof BlockItem block
         && (block.getBlock() instanceof BeaconBeamBlock || block.getBlock() instanceof HalfTransparentBlock || block.getBlock() instanceof NetherPortalBlock);
      //?} else {
      /*return item instanceof BlockItem block ? ItemBlockRenderTypes.getChunkRenderType(block.getBlock().defaultBlockState()) == ChunkSectionLayer.TRANSLUCENT : false;
      *///?}
   }

   private static ItemIcons.Cell paint(ItemIcons.Sheet sheet, boolean seeThrough) {
      if (MODEL.isEmpty()) {
         return BLANK;
      } else {
         try {
            if (sheet.color == null) {
               allocate(sheet);
            }

            int px = sheet.px;
            int perRow = 512 / px;
            int index = sheet.filled++;
            int left = index % perRow * px;
            int top = index / perRow * px;
            int glTop = 512 - top - px;
            render(MODEL, sheet.color, sheet.colorView, depth, depthView, left, top, px);
            if (seeThrough) {
               straighten(sheet.color, left, glTop, px);
            }

            ItemIcons.Cell cell = new ItemIcons.Cell();
            cell.texture = ((GlTexture)sheet.color).glId();
            cell.u0 = left / 512.0F;
            cell.u1 = (left + px) / 512.0F;
            cell.v0 = (512 - top) / 512.0F;
            cell.v1 = glTop / 512.0F;
            return cell;
         } catch (RuntimeException var9) {
            disabled = true;
            LOGGER.warn("[Venti] HUD item icons could not be drawn; showing plain slots instead", var9);
            return BLANK;
         }
      }
   }

   private static void allocate(ItemIcons.Sheet sheet) {
      GpuDevice device = RenderSystem.getDevice();
      if (depth == null) {
         depth = create("Venti HUD icon depth", 8, true, 512);
         depthView = device.createTextureView(depth);
      }

      sheet.color = create("Venti HUD icons " + sheet.px, 15, false, 512);
      sheet.colorView = device.createTextureView(sheet.color);
      Gpu.clearColorAndDepth(device.createCommandEncoder(), sheet.color, depth);
   }

   public static void render(
      ItemStackRenderState model, GpuTexture target, GpuTextureView targetView, GpuTexture targetDepth, GpuTextureView targetDepthView, int x, int y, int cell
   ) {
      Minecraft mc = Minecraft.getInstance();
      int side = target.getWidth(0);
      int glY = side - y - cell;
      Gpu.clearColorAndDepth(RenderSystem.getDevice().createCommandEncoder(), target, targetDepth, x, glY, cell, cell);
      //? if >=26.2 {
      if (submits == null) {
         submits = new SubmitNodeStorage();
      }
      //?} else {
      /*if (features == null) {
         buffers = MultiBufferSource.immediate(new ByteBufferBuilder(262144));
         submits = new SubmitNodeStorage();
         features = new FeatureRenderDispatcher(
            submits, mc.getBlockRenderer(), buffers, mc.getAtlasManager(), mc.renderBuffers().outlineBufferSource(), mc.renderBuffers().crumblingBufferSource(), mc.font
         );
      }
      *///?}

      GpuTextureView colorBefore = RenderSystem.outputColorTextureOverride;
      GpuTextureView depthBefore = RenderSystem.outputDepthTextureOverride;
      GpuBufferSlice lightsBefore = RenderSystem.getShaderLights();
      GpuBufferSlice fogBefore = RenderSystem.getShaderFog();
      GpuBufferSlice projectionBefore = RenderSystem.getProjectionMatrixBuffer();
      ProjectionType projectionTypeBefore = RenderSystem.getProjectionType();
      GL11.glGetIntegerv(2978, VIEWPORT);
      GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.identity();

      try {
         RenderSystem.outputColorTextureOverride = targetView;
         RenderSystem.outputDepthTextureOverride = targetDepthView;
         //? if >=26.2 {
         if (projection == null) {
            projection = new ProjectionMatrixBuffer("Venti icons");
         }

         ORTHO.setupOrtho(-1000.0F, 1000.0F, side, side, true);
         RenderSystem.setProjectionMatrix(projection.getBuffer(ORTHO), ProjectionType.ORTHOGRAPHIC);
         Entry light = model.usesBlockLight() ? Entry.ITEMS_3D : Entry.ITEMS_FLAT;
         mc.gameRenderer.lighting().setupFor(light);
         //?} else {
         /*if (projection == null) {
            projection = new CachedOrthoProjectionMatrixBuffer("Venti icons", -1000.0F, 1000.0F, true);
         }

         RenderSystem.setProjectionMatrix(projection.getBuffer(side, side), ProjectionType.ORTHOGRAPHIC);
         Entry light = model.usesBlockLight() ? Entry.ITEMS_3D : Entry.ITEMS_FLAT;
         mc.gameRenderer.getLighting().setupFor(light);
         *///?}
         RenderSystem.setShaderFog(((dev.venti.mixin.streamhider.GameRendererAccessor)mc.gameRenderer).venti$noInvLeakFogRenderer().getBuffer(FogMode.NONE));
         RenderSystem.enableScissorForRenderTypeDraws(x, glY, cell, cell);
         PoseStack pose = new PoseStack();
         pose.translate(x + cell * 0.5F, y + cell * 0.5F, 0.0F);
         pose.scale(cell, -cell, cell);

         try {
            model.submit(pose, submits, 15728880, OverlayTexture.NO_OVERLAY, 0);
            //? if >=26.2 {
            mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(submits);
            //?} else {
            /*features.renderAllFeatures();
            *///?}
         } finally {
            //? if <26.2 {
            /*submits.clear();
            buffers.endBatch();
            *///?}
         }
      } finally {
         RenderSystem.disableScissorForRenderTypeDraws();
         RenderSystem.outputColorTextureOverride = colorBefore;
         RenderSystem.outputDepthTextureOverride = depthBefore;
         RenderSystem.setShaderLights(lightsBefore);
         RenderSystem.setShaderFog(fogBefore);
         RenderSystem.setProjectionMatrix(projectionBefore, projectionTypeBefore);
         modelView.popMatrix();
         GlStateManager._viewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
      }
   }

   static void straighten(GpuTexture page, int x, int y, int cell) {
      if (!straightenFailed) {
         int bytes = cell * cell * 4;
         long whole = (long)page.getWidth(0) * page.getHeight(0) * 4L;

         try {
            GpuDevice device = RenderSystem.getDevice();
            if (readback == null || readback.size() < whole) {
               if (readback != null) {
                  readback.close();
               }

               readback = device.createBuffer(() -> "Venti icon readback", 9, whole);
            }

            if (straightened == null || straightened.capacity() < bytes) {
               straightened = BufferUtils.createByteBuffer(bytes);
            }

            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToBuffer(page, readback, 0L, () -> {}, 0, x, y, cell, cell);
            GpuFence copied = encoder.createFence();

            try {
               GL11.glFlush();
               if (!copied.awaitCompletion(1000000000L)) {
                  throw new IllegalStateException("icon read-back timed out");
               }
            } catch (Throwable var25) {
               if (copied != null) {
                  try {
                     copied.close();
                  } catch (Throwable var23) {
                     var25.addSuppressed(var23);
                  }
               }

               throw var25;
            }

            if (copied != null) {
               copied.close();
            }

            straightened.clear().limit(bytes);
            Gpu.Mapped view = Gpu.map(encoder, readback.slice(0L, bytes), true, false);

            try {
               ByteBuffer read = view.data();

               for (int i = 0; i < bytes; i += 4) {
                  int a = read.get(i + 3) & 255;

                  for (int ch = 0; ch < 3; ch++) {
                     int v = a == 0 ? 0 : Math.min(255, ((read.get(i + ch) & 255) * 255 + a / 2) / a);
                     straightened.put(i + ch, (byte)v);
                  }

                  straightened.put(i + 3, (byte)a);
               }
            } catch (Throwable var24) {
               if (view != null) {
                  try {
                     view.close();
                  } catch (Throwable var22) {
                     var24.addSuppressed(var22);
                  }
               }

               throw var24;
            }

            if (view != null) {
               view.close();
            }

            Gpu.writeRgba(encoder, page, straightened, x, y, cell, cell);
         } catch (RuntimeException var26) {
            straightenFailed = true;
            LOGGER.warn("[Venti] Couldn't read icons back; see-through ones stay darker", var26);
         } finally {
            GlStateManager._pixelStore(3330, 0);
         }
      }
   }

   static GpuTexture create(String label, int usage, boolean depthFormat, int side) {
      GlStateManager._activeTexture(GL11.glGetInteger(34016));
      GlStateManager._bindTexture(GL11.glGetInteger(32873));
      GpuTexture texture = Gpu.texture(() -> label, usage, depthFormat, side, side, 1);
      if (texture instanceof GlTexture gl && !prepare(gl)) {
         texture.close();
         throw new IllegalStateException(label + " got no storage");
      } else {
         return texture;
      }
   }

   private static boolean prepare(GlTexture gl) {
      int bound = GL11.glGetInteger(32873);
      GL11.glBindTexture(3553, gl.glId());

      boolean held;
      try {
         if (!holds(gl)) {
            int unpack = GL11.glGetInteger(35055);
            GL15.glBindBuffer(35052, 0);
            var format = gl.getFormat();
            GL11.glTexImage2D(
               3553,
               0,
               GlConst.toGlInternalId(format),
               gl.getWidth(0),
               gl.getHeight(0),
               0,
               GlConst.toGlExternalId(format),
               GlConst.toGlType(format),
               (ByteBuffer)null
            );
            GL15.glBindBuffer(35052, unpack);
         }

         GL11.glTexParameteri(3553, 33085, 0);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         held = holds(gl);
      } finally {
         GL11.glBindTexture(3553, bound);
      }

      return held;
   }

   static boolean alive(GpuTexture texture) {
      if (texture.isClosed()) {
         return false;
      } else if (texture instanceof GlTexture gl) {
         if (!GL11.glIsTexture(gl.glId())) {
            return false;
         } else {
            int bound = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, gl.glId());
            boolean alive = GL11.glGetInteger(32873) == gl.glId() && holds(gl);
            GL11.glBindTexture(3553, bound);
            if (!alive) {
               GlStateManager.clearGlErrors();
            }

            return alive;
         }
      } else {
         return true;
      }
   }

   private static boolean holds(GpuTexture texture) {
      return GL11.glGetTexLevelParameteri(3553, 0, 4096) == texture.getWidth(0) && GL11.glGetTexLevelParameteri(3553, 0, 4097) == texture.getHeight(0);
   }

   static void dispose(GpuTexture texture) {
      if (texture != null && alive(texture)) {
         texture.close();
      }
   }

   private static final class Cell {
      int texture;
      float u0;
      float v0;
      float u1;
      float v1;
   }

   private static final class Sheet {
      final int px;
      final Object2ObjectOpenHashMap<Object, ItemIcons.Cell> cells = new Object2ObjectOpenHashMap();
      GpuTexture color;
      GpuTextureView colorView;
      int filled;
      int lastUsed;

      Sheet(int px) {
         this.px = px;
      }

      boolean full() {
         int perRow = 512 / this.px;
         return this.filled >= perRow * perRow;
      }

      void release() {
         if (this.colorView != null) {
            this.colorView.close();
         }

         ItemIcons.dispose(this.color);
         this.colorView = null;
         this.color = null;
         this.cells.clear();
      }
   }
}
