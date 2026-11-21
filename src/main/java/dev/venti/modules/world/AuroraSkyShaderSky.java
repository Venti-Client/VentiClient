package dev.venti.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class AuroraSkyShaderSky {
   static final String OFF = "Off";
   static final String SPACE = "Space";
   static final String NORTHERN_LIGHTS = "Northern Lights";
   static final String RINGED_PLANET = "Ringed Planet";
   static final String BLACK_HOLE = "Black Hole";
   private static final List<String> THEMES = List.of("Space", "Northern Lights", "Ringed Planet", "Black Hole");
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
      .withLocation(Identifier.of("venti", "pipeline/sky_theme"))
      .withVertexShader(Identifier.of("venti", "core/sky_theme"))
      .withFragmentShader(Identifier.of("venti", "core/sky_theme"))
      .withUniform("VentiSky", UniformType.UNIFORM_BUFFER)
      .withDepthWrite(false)
      .withCull(false)
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .build();
   private static final int UNIFORM_SIZE = 112;
   private static final float CUBE = 10.0F;
   private static final int CUBE_INDICES = 36;
   private static final float MAX_STEP = 0.1F;
   private static final float TIME_WRAP = 4096.0F;
   private GpuBuffer cube;
   private GpuBuffer uniforms;
   private ByteBuffer uniformData;
   private boolean invalidLogged;
   private float time;
   private long lastNanos;

   boolean render(
      String theme,
      float visibility,
      float brightness,
      float speed,
      float stars,
      float effect,
      float band,
      int low,
      int high,
      int bandColor,
      int zenith,
      int horizon
   ) {
      int mode = THEMES.indexOf(theme);
      long now = System.nanoTime();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min((float)(now - this.lastNanos) / 1.0E9F, 0.1F);
      this.lastNanos = now;
      this.time = (this.time + dt * speed) % 4096.0F;
      if (mode >= 0 && !(visibility <= 0.002F)) {
         GpuDevice device = RenderSystem.getDevice();
         if (!device.precompilePipeline(PIPELINE).isValid()) {
            if (!this.invalidLogged) {
               this.invalidLogged = true;
               LOGGER.warn("[Venti] Aurora Sky sky shader {} did not compile; using the gradient sky", PIPELINE.getFragmentShader());
            }

            return false;
         } else {
            Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
            if (target.textureWidth > 0 && target.textureHeight > 0) {
               if (this.cube == null) {
                  this.cube = buildCube(device);
               }

               if (this.uniforms == null) {
                  this.uniforms = device.createBuffer(() -> "Venti sky theme settings", 136, 112L);
                  this.uniformData = BufferUtils.createByteBuffer(112);
               }

               this.putVec(0, mode, visibility, this.time, brightness);
               this.putVec(16, stars, effect, band, 1.0F);
               this.putColor(32, low);
               this.putColor(48, high);
               this.putColor(64, bandColor);
               this.putColor(80, zenith);
               this.putColor(96, horizon);
               CommandEncoder encoder = device.createCommandEncoder();
               encoder.writeToBuffer(this.uniforms.slice(), this.uniformData);
               GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                  .write(RenderSystem.getModelViewStack(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
               ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
               RenderPass pass = encoder.createRenderPass(
                  () -> "Venti sky theme", target.getColorAttachmentView(), OptionalInt.empty(), target.getDepthAttachmentView(), OptionalDouble.empty()
               );

               try {
                  this.drawSky(pass, PIPELINE, transforms, indices);
               } catch (Throwable var26) {
                  if (pass != null) {
                     try {
                        pass.close();
                     } catch (Throwable var25) {
                        var26.addSuppressed(var25);
                     }
                  }

                  throw var26;
               }

               if (pass != null) {
                  pass.close();
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void drawSky(RenderPass pass, RenderPipeline pipeline, GpuBufferSlice transforms, ShapeIndexBuffer indices) {
      pass.setPipeline(pipeline);
      RenderSystem.bindDefaultUniforms(pass);
      pass.setUniform("DynamicTransforms", transforms);
      pass.setUniform("VentiSky", this.uniforms.slice());
      pass.setVertexBuffer(0, this.cube);
      pass.setIndexBuffer(indices.getIndexBuffer(36), indices.getIndexType());
      pass.drawIndexed(0, 0, 36, 1);
   }

   void close() {
      if (this.cube != null) {
         this.cube.close();
      }

      if (this.uniforms != null) {
         this.uniforms.close();
      }

      this.cube = null;
      this.uniforms = null;
      this.uniformData = null;
      this.lastNanos = 0L;
      this.invalidLogged = false;
   }

   private void putVec(int offset, float x, float y, float z, float w) {
      this.uniformData.putFloat(offset, x);
      this.uniformData.putFloat(offset + 4, y);
      this.uniformData.putFloat(offset + 8, z);
      this.uniformData.putFloat(offset + 12, w);
   }

   private void putColor(int offset, int rgb) {
      this.putVec(offset, (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
   }

   private static GpuBuffer buildCube(GpuDevice device) {
      BufferAllocator bytes = new BufferAllocator(24 * VertexFormats.POSITION.getVertexSize());

      GpuBuffer var5;
      try {
         BufferBuilder builder = new BufferBuilder(bytes, DrawMode.QUADS, VertexFormats.POSITION);
         float s = 10.0F;
         face(builder, s, -s, -s, s, s, -s, s, s, s, s, -s, s);
         face(builder, -s, -s, s, -s, s, s, -s, s, -s, -s, -s, -s);
         face(builder, -s, s, -s, -s, s, s, s, s, s, s, s, -s);
         face(builder, -s, -s, s, -s, -s, -s, s, -s, -s, s, -s, s);
         face(builder, s, -s, s, s, s, s, -s, s, s, -s, -s, s);
         face(builder, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s);
         BuiltBuffer mesh = builder.end();

         try {
            var5 = device.createBuffer(() -> "Venti sky theme cube", 32, mesh.getBuffer());
         } catch (Throwable var9) {
            if (mesh != null) {
               try {
                  mesh.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (mesh != null) {
            mesh.close();
         }
      } catch (Throwable var10) {
         try {
            bytes.close();
         } catch (Throwable var7) {
            var10.addSuppressed(var7);
         }

         throw var10;
      }

      bytes.close();
      return var5;
   }

   private static void face(
      BufferBuilder builder, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz
   ) {
      builder.vertex(ax, ay, az);
      builder.vertex(bx, by, bz);
      builder.vertex(cx, cy, cz);
      builder.vertex(dx, dy, dz);
   }
}
