package dev.venti.modules.hand;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.venti.compat.Gpu;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;

/** Two-pass separable blur at half resolution that spreads the captured hand into a halo (ported from Anubis). */
final class AuraBlur {
   private static final RenderPipeline PIPELINE = Gpu.pipeline("aura_spread")
         .shaders(Identifier.withDefaultNamespace("core/screenquad"), Identifier.fromNamespaceAndPath("venti", "core/aura_spread"))
         .sampler("InSampler")
         .uniform("AuraSpread")
         .blend(null)
         .depth(false, false)
         .fullScreen()
         .build();
   private static final int CONFIG_SIZE = 16;
   private static final float TAPS = 6.0F;
   private final String name;
   private final Supplier<String> passLabel;
   private TextureTarget across;
   private TextureTarget down;
   private GpuBuffer acrossConfig;
   private GpuBuffer downConfig;
   private GpuBufferSlice acrossSlice;
   private GpuBufferSlice downSlice;
   private ByteBuffer configData;

   AuraBlur(String name) {
      this.name = name;
      this.passLabel = () -> name + " spread";
   }

   static boolean compiled(GpuDevice device) {
      //? if >=26.3 {
      /*return Gpu.compiled(PIPELINE);
      *///?} else {
      return device.precompilePipeline(PIPELINE).isValid();
      //?}
   }

   GpuTextureView spread(CommandEncoder encoder, GpuTextureView source, int width, int height, float reachPx) {
      int divisor = 2;
      int halfWidth = Math.max(1, (width + divisor - 1) / divisor);
      int halfHeight = Math.max(1, (height + divisor - 1) / divisor);
      if (this.across == null) {
         this.allocate(halfWidth, halfHeight);
      } else if (this.across.width != halfWidth || this.across.height != halfHeight) {
         this.across.resize(halfWidth, halfHeight);
         this.down.resize(halfWidth, halfHeight);
      }

      float step = Math.max(0.75F, reachPx / TAPS);
      this.run(encoder, source, this.across, this.acrossSlice, step / width, 0.0F);
      this.run(encoder, this.across.getColorTextureView(), this.down, this.downSlice, 0.0F, step / height);
      return this.down.getColorTextureView();
   }

   private void run(CommandEncoder encoder, GpuTextureView from, TextureTarget into, GpuBufferSlice slice, float du, float dv) {
      this.configData.putFloat(0, du);
      this.configData.putFloat(4, dv);
      encoder.writeToBuffer(slice, this.configData);
      try (RenderPass pass = Gpu.pass(encoder, this.passLabel, into.getColorTextureView(), null)) {
         //? if >=26.3 {
         /*Gpu.setPipeline(pass, PIPELINE);
         pass.setUniform("AuraSpread", slice);
         Gpu.bindTexture(pass, "InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         *///?} else {
         pass.setPipeline(PIPELINE);
         pass.setUniform("AuraSpread", slice);
         pass.bindTexture("InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         //?}
         Gpu.drawFullScreen(pass);
      }
   }

   private void allocate(int width, int height) {
      GpuDevice device = RenderSystem.getDevice();
      this.across = Gpu.target(this.name + " across", width, height, false);
      this.down = Gpu.target(this.name + " down", width, height, false);
      this.acrossConfig = device.createBuffer(() -> this.name + " across config", 136, CONFIG_SIZE);
      this.downConfig = device.createBuffer(() -> this.name + " down config", 136, CONFIG_SIZE);
      this.acrossSlice = this.acrossConfig.slice();
      this.downSlice = this.downConfig.slice();
      this.configData = BufferUtils.createByteBuffer(CONFIG_SIZE);
   }

   void release() {
      if (this.across != null) {
         this.across.destroyBuffers();
         this.across = null;
      }

      if (this.down != null) {
         this.down.destroyBuffers();
         this.down = null;
      }

      if (this.acrossConfig != null) {
         this.acrossConfig.close();
         this.acrossConfig = null;
      }

      if (this.downConfig != null) {
         this.downConfig.close();
         this.downConfig = null;
      }

      this.acrossSlice = null;
      this.downSlice = null;
      this.configData = null;
   }
}
