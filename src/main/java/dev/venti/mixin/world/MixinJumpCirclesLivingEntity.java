package dev.venti.mixin.world;

import dev.venti.modules.world.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinJumpCirclesLivingEntity {
   @Inject(
      method = {"jump"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"
      )}
   )
   private void venti$jumpCircle(CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.onJump((LivingEntity)(Object)this);
      }
   }
}
