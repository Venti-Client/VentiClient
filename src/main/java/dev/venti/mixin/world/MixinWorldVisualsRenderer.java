package dev.venti.mixin.world;

import dev.venti.modules.world.WorldHooks;
import dev.venti.modules.world.WorldQuads;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
//? if <1.21.11
//import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the world visual modules a per-frame 3D hook (where Anubis fired Render3DEvent) and flushes their quads at the end of the world pass. */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldVisualsRenderer {
   //? if >=1.21.11 {
   @ModifyArg(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/render/Frustum;DDDF)V"),
      index = 0
   )
   private Frustum venti$worldVisuals(Frustum frustum) {
      MinecraftClient mc = MinecraftClient.getInstance();
      WorldQuads.beginFrame(mc.gameRenderer.getCamera().getCameraPos());
      WorldHooks.render3D(mc.getRenderTickCounter().getTickProgress(false));
      return frustum;
   }
   //?} elif >=1.21.9 {
   /*// Before 1.21.11 the debug renderer runs inside the frame graph, so hook the same spot (end of extraction,
   // right before the frame graph is set up) by its profiler section.
   @Inject(
      method = "render",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=setupFrameGraph")
   )
   *///?} else {
   /*// Before 1.21.9 there is no extraction step: hook right before the frame graph is created (after section compiling).
   @Inject(
      method = "render",
      at = @At(value = "NEW", target = "()Lnet/minecraft/client/render/FrameGraphBuilder;")
   )
   *///?}
   //? if <1.21.11 {
   /*private void venti$worldVisuals(
      CallbackInfo ci, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      WorldQuads.beginFrame(mc.gameRenderer.getCamera().getCameraPos());
      WorldQuads.beginLines(camera, projection);
      WorldHooks.render3D(mc.getRenderTickCounter().getTickProgress(false));
   }
   *///?}

   @Inject(method = "render", at = @At("TAIL"))
   private void venti$worldVisualsQuads(
      CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation, @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      WorldQuads.render(viewRotation, projection);
   }
}
