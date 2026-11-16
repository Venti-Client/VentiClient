package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Render: keep the sky under blindness/darkness, and hide fire on entities. */
@Mixin(WorldRenderer.class)
public abstract class ViewWorldRendererMixin {
   @Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
   private void venti$keepSky(Camera camera, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         cir.setReturnValue(false);
      }
   }

   //? if >=1.21.9 {
   @Inject(
      method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
      at = @At("RETURN")
   )
   private void venti$hideEntityFire(Entity entity, float tickProgress, CallbackInfoReturnable<EntityRenderState> cir) {
      EntityRenderState state = cir.getReturnValue();
      if (state != null && NoRenderModule.hideEntityFire()) {
         state.onFire = false;
      }
   }
   //?}
   // (before 1.21.9 entity fire is hidden from ViewNameProtectEntityRendererMixin, on EntityRenderer.getAndUpdateRenderState)
}
