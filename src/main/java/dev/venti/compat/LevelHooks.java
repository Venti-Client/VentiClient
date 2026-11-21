//? if >=26.2 {
package dev.venti.compat;

import dev.venti.module.Modules;
import dev.venti.modules.waypoints.WaypointRenderer;
import dev.venti.modules.world.BlockOverlayModule;
import dev.venti.modules.world.JumpCirclesModule;
import dev.venti.modules.world.WorldHooks;
import dev.venti.modules.world.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;

// The world-render hooks for 26.x, where LevelRenderer was split into extraction and frame-graph passes.
// Up to 1.21.11 the same calls come from LevelRenderer mixins (MixinWorldVisualsRenderer, MixinWaypointsWorldRenderer,
// MixinBlockOverlayLevelRenderer, MixinJumpCirclesLevelRenderer).
@Environment(EnvType.CLIENT)
public final class LevelHooks {
   private LevelHooks() {
   }

   public static void register() {
      // Start of the main pass: capture the camera for waypoint labels and let the world modules build this frame's quads.
      LevelRenderEvents.START_MAIN.register(context -> {
         CameraRenderState camera = context.levelState().cameraRenderState;
         WaypointRenderer.capture(camera.viewRotationMatrix, camera.projectionMatrix, camera.pos);
         WorldQuads.beginFrame(camera.pos);
         WorldHooks.render3D(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
      });
      // End of the main pass: Jump Circles, then the world modules' quads.
      LevelRenderEvents.END_MAIN.register(context -> {
         CameraRenderState camera = context.levelState().cameraRenderState;
         if (JumpCirclesModule.active != null) {
            JumpCirclesModule.renderLevel(camera);
         }

         WorldQuads.render(camera.viewRotationMatrix, camera.projectionMatrix);
      });
      // Block Overlay replaces the vanilla block outline.
      LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
         BlockOverlayModule module = Modules.get(BlockOverlayModule.class);
         return module == null || !module.isEnabled();
      });
   }
}
//?}
