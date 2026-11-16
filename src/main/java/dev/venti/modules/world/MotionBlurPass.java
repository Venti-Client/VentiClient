package dev.venti.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class MotionBlurPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.of("venti", "pipeline/motion_blur"))
      //? if >=1.21.9 {
      .withVertexShader("core/screenquad")
      //?} else {
      /*.withVertexShader("core/blit_screen") // full-screen quad shader before 1.21.9
      *///?}
      .withFragmentShader(Identifier.of("venti", "core/motion_blur"))
      .withSampler("InSampler")
      .withUniform("MotionBlurConfig", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      //? if >=1.21.9 {
      .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
      //?} else {
      /*.withVertexFormat(VertexFormats.POSITION, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
      *///?}
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Venti motion blur";
   private static final Supplier<String> CONFIG_LABEL = () -> "Venti motion blur config";
   private static final int CONFIG_SIZE = 16;
   private static final long STALE_NANOS = 1000000000L;
   private static final float MIN_RETENTION = 0.003921569F;
   private static SimpleFramebuffer history;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean primed;
   private static long lastFrameNanos;
   private static int levelKey;
   private static int seed;
   private static boolean invalidShaderLogged;

   private MotionBlurPass() {
   }

   static void apply(boolean worldRendered, float retention60) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Framebuffer main = mc.getFramebuffer();
      GpuTexture mainColor = main.getColorAttachment();
      if (worldRendered
         && mc.world != null
         && mc.isFinishedLoading()
         && !mc.gameRenderer.isRenderingPanorama()
         && mainColor != null
         && main.textureWidth > 0
         && main.textureHeight > 0) {
         long now = System.nanoTime();
         long gap = now - lastFrameNanos;
         lastFrameNanos = now;
         int level = System.identityHashCode(mc.world);
         if (level != levelKey) {
            levelKey = level;
            primed = false;
         }

         if (history == null) {
            allocate(main.textureWidth, main.textureHeight);
            primed = false;
         } else if (history.textureWidth != main.textureWidth || history.textureHeight != main.textureHeight) {
            history.resize(main.textureWidth, main.textureHeight);
            primed = false;
         }

         GpuDevice device = RenderSystem.getDevice();
         CommandEncoder encoder = device.createCommandEncoder();
         if (primed && gap > 0L && gap < 1000000000L) {
            float retention = (float)Math.pow(retention60, gap * 6.0E-8);
            if (retention >= 0.003921569F && shaderReady(device)) {
               blend(encoder, main, retention);
            }
         }

         encoder.copyTextureToTexture(mainColor, history.getColorAttachment(), 0, 0, 0, 0, 0, main.textureWidth, main.textureHeight);
         primed = true;
      } else {
         primed = false;
      }
   }

   private static void blend(CommandEncoder encoder, Framebuffer main, float retention) {
      seed = seed + 1 & 63;
      configData.putFloat(0, retention);
      configData.putFloat(4, seed * 7.0F);
      encoder.writeToBuffer(configSlice, configData);
      RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorAttachmentView(), OptionalInt.empty());

      try {
         pass.setPipeline(PIPELINE);
         pass.setUniform("MotionBlurConfig", configSlice);
         //? if >=1.21.11 {
         pass.bindTexture("InSampler", history.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         //?} else {
         /*history.getColorAttachment().setTextureFilter(FilterMode.NEAREST, false);
         pass.bindSampler("InSampler", history.getColorAttachmentView());
         *///?}
         //? if >=1.21.9 {
         pass.draw(0, 3);
         //?} else {
         /*pass.setVertexBuffer(0, com.mojang.blaze3d.systems.RenderSystem.getQuadVertexBuffer()); // no vertex-less draws before 1.21.9
         com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer quads = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS);
         pass.setIndexBuffer(quads.getIndexBuffer(6), quads.getIndexType());
         pass.drawIndexed(0, 0, 6, 1);
         *///?}
      } catch (Throwable var7) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Motion Blur shader {} did not compile; frames are passed through unblurred", PIPELINE.getFragmentShader());
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      history = new SimpleFramebuffer("Venti motion blur history", width, height, false);
      //? if >=1.21.11 {
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16L);
      //?} else {
      /*config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 16);
      *///?}
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(16);
   }

   static void release() {
      if (history != null) {
         history.delete();
         history = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
      primed = false;
      lastFrameNanos = 0L;
      levelKey = 0;
   }
}
