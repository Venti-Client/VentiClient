//? if >=26.3 {
/*package dev.venti.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.joml.Vector4fc;
import org.lwjgl.PointerBuffer;

// The vanilla sky's render pass on 26.3. Up to 26.2 every part of the sky (disc, sunrise, sun, moon, stars) opened its
// own pass, and Venti's sky modules drew their own passes in between. 26.3 draws the whole sky in one pass, and only one
// pass may be open at a time; this pass can be suspended (closed) for Venti to draw, and reopens on the same targets
// the next time vanilla draws into it.
@Environment(EnvType.CLIENT)
public final class SkyPass implements RenderPass {
   @Nullable
   private static SkyPass current;
   private final CommandEncoder encoder;
   private final Supplier<String> label;
   private final GpuTextureView color;
   @Nullable
   private final GpuTextureView depth;
   @Nullable
   private RenderPass pass;

   private SkyPass(CommandEncoder encoder, Supplier<String> label, GpuTextureView color, @Nullable GpuTextureView depth, RenderPass pass) {
      this.encoder = encoder;
      this.label = label;
      this.color = color;
      this.depth = depth;
      this.pass = pass;
   }

   public static RenderPass open(
      CommandEncoder encoder, Supplier<String> label, GpuTextureView color, Optional<Vector4fc> clearColor, @Nullable GpuTextureView depth, OptionalDouble clearDepth
   ) {
      SkyPass sky = new SkyPass(encoder, label, color, depth, encoder.createRenderPass(label, color, clearColor, depth, clearDepth));
      current = sky;
      return sky;
   }

   // Closes the open sky pass, if any, so Venti can draw its own passes.
   public static void suspend() {
      SkyPass sky = current;
      if (sky != null && sky.pass != null) {
         sky.pass.close();
         sky.pass = null;
      }
   }

   private RenderPass pass() {
      if (this.pass == null) {
         this.pass = this.encoder.createRenderPass(this.label, this.color, Optional.empty(), this.depth, OptionalDouble.empty());
         RenderSystem.bindDefaultUniforms(this.pass);
      }

      return this.pass;
   }

   @Override
   public void close() {
      if (current == this) {
         current = null;
      }

      if (this.pass != null) {
         this.pass.close();
         this.pass = null;
      }
   }

   @Override
   public void pushDebugGroup(Supplier<String> label) {
      this.pass().pushDebugGroup(label);
   }

   @Override
   public void popDebugGroup() {
      this.pass().popDebugGroup();
   }

   @Override
   public void writeTimestamp(GpuQueryPool pool, int index) {
      this.pass().writeTimestamp(pool, index);
   }

   @Override
   public void setPipeline(CompiledRenderPipeline pipeline) {
      this.pass().setPipeline(pipeline);
   }

   @Override
   public void setUniform(String name, GpuTextureView view, GpuSampler sampler) {
      this.pass().setUniform(name, view, sampler);
   }

   @Override
   public void setUniform(String name, GpuBuffer buffer) {
      this.pass().setUniform(name, buffer);
   }

   @Override
   public void setUniform(String name, GpuBufferSlice slice) {
      this.pass().setUniform(name, slice);
   }

   @Override
   public void pushConstants(ByteBuffer data) {
      this.pass().pushConstants(data);
   }

   @Override
   public void enableScissor(int x, int y, int width, int height) {
      this.pass().enableScissor(x, y, width, height);
   }

   @Override
   public void disableScissor() {
      this.pass().disableScissor();
   }

   @Override
   public void setVertexBuffer(int slot, GpuBufferSlice vertices) {
      this.pass().setVertexBuffer(slot, vertices);
   }

   @Override
   public void setIndexBuffer(GpuBuffer indices, IndexType type) {
      this.pass().setIndexBuffer(indices, type);
   }

   @Override
   public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int baseVertex, int firstInstance) {
      this.pass().drawIndexed(indexCount, instanceCount, firstIndex, baseVertex, firstInstance);
   }

   @Override
   public void multiDrawIndexed(IntBuffer a, int b, int c, int d) {
      this.pass().multiDrawIndexed(a, b, c, d);
   }

   @Override
   public void multiDrawIndexed(PointerBuffer a, IntBuffer b, IntBuffer c, int d) {
      this.pass().multiDrawIndexed(a, b, c, d);
   }

   @Override
   public void drawIndexedIndirect(GpuBufferSlice commands, int count) {
      this.pass().drawIndexedIndirect(commands, count);
   }

   @Override
   public <T> void drawMultipleIndexed(
      Collection<RenderPass.Draw<T>> draws, @Nullable GpuBuffer indices, @Nullable IndexType type, Collection<String> uniforms, T userData
   ) {
      this.pass().drawMultipleIndexed(draws, indices, type, uniforms, userData);
   }

   @Override
   public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
      this.pass().draw(vertexCount, instanceCount, firstVertex, firstInstance);
   }

   @Override
   public void multiDraw(IntBuffer a, int b, int c, int d) {
      this.pass().multiDraw(a, b, c, d);
   }

   @Override
   public void multiDraw(IntBuffer a, IntBuffer b, int c) {
      this.pass().multiDraw(a, b, c);
   }

   @Override
   public void drawIndirect(GpuBufferSlice commands, int count) {
      this.pass().drawIndirect(commands, count);
   }
}
*///?}
