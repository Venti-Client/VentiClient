package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.hand.CustomGlintModule;
//? if >=1.21.11 {
import net.minecraft.client.render.TextureTransform;
//?} else {
/*import net.minecraft.client.render.RenderPhase;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Custom Glint: pattern size and scroll speed of the enchantment glint. */
//? if >=1.21.11 {
@Mixin(TextureTransform.class)
//?} else {
/*// Before 1.21.11 the glint matrix is built in RenderPhase.setupGlintTexturing(float).
@Mixin(RenderPhase.class)
*///?}
public abstract class MixinCustomGlintTextureTransform {
   //? if >=1.21.11 {
   @ModifyVariable(method = "getGlintTransformation(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), argsOnly = true)
   //?} else {
   /*@ModifyVariable(method = "setupGlintTexturing(F)V", at = @At("HEAD"), argsOnly = true)
   *///?}
   private static float venti$glintPatternScale(float scale) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? scale : scale * module.uvScaleMultiplier();
   }

   @ModifyExpressionValue(
      //? if >=1.21.11 {
      method = "getGlintTransformation(F)Lorg/joml/Matrix4f;",
      //?} else {
      /*method = "setupGlintTexturing(F)V",
      *///?}
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J")
   )
   private static long venti$glintClock(long millis) {
      CustomGlintModule module = CustomGlintModule.active;
      return module == null ? millis : module.glintMillis(millis);
   }
}
