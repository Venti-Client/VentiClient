//? if >=26.3 {
/*package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.venti.modules.hand.HandGlowModule;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Hand Aura (26.3): GameRenderer opens the first-person hand pass itself; while Hand Aura captures, the pass
// draws into Hand Aura's own target instead, which is composited back with a glow once the pass is done.
// Listed by VentiMixinPlugin.
@Mixin(GameRenderer.class)
public abstract class MixinHandAuraGameRenderer {
   @Unique
   private boolean venti$capturing;

   @Inject(method = "renderItemInHand", at = @At("HEAD"))
   private void venti$handAuraReset(CallbackInfo ci) {
      if (this.venti$capturing) {
         // The previous hand pass did not finish.
         this.venti$capturing = false;
         HandGlowModule.endCapture(false);
      }
   }

   @WrapOperation(
      method = "renderItemInHand",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;"
      )
   )
   private RenderPass venti$handAura(
      CommandEncoder encoder,
      Supplier<String> label,
      GpuTextureView color,
      Optional<Vector4fc> clearColor,
      GpuTextureView depth,
      OptionalDouble clearDepth,
      Operation<RenderPass> original
   ) {
      if (HandGlowModule.active != null && HandGlowModule.beginCapture()) {
         this.venti$capturing = true;
         return original.call(encoder, label, HandGlowModule.captureColor(), clearColor, HandGlowModule.captureDepth(), clearDepth);
      }

      return original.call(encoder, label, color, clearColor, depth, clearDepth);
   }

   @Inject(
      method = "renderItemInHand",
      at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;", remap = false)
   )
   private void venti$handAuraCompose(CallbackInfo ci) {
      if (this.venti$capturing) {
         this.venti$capturing = false;
         HandGlowModule.endCapture(true);
      }
   }
}
*///?} elif >=26.2 {
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
