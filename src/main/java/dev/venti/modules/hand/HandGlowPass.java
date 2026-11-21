package dev.venti.modules.hand;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.venti.compat.Gpu;
import dev.venti.compat.Mc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Captures the first-person hand into its own target, then composites it back with a glow (ported from Anubis). */
final class HandGlowPass {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = Gpu.pipeline("hand_aura")
         .shaders(Identifier.withDefaultNamespace("core/screenquad"), Identifier.fromNamespaceAndPath("venti", "core/hand_aura"))
         .sampler("CaptureSampler")
         .sampler("HaloSampler")
         .uniform("HandGlowConfig")
         .blend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
         .colorOnly()
         .depth(false, false)
         .fullScreen()
         .build();
   private static final Supplier<String> PASS_LABEL = () -> "Venti hand aura";
   private static final Supplier<String> CONFIG_LABEL = () -> "Venti hand aura config";
   private static final int CONFIG_SIZE = 48;
   private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();
   private static TextureTarget hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandGlowPass() {
   }

   static boolean begin() {
      if (capturing || RenderSystem.outputColorTextureOverride != null || RenderSystem.outputDepthTextureOverride != null) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = Mc.mainTarget();
      if (mc.level == null
         || Mc.panoramic()
         || main.getColorTexture() == null
         || main.width <= 0
         || main.height <= 0
         || shaderPackInUse()) {
         return false;
      }

      GpuDevice device = RenderSystem.getDevice();
      if (!shaderReady(device)) {
         return false;
      }

      if (hand == null) {
         allocate(main.width, main.height);
      } else if (hand.width != main.width || hand.height != main.height) {
         hand.resize(main.width, main.height);
      }

      Gpu.clearColorAndDepth(device.createCommandEncoder(), hand.getColorTexture(), hand.getDepthTexture());
      RenderSystem.outputColorTextureOverride = hand.getColorTextureView();
      RenderSystem.outputDepthTextureOverride = hand.getDepthTextureView();
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
      RenderTarget main = Mc.mainTarget();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = aura > 0.0F
         ? blur.spread(encoder, hand.getColorTextureView(), hand.width, hand.height, reachPx)
         : hand.getColorTextureView();
      putColor(0, tintArgb, aura);
      putColor(16, sheenArgb, sheen);
      configData.putFloat(32, rim);
      configData.putFloat(36, rimPx);
      configData.putFloat(40, phase);
      configData.putFloat(44, 0.0F);
      encoder.writeToBuffer(configSlice, configData);
      try (RenderPass pass = Gpu.pass(encoder, PASS_LABEL, main.getColorTextureView(), null)) {
         pass.setPipeline(PIPELINE);
         pass.setUniform("HandGlowConfig", configSlice);
         pass.bindTexture("CaptureSampler", hand.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.bindTexture("HaloSampler", halo, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         Gpu.drawFullScreen(pass);
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
         LOGGER.warn("[Venti] Hand Glow shaders did not compile; the hand is drawn as usual");
      }

      return false;
   }

   private static void allocate(int width, int height) {
      hand = Gpu.target("Venti hand aura", width, height, true);
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
         hand.destroyBuffers();
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
