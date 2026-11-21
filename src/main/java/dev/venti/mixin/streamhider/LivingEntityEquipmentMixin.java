package dev.venti.mixin.streamhider;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.venti.modules.streamhider.StreamHiderModule;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While the inventory screen draws your own model, it is drawn without its held and worn items. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentMixin {
   @ModifyReturnValue(method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
   private ItemStack venti$noInvLeakBareModel(ItemStack stack) {
      return StreamHiderModule.modelItem((LivingEntity)(Object)this, stack);
   }
}
