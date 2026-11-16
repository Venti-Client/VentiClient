package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.FullbrightModule;
import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fullbright (lightmap gamma) and No Render (darkness pulse). */
@Mixin(LightmapTextureManager.class)
public abstract class ViewLightmapMixin {
   @ModifyExpressionValue(
      method = "update(F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
      )
   )
   private float venti$hideDarknessPulse(float factor) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : factor;
   }

   @ModifyArg(
      method = "update(F)V",
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
