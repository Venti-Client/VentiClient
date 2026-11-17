package dev.venti.mixin.hand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.hand.SwingSpeedModule;
import dev.venti.modules.hand.SwingAnimationModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swing Animation (tempo + combat latch) and Slow Swing (duration) on the local player's hand swing. */
@Mixin(LivingEntity.class)
public abstract class MixinHandLivingEntity {
   @Inject(method = "getHandSwingDuration()I", at = @At("RETURN"), cancellable = true)
   private void venti$slowSwing(CallbackInfoReturnable<Integer> cir) {
      SwingSpeedModule module = SwingSpeedModule.active();
      if (module != null && (Object)this == MinecraftClient.getInstance().player) {
         cir.setReturnValue(module.getModifiedSwingDuration(cir.getReturnValue()));
      }
   }

   @ModifyExpressionValue(
      method = {"tickHandSwing()V", "swingHand(Lnet/minecraft/util/Hand;Z)V"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getHandSwingDuration()I")
   )
   private int venti$swingTempo(int ticks) {
      SwingAnimationModule module = SwingAnimationModule.active;
      return module != null && (Object)this == MinecraftClient.getInstance().player ? module.swingTicks(ticks) : ticks;
   }

   @Inject(
      method = "swingHand(Lnet/minecraft/util/Hand;Z)V",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;handSwinging:Z", opcode = Opcodes.PUTFIELD)
   )
   private void venti$latchSwing(Hand hand, boolean broadcast, CallbackInfo ci) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && (Object)this == MinecraftClient.getInstance().player) {
         module.latch();
      }
   }
}
