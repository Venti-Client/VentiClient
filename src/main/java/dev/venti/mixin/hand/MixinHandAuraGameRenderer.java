//? if >=26.2 {
package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.hand.HandGlowModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Hand Aura (26.x): GameRenderer draws the first-person hand pass; capture it into Hand Aura's own target,
// then composite it back with a glow. Up to 1.21.11 this is in MixinHandHeldItemRenderer. Listed by VentiMixinPlugin.
@Mixin(GameRenderer.class)
public abstract class MixinHandAuraGameRenderer {
   @WrapOperation(
      method = "renderItemInHand",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
      )
   )
   private void venti$handAura(FeatureRenderDispatcher features, SubmitNodeStorage submits, Operation<Void> original) {
      if (HandGlowModule.active != null && HandGlowModule.beginCapture()) {
         boolean drawn = false;
         try {
            original.call(features, submits);
            drawn = true;
         } finally {
            HandGlowModule.endCapture(drawn);
         }
      } else {
         original.call(features, submits);
      }
   }
}
//?}
