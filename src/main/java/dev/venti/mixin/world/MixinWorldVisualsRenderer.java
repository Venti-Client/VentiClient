// 26.x renders the level through Fabric's LevelRenderEvents instead: see dev.venti.compat.LevelHooks.
//? if <26.2 {
/*package dev.venti.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.world.WorldHooks;
import dev.venti.modules.world.WorldQuads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Gives the world visual modules a per-frame 3D hook (where Anubis fired Render3DEvent) and flushes their quads at the end of the world pass.
@Mixin(LevelRenderer.class)
public abstract class MixinWorldVisualsRenderer {
   @ModifyArg(
      method = "renderLevel",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V"),
      index = 0
   )
   private Frustum venti$worldVisuals(Frustum frustum) {
      Minecraft mc = Minecraft.getInstance();
      WorldQuads.beginFrame(mc.gameRenderer.getMainCamera().position());
      WorldHooks.render3D(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
      return frustum;
   }

   @Inject(method = "renderLevel", at = @At("TAIL"))
   private void venti$worldVisualsQuads(
      CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) Matrix4f viewRotation, @Local(argsOnly = true, ordinal = 1) Matrix4f projection
   ) {
      WorldQuads.render(viewRotation, projection);
   }
}
*///?}
