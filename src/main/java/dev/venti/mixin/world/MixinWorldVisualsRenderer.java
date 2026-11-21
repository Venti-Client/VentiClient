package dev.venti.mixin.world;

import dev.venti.modules.world.WorldHooks;
import dev.venti.modules.world.WorldQuads;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
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

   @Inject(method = "render", at = @At("TAIL"))
   private void venti$worldVisualsQuads(
      CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation, @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      WorldQuads.render(viewRotation, projection);
   }
}
