package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
}
