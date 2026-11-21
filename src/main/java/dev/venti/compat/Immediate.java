package dev.venti.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
//? if >=26.2 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
//?} else {
/*import net.minecraft.client.Minecraft;
*///?}

/**
 * Draws vertices with a {@link RenderType} right away, outside the feature system.
 * Up to 1.21.11 this is the client's immediate buffer source; 26.x removed it, so the vertices are built here
 * and drawn with the render type's prepared pipeline, textures and transforms.
 */
@Environment(EnvType.CLIENT)
public final class Immediate {
   //? if >=26.2 {
   private static ByteBufferBuilder bytes;
   private static BufferBuilder building;
   private static RenderType buildingType;
   //?}

   private Immediate() {
   }

   /** Starts (or continues) a batch for {@code type}; finish it with {@link #end(RenderType)}. */
   public static VertexConsumer begin(RenderType type) {
      //? if >=26.2 {
      if (buildingType != type) {
         end(buildingType);
         if (bytes == null) {
            bytes = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
         }

         building = new BufferBuilder(bytes, type.primitiveTopology(), type.format());
         buildingType = type;
      }

      return building;
      //?} else {
      /*return Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(type);
      *///?}
   }

   /** Draws what was added for {@code type} since {@link #begin(RenderType)}. */
   public static void end(RenderType type) {
      //? if >=26.2 {
      if (type == null || buildingType != type) {
         return;
      }

      BufferBuilder done = building;
      building = null;
      buildingType = null;
      try (MeshData mesh = done.build()) {
         if (mesh != null) {
            draw(type.prepare(), mesh);
         }
      }
      //?} else {
      /*Minecraft.getInstance().renderBuffers().bufferSource().endBatch(type);
      *///?}
   }

   //? if >=26.2 {
   private static void draw(PreparedRenderType prepared, MeshData mesh) {
      Gpu.Mesh vertices = Gpu.upload(mesh);
      RenderTarget target = prepared.outputTarget().getRenderTarget();
      try (RenderPass pass = Gpu.pass(
         () -> "Venti immediate " + prepared.pipeline(), target.getColorTextureView(), target.useDepth ? target.getDepthTextureView() : null
      )) {
         pass.setPipeline(prepared.pipeline());
         if (prepared.scissorState().enabled()) {
            pass.enableScissor(prepared.scissorState().x(), prepared.scissorState().y(), prepared.scissorState().width(), prepared.scissorState().height());
         }

         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", prepared.dynamicTransforms());
         for (PreparedRenderType.Texture texture : prepared.textures()) {
            pass.bindTexture(texture.name(), texture.textureView(), texture.sampler());
         }

         vertices.drawAll(pass);
      }
   }
   //?}
}
