package dev.venti.compat;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
//? if >=26.2 {
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import org.joml.Vector4f;
//?} else {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.OptionalInt;
*///?}

/**
 * The small slice of Blaze3D that Venti's own render passes use, written once per Minecraft version.
 * 26.2 reworked pipelines (bind group layouts, colour/depth target states, reversed depth, primitive
 * topologies) and vertex uploads (transient memory, buffer slices); modules build pipelines and draw
 * through this class so they need no version conditions of their own.
 */
@Environment(EnvType.CLIENT)
public final class Gpu {
   private Gpu() {
   }

   /** Primitive layout of a pipeline's vertices. */
   public enum Topology {
      QUADS,
      TRIANGLES
   }

   public static Pipeline pipeline(String path) {
      return new Pipeline(Identifier.fromNamespaceAndPath("venti", "pipeline/" + path));
   }

   /** Fluent description of a render pipeline; {@link #build()} turns it into the version's {@link RenderPipeline}. */
   public static final class Pipeline {
      private final Identifier location;
      private Identifier vertex;
      private Identifier fragment;
      private boolean matrices;
      private final List<String> samplers = new ArrayList<>();
      private final List<String> uniforms = new ArrayList<>();
      @Nullable
      private BlendFunction blend;
      private boolean writeAlpha = true;
      private boolean depthTest = true;
      private boolean depthWrite = true;
      private boolean cull = true;
      @Nullable
      private VertexFormat format;
      private Topology topology = Topology.TRIANGLES;

      private Pipeline(Identifier location) {
         this.location = location;
      }

      /** Vanilla shader by path ("core/position_color"). */
      public Pipeline shaders(String vertex, String fragment) {
         this.vertex = Identifier.withDefaultNamespace(vertex);
         this.fragment = Identifier.withDefaultNamespace(fragment);
         return this;
      }

      public Pipeline shaders(Identifier vertex, Identifier fragment) {
         this.vertex = vertex;
         this.fragment = fragment;
         return this;
      }

      /** Model-view ("DynamicTransforms") and projection uniforms. */
      public Pipeline matrices() {
         this.matrices = true;
         return this;
      }

      public Pipeline sampler(String name) {
         this.samplers.add(name);
         return this;
      }

      /** A std140 uniform block. */
      public Pipeline uniform(String name) {
         this.uniforms.add(name);
         return this;
      }

      /** {@code null} disables blending. */
      public Pipeline blend(@Nullable BlendFunction blend) {
         this.blend = blend;
         return this;
      }

      /** Writes colour only, leaving the target's alpha untouched. */
      public Pipeline colorOnly() {
         this.writeAlpha = false;
         return this;
      }

      public Pipeline depth(boolean test, boolean write) {
         this.depthTest = test;
         this.depthWrite = write;
         return this;
      }

      public Pipeline cull(boolean cull) {
         this.cull = cull;
         return this;
      }

      public Pipeline vertices(VertexFormat format, Topology topology) {
         this.format = format;
         this.topology = topology;
         return this;
      }

      /** No vertex buffer: the vertex shader makes a full-screen triangle from gl_VertexID (core/screenquad). */
      public Pipeline fullScreen() {
         this.format = null;
         this.topology = Topology.TRIANGLES;
         return this;
      }

      public RenderPipeline build() {
         //? if >=26.3 {
         /*RenderPipeline.Builder b = this.matrices
            ? RenderPipeline.builder()
               .withBindGroupLayout(BindGroupLayouts.GLOBALS)
               .withBindGroupLayout(BindGroupLayouts.PROJECTION)
               .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            : RenderPipeline.builder();
         b.withLocation(this.location).withVertexShader(this.vertex).withFragmentShader(this.fragment);
         if (!this.samplers.isEmpty() || !this.uniforms.isEmpty()) {
            BindGroupLayout.Builder layout = BindGroupLayout.builder();
            // 26.3: samplers are uniforms of the bind group layout.
            this.samplers.forEach(s -> layout.withUniform(s, UniformType.COMBINED_IMAGE_SAMPLER));
            this.uniforms.forEach(u -> layout.withUniform(u, UniformType.UNIFORM_BUFFER));
            b.withBindGroupLayout(layout.build());
         }
         b.withColorTargetState(new ColorTargetState(
            Optional.ofNullable(this.blend), GpuFormat.RGBA8_UNORM, this.writeAlpha ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_COLOR
         ));
         if (this.format != null) {
            // 26.x renders with reversed depth: "nearer or equal" is GREATER_THAN_OR_EQUAL.
            b.withDepthStencilState(new DepthStencilState(this.depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, this.depthWrite));
            b.withVertexBinding(0, this.format);
         }
         b.withCull(this.cull);
         b.withPrimitiveTopology(this.topology == Topology.QUADS ? PrimitiveTopology.QUADS : PrimitiveTopology.TRIANGLES);
         return b.build();
         *///?} elif >=26.2 {
         RenderPipeline.Builder b = this.matrices
            ? RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET).withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            : RenderPipeline.builder();
         b.withLocation(this.location).withVertexShader(this.vertex).withFragmentShader(this.fragment);
         if (!this.samplers.isEmpty() || !this.uniforms.isEmpty()) {
            BindGroupLayout.Builder layout = BindGroupLayout.builder();
            this.samplers.forEach(layout::withSampler);
            this.uniforms.forEach(u -> layout.withUniform(u, UniformType.UNIFORM_BUFFER));
            b.withBindGroupLayout(layout.build());
         }
         b.withColorTargetState(new ColorTargetState(
            Optional.ofNullable(this.blend), GpuFormat.RGBA8_UNORM, this.writeAlpha ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_COLOR
         ));
         if (this.format != null) {
            // 26.x renders with reversed depth: "nearer or equal" is GREATER_THAN_OR_EQUAL.
            b.withDepthStencilState(new DepthStencilState(this.depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, this.depthWrite));
            b.withVertexBinding(0, this.format);
         }
         b.withCull(this.cull);
         b.withPrimitiveTopology(this.topology == Topology.QUADS ? PrimitiveTopology.QUADS : PrimitiveTopology.TRIANGLES);
         return b.build();
         //?} else {
         /*RenderPipeline.Builder b = this.matrices
            ? RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            : RenderPipeline.builder();
         b.withLocation(this.location).withVertexShader(this.vertex).withFragmentShader(this.fragment);
         this.samplers.forEach(b::withSampler);
         this.uniforms.forEach(u -> b.withUniform(u, UniformType.UNIFORM_BUFFER));
         if (this.blend == null) {
            b.withoutBlend();
         } else {
            b.withBlend(this.blend);
         }
         if (!this.writeAlpha) {
            b.withColorWrite(true, false);
         }
         b.withDepthWrite(this.depthWrite);
         b.withDepthTestFunction(this.depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST);
         b.withCull(this.cull);
         b.withVertexFormat(
            this.format == null ? DefaultVertexFormat.EMPTY : this.format,
            this.topology == Topology.QUADS ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES
         );
         return b.build();
         *///?}
      }
   }

   //? if >=26.3 {
   /*// Sets a pipeline on the pass (26.3 compiles it on first use and caches it).
   public static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
      pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
   }

   // Binds a texture to a sampler uniform.
   public static void bindTexture(RenderPass pass, String name, GpuTextureView view, com.mojang.blaze3d.textures.GpuSampler sampler) {
      pass.setUniform(name, view, sampler);
   }

   // Whether the pipeline's shaders compiled (compiles it now if needed).
   public static boolean compiled(RenderPipeline pipeline) {
      return RenderSystem.getCompiledPipelineNullable(pipeline) != null;
   }

   // The pipeline's fragment shader, for log messages.
   public static Identifier fragmentShader(RenderPipeline pipeline) {
      return pipeline.getShaders().get(com.mojang.renderpearl.api.pipeline.ShaderType.FRAGMENT);
   }

   *///?}
   public static CommandEncoder encoder() {
      return RenderSystem.getDevice().createCommandEncoder();
   }

   /** A render pass that keeps the target's contents (no clear); {@code depth} may be null. */
   public static RenderPass pass(Supplier<String> label, GpuTextureView color, @Nullable GpuTextureView depth) {
      return pass(encoder(), label, color, depth);
   }

   public static RenderPass pass(CommandEncoder encoder, Supplier<String> label, GpuTextureView color, @Nullable GpuTextureView depth) {
      //? if >=26.2 {
      return depth == null
         ? encoder.createRenderPass(label, color, Optional.empty())
         : encoder.createRenderPass(label, color, Optional.empty(), depth, OptionalDouble.empty());
      //?} else {
      /*return depth == null
         ? encoder.createRenderPass(label, color, OptionalInt.empty())
         : encoder.createRenderPass(label, color, OptionalInt.empty(), depth, OptionalDouble.empty());
      *///?}
   }

   /** A mesh uploaded for this frame, with the shared sequential index buffer for its topology. */
   public static final class Mesh {
      //? if >=26.2 {
      private final GpuBufferSlice vertices;
      private final com.mojang.blaze3d.IndexType indexType;
      //?} else {
      /*private final GpuBuffer vertices;
      private final VertexFormat.IndexType indexType;
      *///?}
      private final GpuBuffer indices;
      public final int indexCount;

      private Mesh(MeshData mesh) {
         MeshData.DrawState state = mesh.drawState();
         this.indexCount = state.indexCount();
         //? if >=26.2 {
         this.vertices = encoder().transientMemory().uploadGpu(mesh.vertexBuffer(), state.format().getVertexSize(), GpuBuffer.USAGE_VERTEX);
         RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(state.primitiveTopology());
         //?} else {
         /*this.vertices = state.format().uploadImmediateVertexBuffer(mesh.vertexBuffer());
         RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(state.mode());
         *///?}
         this.indices = sequential.getBuffer(this.indexCount);
         this.indexType = sequential.type();
      }

      /** Binds the vertex and index buffers (again after each pipeline change). */
      public void bind(RenderPass pass) {
         pass.setVertexBuffer(0, this.vertices);
         pass.setIndexBuffer(this.indices, this.indexType);
      }

      /** Binds and draws the whole mesh. */
      public void drawAll(RenderPass pass) {
         this.bind(pass);
         this.draw(pass, 0, this.indexCount);
      }

      public void draw(RenderPass pass, int firstIndex, int count) {
         //? if >=26.2 {
         pass.drawIndexed(count, 1, firstIndex, 0, 0);
         //?} else {
         /*pass.drawIndexed(0, firstIndex, count, 1);
         *///?}
      }
   }

   /** Uploads the mesh; do this before opening the render pass that draws it (no uploads while a pass is open). */
   public static Mesh upload(MeshData mesh) {
      return new Mesh(mesh);
   }

   /** Draws {@code indexCount} indices of quads whose vertices sit at the start of a persistent vertex buffer. */
   public static void drawQuads(RenderPass pass, GpuBuffer vertices, int indexCount) {
      //? if >=26.2 {
      RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
      pass.setVertexBuffer(0, vertices.slice());
      pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
      pass.drawIndexed(indexCount, 1, 0, 0, 0);
      //?} else {
      /*RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      pass.setVertexBuffer(0, vertices);
      pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
      pass.drawIndexed(0, 0, indexCount, 1);
      *///?}
   }

   /** Draws the full-screen triangle of a {@link Pipeline#fullScreen()} pipeline. */
   public static void drawFullScreen(RenderPass pass) {
      //? if >=26.2 {
      pass.draw(3, 1, 0, 0);
      //?} else {
      /*pass.draw(0, 3);
      *///?}
   }

   public static BufferBuilder builder(ByteBufferBuilder bytes, VertexFormat format, Topology topology) {
      //? if >=26.2 {
      return new BufferBuilder(bytes, topology == Topology.QUADS ? PrimitiveTopology.QUADS : PrimitiveTopology.TRIANGLES, format);
      //?} else {
      /*return new BufferBuilder(bytes, topology == Topology.QUADS ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES, format);
      *///?}
   }

   /** A 2D RGBA8 texture, or a 32-bit depth texture when {@code depth}. */
   public static GpuTexture texture(Supplier<String> label, int usage, boolean depth, int width, int height, int mipLevels) {
      //? if >=26.2 {
      return RenderSystem.getDevice().createTexture(label, usage, depth ? GpuFormat.D32_FLOAT : GpuFormat.RGBA8_UNORM, width, height, 1, mipLevels);
      //?} else {
      /*return RenderSystem.getDevice().createTexture(label, usage, depth ? TextureFormat.DEPTH32 : TextureFormat.RGBA8, width, height, 1, mipLevels);
      *///?}
   }

   /** Writes the whole image into mip level {@code mipLevel} at (x, y). */
   public static void write(CommandEncoder encoder, GpuTexture texture, NativeImage image, int mipLevel, int x, int y) {
      //? if >=26.2 {
      encoder.writeToTexture(texture, image, mipLevel, 0, x, y);
      //?} else {
      /*encoder.writeToTexture(texture, image, mipLevel, 0, x, y, image.getWidth(), image.getHeight(), 0, 0);
      *///?}
   }

   /** Writes tightly packed RGBA bytes into a rectangle of mip level 0. */
   public static void writeRgba(CommandEncoder encoder, GpuTexture texture, ByteBuffer rgba, int x, int y, int width, int height) {
      //? if >=26.2 {
      encoder.writeToTexture(texture, rgba, 0, 0, x, y, width, height);
      //?} else {
      /*encoder.writeToTexture(texture, rgba, NativeImage.Format.RGBA, 0, 0, x, y, width, height);
      *///?}
   }

   /** Clears a colour texture to transparent black and a depth texture to "far" (1.0, or 0.0 with 26.x's reversed depth). */
   public static void clearColorAndDepth(CommandEncoder encoder, GpuTexture color, GpuTexture depth) {
      //? if >=26.2 {
      encoder.clearColorAndDepthTextures(color, new Vector4f(0.0F), depth, RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE);
      //?} else {
      /*encoder.clearColorAndDepthTextures(color, 0, depth, 1.0);
      *///?}
   }

   /** Like {@link #clearColorAndDepth(CommandEncoder, GpuTexture, GpuTexture)} for a rectangle (GL window coordinates). */
   public static void clearColorAndDepth(CommandEncoder encoder, GpuTexture color, GpuTexture depth, int x, int y, int width, int height) {
      //? if >=26.3 {
      /*encoder.clearColorAndDepthTextures(color, new Vector4f(0.0F), depth, RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE, x, y, width, height, 0);
      *///?} elif >=26.2 {
      encoder.clearColorAndDepthTextures(color, new Vector4f(0.0F), depth, RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE, x, y, width, height);
      //?} else {
      /*encoder.clearColorAndDepthTextures(color, 0, depth, 1.0, x, y, width, height);
      *///?}
   }

   /** A mapped range of a GPU buffer; close it when done. */
   public static final class Mapped implements AutoCloseable {
      //? if >=26.2 {
      private final GpuBufferSlice.MappedView view;

      private Mapped(GpuBufferSlice.MappedView view) {
         this.view = view;
      }
      //?} else {
      /*private final GpuBuffer.MappedView view;

      private Mapped(GpuBuffer.MappedView view) {
         this.view = view;
      }
      *///?}

      public ByteBuffer data() {
         return this.view.data();
      }

      @Override
      public void close() {
         this.view.close();
      }
   }

   public static Mapped map(CommandEncoder encoder, GpuBufferSlice slice, boolean read, boolean write) {
      //? if >=26.2 {
      return new Mapped(slice.map(read, write));
      //?} else {
      /*return new Mapped(encoder.mapBuffer(slice, read, write));
      *///?}
   }

   /** An RGBA8 off-screen target. */
   public static TextureTarget target(String label, int width, int height, boolean depth) {
      //? if >=26.3 {
      /*return new TextureTarget(label, width, height, GpuFormat.RGBA8_UNORM, depth ? GpuFormat.D32_FLOAT : null);
      *///?} elif >=26.2 {
      return new TextureTarget(label, width, height, depth, GpuFormat.RGBA8_UNORM);
      //?} else {
      /*return new TextureTarget(label, width, height, depth);
      *///?}
   }
}
