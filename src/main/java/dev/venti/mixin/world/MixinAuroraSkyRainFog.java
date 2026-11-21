package dev.venti.mixin.world;

import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({AtmosphericFogEnvironment.class})
public abstract class MixinAuroraSkyRainFog {
   @Shadow
   private float rainFogMultiplier;

   @Inject(
      method = {"updateRainFogState(Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("TAIL")}
   )
   private void venti$keepClearInRain(Camera camera, ClientLevel level, DeltaTracker deltaTracker, CallbackInfo ci) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      if (atmosphere != null && atmosphere.keepsSkyInRain()) {
         this.rainFogMultiplier = 0.0F;
      }
   }
}
