package dev.venti.mixin.world;

import dev.venti.modules.world.MotionBlurModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinMotionBlurGameRenderer {
   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/fog/FogRenderer;rotate()V"
      )}
   )
   private void venti$motionBlur(RenderTickCounter deltaTracker, boolean renderLevel, CallbackInfo ci) {
      if (MotionBlurModule.active != null) {
         MotionBlurModule.afterWorld(renderLevel);
      }
   }
}
