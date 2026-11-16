package dev.venti.mixin.noinvleak;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.venti.modules.noinvleak.NoInvLeakModule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While the inventory screen draws your own model, it is drawn without its held and worn items. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentMixin {
   @ModifyReturnValue(method = "getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
   private ItemStack venti$noInvLeakBareModel(ItemStack stack) {
      return NoInvLeakModule.modelItem((LivingEntity)(Object)this, stack);
   }
}
