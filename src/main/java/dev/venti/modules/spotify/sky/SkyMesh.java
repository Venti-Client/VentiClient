package dev.venti.modules.spotify.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.compat.Gpu;
import dev.venti.compat.Mc;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
final class SkyMesh {
   static final RenderPipeline GLOW = pipeline("sky_glow", BlendFunction.LIGHTNING);
   static final RenderPipeline PAINT = pipeline("sky_paint", null);
   private static final int VERTEX_SIZE = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
   private final String label;
   private final RenderPipeline pipeline;
   private final int maxQuads;
   private ByteBufferBuilder bytes;
   private GpuBuffer buffer;
   private int indexCount;
   private boolean built;

   SkyMesh(String label, RenderPipeline pipeline, int maxQuads) {
      this.label = label;
      this.pipeline = pipeline;
      this.maxQuads = maxQuads;
   }

   private static RenderPipeline pipeline(String name, BlendFunction blend) {
      return Gpu.pipeline("" + name)
         .shaders("core/position_color", "core/position_color")
         .matrices()
         .blend(blend)
         .depth(true, false)
         .cull(false)
         .vertices(DefaultVertexFormat.POSITION_COLOR, Gpu.Topology.QUADS)
         .build();
   }

   BufferBuilder begin() {
      if (this.bytes == null) {
         this.bytes = new ByteBufferBuilder(this.maxQuads * 4 * VERTEX_SIZE);
      }

      return Gpu.builder(this.bytes, DefaultVertexFormat.POSITION_COLOR, Gpu.Topology.QUADS);
   }

   void upload(BufferBuilder builder) {
      this.built = true;

      try (MeshData mesh = builder.build()) {
         if (mesh == null) {
            this.indexCount = 0;
            return;
         }

         if (this.buffer == null) {
            this.buffer = RenderSystem.getDevice()
               .createBuffer(() -> this.label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, this.maxQuads * 4L * VERTEX_SIZE);
         }

         Gpu.encoder().writeToBuffer(this.buffer.slice(0L, mesh.vertexBuffer().remaining()), mesh.vertexBuffer());
         this.indexCount = mesh.drawState().indexCount();
      }
   }

   boolean isBuilt() {
      return this.built;
   }

   void draw(PoseStack pose, float alpha) {
      if (this.indexCount != 0 && !(alpha <= 0.002F)) {
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix();

         try {
            modelView.mul(pose.last().pose());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
               .writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
            RenderTarget target = Mc.mainTarget();

            try (RenderPass pass = Gpu.pass(() -> this.label, target.getColorTextureView(), target.getDepthTextureView())) {
               //? if >=26.3 {
               /*Gpu.setPipeline(pass, this.pipeline);
               *///?} else {
               pass.setPipeline(this.pipeline);
               //?}
               RenderSystem.bindDefaultUniforms(pass);
               pass.setUniform("DynamicTransforms", transforms);
               Gpu.drawQuads(pass, this.buffer, this.indexCount);
            }
         } finally {
            modelView.popMatrix();
         }
      }
   }

   void close() {
      if (this.buffer != null) {
         this.buffer.close();
      }

      if (this.bytes != null) {
         this.bytes.close();
      }

      this.buffer = null;
      this.bytes = null;
      this.indexCount = 0;
      this.built = false;
   }

   static void vertex(BufferBuilder builder, float x, float y, float z, int argb) {
      builder.addVertex(x, y, z).setColor(argb);
   }
}
