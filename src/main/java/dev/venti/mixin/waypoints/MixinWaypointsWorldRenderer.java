// 26.x renders the level through Fabric's LevelRenderEvents instead: see dev.venti.compat.LevelHooks.
//? if <26.2 {
/*package dev.venti.mixin.waypoints;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.venti.modules.waypoints.WaypointRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keeps this frame's camera position, view rotation and projection so waypoint labels can be projected onto the HUD.
@Mixin(LevelRenderer.class)
public abstract class MixinWaypointsWorldRenderer {
   @Inject(method = "renderLevel", at = @At("HEAD"))
   private void venti$waypointsCapture(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f matrix4f,
      GpuBufferSlice fog,
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      WaypointRenderer.capture(positionMatrix, projectionMatrix, camera.position());
   }
}
*///?}
