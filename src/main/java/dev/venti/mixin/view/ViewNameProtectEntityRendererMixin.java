package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <1.21.9 {
/*import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

/** Name Protect: nametags above entities (also what the Nametags module shows). */
@Mixin(EntityRenderer.class)
public abstract class ViewNameProtectEntityRendererMixin {
   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/EntityRenderer;getDisplayName(Lnet/minecraft/entity/Entity;)Lnet/minecraft/text/Text;"
      )
   )
   private Text venti$protectNametag(Text original) {
      return NameProtectModule.protect(original);
   }
   //? if <1.21.9 {

   /*// No Render: hide fire on entities (WorldRenderer.getAndUpdateRenderState only exists from 1.21.9).
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
   *///?}
}
