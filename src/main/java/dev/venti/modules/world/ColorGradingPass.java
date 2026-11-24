package dev.venti.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.venti.compat.Gpu;
import dev.venti.compat.Mc;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class ColorGradingPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = Gpu.pipeline("color_grading")
         .shaders(Identifier.withDefaultNamespace("core/screenquad"), Identifier.fromNamespaceAndPath("venti", "core/color_grading"))
         .sampler("InSampler")
         .uniform("ColorGradingConfig")
         .blend(null)
         .colorOnly()
         .depth(false, false)
         .fullScreen()
         .build();
   private static final Supplier<String> PASS_LABEL = () -> "Venti color grading";
   private static final Supplier<String> CONFIG_LABEL = () -> "Venti color grading config";
   private static final int CONFIG_SIZE = 32;
   private static TextureTarget copy;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean invalidShaderLogged;

   private ColorGradingPass() {
   }

   static void apply(boolean worldRendered, float vibrance, float contrast, float exposure, float warmth, float vignette) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = Mc.mainTarget();
      GpuTexture mainColor = main.getColorTexture();
      if (worldRendered
         && mc.level != null
         && mc.isGameLoadFinished()
         && !Mc.panoramic()
         && mainColor != null
         && main.width > 0
         && main.height > 0) {
         if (copy == null) {
            allocate(main.width, main.height);
         } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height);
         }

         GpuDevice device = RenderSystem.getDevice();
         if (shaderReady(device)) {
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(mainColor, copy.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
            configData.putFloat(0, vibrance);
            configData.putFloat(4, contrast);
            configData.putFloat(8, exposure);
            configData.putFloat(12, warmth);
            configData.putFloat(16, vignette);
            encoder.writeToBuffer(configSlice, configData);
            RenderPass pass = Gpu.pass(encoder, PASS_LABEL, main.getColorTextureView(), null);

            try {
               //? if >=26.3 {
               /*Gpu.setPipeline(pass, PIPELINE);
               pass.setUniform("ColorGradingConfig", configSlice);
               Gpu.bindTexture(pass, "InSampler", copy.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
               *///?} else {
               pass.setPipeline(PIPELINE);
               pass.setUniform("ColorGradingConfig", configSlice);
               pass.bindTexture("InSampler", copy.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
               //?}
               Gpu.drawFullScreen(pass);
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
      //? if >=26.3 {
      /*if (Gpu.compiled(PIPELINE)) {
      *///?} else {
      if (device.precompilePipeline(PIPELINE).isValid()) {
      //?}
         invalidShaderLogged = false;
         return true;
      } else {
         if (!invalidShaderLogged) {
            invalidShaderLogged = true;
            //? if >=26.3 {
            /*LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", Gpu.fragmentShader(PIPELINE));
            *///?} else {
            LOGGER.warn("Color Grading shader {} did not compile; the picture is left as it is", PIPELINE.getFragmentShader());
            //?}
         }

         return false;
      }
   }

   private static void allocate(int width, int height) {
      copy = Gpu.target("Venti color grading copy", width, height, false);
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(32);
   }

   static void release() {
      if (copy != null) {
         copy.destroyBuffers();
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
