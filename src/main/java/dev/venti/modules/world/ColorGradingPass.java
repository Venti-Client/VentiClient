package dev.venti.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
final class ColorGradingPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.of("venti", "pipeline/color_grading"))
      //? if >=1.21.9 {
      .withVertexShader("core/screenquad")
      //?} else {
      /*.withVertexShader("core/blit_screen") // full-screen quad shader before 1.21.9
      *///?}
      .withFragmentShader(Identifier.of("venti", "core/color_grading"))
      .withSampler("InSampler")
      .withUniform("ColorGradingConfig", UniformType.UNIFORM_BUFFER)
      .withoutBlend()
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      //? if >=1.21.9 {
      .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
      //?} else {
      /*.withVertexFormat(VertexFormats.POSITION, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
      *///?}
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Venti color grading";
   private static final Supplier<String> CONFIG_LABEL = () -> "Venti color grading config";
   private static final int CONFIG_SIZE = 32;
   private static SimpleFramebuffer copy;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean invalidShaderLogged;

   private ColorGradingPass() {
   }

   static void apply(boolean worldRendered, float vibrance, float contrast, float exposure, float warmth, float vignette) {
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
         if (copy == null) {
            allocate(main.textureWidth, main.textureHeight);
         } else if (copy.textureWidth != main.textureWidth || copy.textureHeight != main.textureHeight) {
            copy.resize(main.textureWidth, main.textureHeight);
         }

         GpuDevice device = RenderSystem.getDevice();
         if (shaderReady(device)) {
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(mainColor, copy.getColorAttachment(), 0, 0, 0, 0, 0, main.textureWidth, main.textureHeight);
            configData.putFloat(0, vibrance);
            configData.putFloat(4, contrast);
            configData.putFloat(8, exposure);
            configData.putFloat(12, warmth);
            configData.putFloat(16, vignette);
            encoder.writeToBuffer(configSlice, configData);
            RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorAttachmentView(), OptionalInt.empty());

            try {
               pass.setPipeline(PIPELINE);
               pass.setUniform("ColorGradingConfig", configSlice);
               //? if >=1.21.11 {
               pass.bindTexture("InSampler", copy.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
               //?} else {
               /*copy.getColorAttachment().setTextureFilter(FilterMode.NEAREST, false);
               pass.bindSampler("InSampler", copy.getColorAttachmentView());
               *///?}
               //? if >=1.21.9 {
               pass.draw(0, 3);
               //?} else {
               /*pass.setVertexBuffer(0, com.mojang.blaze3d.systems.RenderSystem.getQuadVertexBuffer()); // no vertex-less draws before 1.21.9
               com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer quads = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS);
               pass.setIndexBuffer(quads.getIndexBuffer(6), quads.getIndexType());
               pass.drawIndexed(0, 0, 6, 1);
               *///?}
            } catch (Throwable var15) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (pass != null) {
               pass.close();
            }
         }
      }
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid()) {
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", PIPELINE.getFragmentShader());
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      copy = new SimpleFramebuffer("Venti color grading copy", width, height, false);
      //? if >=1.21.11 {
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      //?} else {
      /*config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32);
      *///?}
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      if (copy != null) {
         copy.delete();
         copy = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
