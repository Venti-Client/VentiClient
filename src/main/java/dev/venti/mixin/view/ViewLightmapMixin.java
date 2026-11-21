package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.FullbrightModule;
import dev.venti.modules.view.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//? if >=26.2 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
*///?}

/** Fullbright (lightmap gamma) and No Render (darkness pulse). */
//? if >=26.2 {
// 26.x: the darkness pulse and the brightness reach the lightmap through its render state; both are
// written into the uniform block in Lightmap.render (darkness scale 4th, brightness 6th).
@Mixin(Lightmap.class)
public abstract class ViewLightmapMixin {
   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 3
      ),
      index = 0
   )
   private float venti$hideDarknessPulse(float darknessScale) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : darknessScale;
   }

   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 5
      ),
      index = 0
   )
   private float venti$fullbrightGamma(float brightness) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         // Darkness also dims the brightness; without the pulse use the plain brightness option.
         brightness = Math.max(brightness, Minecraft.getInstance().options.gamma().get().floatValue());
      }

      return FullbrightModule.gamma(brightness);
   }
}
//?} else {
/*@Mixin(LightTexture.class)
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
*///?}
