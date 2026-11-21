package dev.venti.mixin.world;

import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.2 {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class MixinAuroraSkyFogRenderer {
   @Inject(
      method = {"computeFogColor"},
      at = {@At("RETURN")}
   )
   //? if >=26.2 {
   private void venti$fogColour(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkness, Vector4f color, CallbackInfo ci) {
   //?} else {
   /*private void venti$fogColour(
      Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      Vector4f color = cir.getReturnValue();
   *///?}
      FogType fluid = camera.getFluidInCamera();
      if (fluid == FogType.NONE && !(darkness > 0.0F)) {
         if (!(camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
            )
          {
            AuroraSkyModule atmosphere = AuroraSkyModule.get();
            if (atmosphere != null) {
               atmosphere.fog(camera, level, color);
            }

         }
      }
   }
}
