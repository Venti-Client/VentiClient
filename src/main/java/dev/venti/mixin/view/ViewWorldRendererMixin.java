package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Render: keep the sky under blindness/darkness, and hide fire on entities. */
@Mixin(LevelRenderer.class)
public abstract class ViewWorldRendererMixin {
   @Inject(method = "doesMobEffectBlockSky(Lnet/minecraft/client/Camera;)Z", at = @At("HEAD"), cancellable = true)
   private void venti$keepSky(Camera camera, CallbackInfoReturnable<Boolean> cir) {
      if (NoRenderModule.hideBlindnessDarkness()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
      at = @At("RETURN")
   )
   private void venti$hideEntityFire(Entity entity, float tickProgress, CallbackInfoReturnable<EntityRenderState> cir) {
      EntityRenderState state = cir.getReturnValue();
      if (state != null && NoRenderModule.hideEntityFire()) {
         state.displayFireAnimation = false;
      }
   }
   // (before 1.21.9 entity fire is hidden from ViewNameProtectEntityRendererMixin, on EntityRenderer.getAndUpdateRenderState)
}
