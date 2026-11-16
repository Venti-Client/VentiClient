package dev.venti.mixin.noinvleak;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.noinvleak.NoInvLeakModule;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The inventory's player model is drawn without the items it holds or wears. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenModelMixin {
   @WrapOperation(
      method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/client/render/entity/state/EntityRenderState;"
      )
   )
   private static EntityRenderState venti$noInvLeakModel(LivingEntity entity, Operation<EntityRenderState> original) {
      if (!NoInvLeakModule.hidesModelOf(entity)) {
         return original.call(entity);
      }

      NoInvLeakModule.bareModel(true);
      try {
         return original.call(entity);
      } finally {
         NoInvLeakModule.bareModel(false);
      }
   }
}
