package dev.venti.mixin.streamhider;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.streamhider.StreamHiderModule;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The inventory's player model is drawn without the items it holds or wears. */
//? if >=26.2 {
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenModelMixin {
   @WrapOperation(
      method = "extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
      )
   )
   private static EntityRenderState venti$noInvLeakModel(LivingEntity entity, Operation<EntityRenderState> original) {
      if (!StreamHiderModule.hidesModelOf(entity)) {
         return original.call(entity);
      }

      StreamHiderModule.bareModel(true);
      try {
         return original.call(entity);
      } finally {
         StreamHiderModule.bareModel(false);
      }
   }
}
//?} else {
/*@Mixin(InventoryScreen.class)
public abstract class InventoryScreenModelMixin {
   @WrapOperation(
      method = "renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
      )
   )
   private static EntityRenderState venti$noInvLeakModel(LivingEntity entity, Operation<EntityRenderState> original) {
      if (!StreamHiderModule.hidesModelOf(entity)) {
         return original.call(entity);
      }

      StreamHiderModule.bareModel(true);
      try {
         return original.call(entity);
      } finally {
         StreamHiderModule.bareModel(false);
      }
   }
}
*///?}
