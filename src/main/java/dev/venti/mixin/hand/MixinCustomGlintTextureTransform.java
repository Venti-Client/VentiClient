package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.hand.CustomGlintModule;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Custom Glint: pattern size and scroll speed of the enchantment glint. */
@Mixin(TextureTransform.class)
public abstract class MixinCustomGlintTextureTransform {
   @ModifyVariable(method = "getGlintTransformation(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), argsOnly = true)
   private static float venti$glintPatternScale(float scale) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? scale : scale * module.uvScaleMultiplier();
   }

   @ModifyExpressionValue(
      method = "getGlintTransformation(F)Lorg/joml/Matrix4f;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J")
   )
   private static long venti$glintClock(long millis) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? millis : module.glintMillis(millis);
   }
}
