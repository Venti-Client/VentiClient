package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.FullbrightModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fullbright (lightmap gamma) and No Render (darkness pulse). */
@Mixin(LightTexture.class)
public abstract class ViewLightmapMixin {
   @ModifyExpressionValue(
      method = "updateLightTexture(F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"
      )
   )
   private float venti$hideDarknessPulse(float factor) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : factor;
   }

   @ModifyArg(
      method = "updateLightTexture(F)V",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 6
      ),
      index = 0
   )
   private float venti$fullbrightGamma(float gamma) {
      return FullbrightModule.gamma(gamma);
   }
}
