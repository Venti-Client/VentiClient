package dev.venti.modules.hand;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Captures the first-person hand into its own target, then composites it back with a glow (ported from Anubis). */
final class HandAuraPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder()
      .withLocation(Identifier.of("venti", "pipeline/hand_aura"))
      //? if >=1.21.9 {
      .withVertexShader("core/screenquad")
      //?} else {
      /*.withVertexShader("core/blit_screen") // full-screen quad shader before 1.21.9
      *///?}
      .withFragmentShader(Identifier.of("venti", "core/hand_aura"))
      .withSampler("CaptureSampler")
      .withSampler("HaloSampler")
      .withUniform("HandAuraConfig", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
      .withColorWrite(true, false)
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      //? if >=1.21.9 {
      .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
      //?} else {
      /*.withVertexFormat(VertexFormats.POSITION, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS)
      *///?}
      .build();
   private static final Supplier<String> PASS_LABEL = () -> "Venti hand aura";
   private static final Supplier<String> CONFIG_LABEL = () -> "Venti hand aura config";
   private static final int CONFIG_SIZE = 48;
   private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();
   private static SimpleFramebuffer hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandAuraPass() {
   }

   static boolean begin() {
      if (capturing || RenderSystem.outputColorTextureOverride != null || RenderSystem.outputDepthTextureOverride != null) {
         return false;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      Framebuffer main = mc.getFramebuffer();
      if (mc.world == null
         || mc.gameRenderer.isRenderingPanorama()
         || main.getColorAttachment() == null
         || main.textureWidth <= 0
         || main.textureHeight <= 0
         || shaderPackInUse()) {
         return false;
      }

      GpuDevice device = RenderSystem.getDevice();
      if (!shaderReady(device)) {
         return false;
      }

      if (hand == null) {
         allocate(main.textureWidth, main.textureHeight);
      } else if (hand.textureWidth != main.textureWidth || hand.textureHeight != main.textureHeight) {
         hand.resize(main.textureWidth, main.textureHeight);
      }

      device.createCommandEncoder().clearColorAndDepthTextures(hand.getColorAttachment(), 0, hand.getDepthAttachment(), 1.0);
      RenderSystem.outputColorTextureOverride = hand.getColorAttachmentView();
      RenderSystem.outputDepthTextureOverride = hand.getDepthAttachmentView();
      capturing = true;
      return true;
   }

   static boolean end() {
      if (!capturing) {
         return false;
      }

      capturing = false;
      RenderSystem.outputColorTextureOverride = null;
      RenderSystem.outputDepthTextureOverride = null;
      return true;
   }

   static void compose(int tintArgb, float aura, float reachPx, float rim, float rimPx, int sheenArgb, float sheen, float phase) {
      Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = aura > 0.0F
         ? blur.spread(encoder, hand.getColorAttachmentView(), hand.textureWidth, hand.textureHeight, reachPx)
         : hand.getColorAttachmentView();
      putColor(0, tintArgb, aura);
      putColor(16, sheenArgb, sheen);
      configData.putFloat(32, rim);
      configData.putFloat(36, rimPx);
      configData.putFloat(40, phase);
      configData.putFloat(44, 0.0F);
      encoder.writeToBuffer(configSlice, configData);
      try (RenderPass pass = encoder.createRenderPass(PASS_LABEL, main.getColorAttachmentView(), OptionalInt.empty())) {
         pass.setPipeline(PIPELINE);
         pass.setUniform("HandAuraConfig", configSlice);
         //? if >=1.21.11 {
         pass.bindTexture("CaptureSampler", hand.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         pass.bindTexture("HaloSampler", halo, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         //?} else {
         /*hand.getColorAttachment().setTextureFilter(FilterMode.LINEAR, false);
         halo.texture().setTextureFilter(FilterMode.LINEAR, false);
         pass.bindSampler("CaptureSampler", hand.getColorAttachmentView());
         pass.bindSampler("HaloSampler", halo);
         *///?}
         //? if >=1.21.9 {
         pass.draw(0, 3);
         //?} else {
         /*pass.setVertexBuffer(0, com.mojang.blaze3d.systems.RenderSystem.getQuadVertexBuffer()); // no vertex-less draws before 1.21.9
         com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer quads = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.DrawMode.QUADS);
         pass.setIndexBuffer(quads.getIndexBuffer(6), quads.getIndexType());
         pass.drawIndexed(0, 0, 6, 1);
         *///?}
      }
   }

   private static void putColor(int offset, int argb, float weight) {
      configData.putFloat(offset, (argb >> 16 & 0xFF) / 255.0F);
      configData.putFloat(offset + 4, (argb >> 8 & 0xFF) / 255.0F);
      configData.putFloat(offset + 8, (argb & 0xFF) / 255.0F);
      configData.putFloat(offset + 12, weight);
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(device)) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("[Venti] Hand Aura shaders did not compile; the hand is drawn as usual");
      }

      return false;
   }

   private static void allocate(int width, int height) {
      hand = new SimpleFramebuffer("Venti hand aura", width, height, true);
      blur = new AuraBlur("Venti hand aura");
      config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, CONFIG_SIZE);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(CONFIG_SIZE);
   }

   static void release() {
      if (capturing) {
         end();
      }

      if (hand != null) {
         hand.delete();
         hand = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }

   /** Iris draws the world through its own targets, so the capture is skipped while a shader pack is on. */
   private static boolean shaderPackInUse() {
      if (SHADER_PACK_IN_USE == null) {
         return false;
      }

      try {
         return (boolean)SHADER_PACK_IN_USE.invokeExact();
      } catch (Throwable e) {
         return false;
      }
   }

   private static MethodHandle irisProbe() {
      if (!FabricLoader.getInstance().isModLoaded("iris")) {
         return null;
      }

      try {
         Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Object instance = api.getMethod("getInstance").invoke(null);
         return MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
      } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
         return null;
      }
   }
}
