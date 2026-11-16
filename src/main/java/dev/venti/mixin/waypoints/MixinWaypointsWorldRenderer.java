package dev.venti.mixin.waypoints;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.venti.modules.waypoints.WaypointRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
//? if >=1.21.11 {
import net.minecraft.client.util.memory.ObjectAllocator;
//?} else {
/*import net.minecraft.client.util.ObjectAllocator;
*///?}
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps this frame's camera position, view rotation and projection so waypoint labels can be projected onto the HUD. */
@Mixin(WorldRenderer.class)
public abstract class MixinWaypointsWorldRenderer {
   @Inject(method = "render", at = @At("HEAD"))
   private void venti$waypointsCapture(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      //? if >=1.21.9 {
      Matrix4f projectionMatrix,
      Matrix4f matrix4f,
      GpuBufferSlice fog,
      //?} else {
      /*Matrix4f projectionMatrix,
      GpuBufferSlice fog,
      *///?}
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      WaypointRenderer.capture(positionMatrix, projectionMatrix, camera.getCameraPos());
   }
}
