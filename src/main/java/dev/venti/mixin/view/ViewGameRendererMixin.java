package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.venti.modules.view.CustomFovModule;
import dev.venti.modules.view.NoRenderModule;
import dev.venti.modules.view.ZoomModule;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom Fov and Zoom (FOV); No Render (totem animation, nausea/portal wobble). */
@Mixin(GameRenderer.class)
public abstract class ViewGameRendererMixin {
   // Custom Fov and Zoom: 26.x moved the FOV into Camera (ViewCameraMixin).
   //? if <26.2 {
   /*@Shadow
   public abstract boolean isPanoramicMode();

   @ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"))
   private float venti$fov(float original, @Local(argsOnly = true) boolean changingFov) {
      if (this.isPanoramicMode()) {
         return original;
      }

      CustomFovModule module = CustomFovModule.active;
      float fov = module != null ? module.fov() : original;
      if (!changingFov) {
         CustomFovModule.hudFovApplied(original, fov);
      }

      return changingFov ? ZoomModule.fov(fov) : fov;
   }
   *///?}

   @Inject(method = "displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private void venti$hideTotemAnimation(ItemStack stack, CallbackInfo ci) {
      if (NoRenderModule.hideTotemAnimation()) {
         ci.cancel();
      }
   }

   @WrapOperation(
      method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
      at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
   )
   private float venti$hideNauseaWobble(float portal, float nausea, Operation<Float> original) {
      return NoRenderModule.hideNauseaPortal() ? 0.0F : original.call(portal, nausea);
   }
}
