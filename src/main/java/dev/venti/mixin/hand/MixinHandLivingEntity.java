package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.hand.SwingAnimationModule;
import dev.venti.modules.hand.SwingSpeedModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swing Animation (tempo + combat latch) and Slow Swing (duration) on the local player's hand swing. */
@Mixin(LivingEntity.class)
public abstract class MixinHandLivingEntity {
   @Inject(method = "getCurrentSwingDuration()I", at = @At("RETURN"), cancellable = true)
   private void venti$slowSwing(CallbackInfoReturnable<Integer> cir) {
      SwingSpeedModule module = SwingSpeedModule.active();
      if (module != null && (Object)this == Minecraft.getInstance().player) {
         cir.setReturnValue(module.getModifiedSwingDuration(cir.getReturnValue()));
      }
   }

   @ModifyExpressionValue(
      method = {"updateSwingTime()V", "swing(Lnet/minecraft/world/InteractionHand;Z)V"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I")
   )
   private int venti$swingTempo(int ticks) {
      SwingAnimationModule module = SwingAnimationModule.active;
      return module != null && (Object)this == Minecraft.getInstance().player ? module.swingTicks(ticks) : ticks;
   }

   @Inject(
      method = "swing(Lnet/minecraft/world/InteractionHand;Z)V",
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;swinging:Z", opcode = Opcodes.PUTFIELD)
   )
   private void venti$latchSwing(InteractionHand hand, boolean broadcast, CallbackInfo ci) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && (Object)this == Minecraft.getInstance().player) {
         module.latch();
      }
   }
}
