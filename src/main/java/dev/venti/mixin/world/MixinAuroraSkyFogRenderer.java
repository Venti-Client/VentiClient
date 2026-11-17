package dev.venti.mixin.world;

import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class MixinAuroraSkyFogRenderer {
   @Inject(
      method = {"getFogColor"},
      at = {@At("RETURN")}
   )
   private void venti$fogColour(
      //? if >=1.21.11 {
      Camera camera, float partialTick, ClientWorld level, int renderDistance, float darkness, CallbackInfoReturnable<Vector4f> cir
      //?} else {
      /*Camera camera, float partialTick, ClientWorld level, int renderDistance, float darkness, boolean thick, CallbackInfoReturnable<Vector4f> cir
      *///?}
   ) {
      CameraSubmersionType fluid = camera.getSubmersionType();
      if (fluid == CameraSubmersionType.NONE && !(darkness > 0.0F)) {
         if (!(camera.getFocusedEntity() instanceof LivingEntity living && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS)))
            )
          {
            AuroraSkyModule atmosphere = AuroraSkyModule.get();
            if (atmosphere != null) {
               atmosphere.fog(camera, level, (Vector4f)cir.getReturnValue());
            }

         }
      }
   }
}
