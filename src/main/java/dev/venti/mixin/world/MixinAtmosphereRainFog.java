package dev.venti.mixin.world;

import dev.venti.modules.world.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
//? if >=1.21.11 {
import net.minecraft.client.render.Camera;
//?} else {
/*import net.minecraft.client.render.fog.FogData;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.Opcodes;
*///?}
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({AtmosphericFogModifier.class})
public abstract class MixinAtmosphereRainFog {
   @Shadow
   private float fogMultiplier;

   //? if >=1.21.11 {
   @Inject(
      method = {"method_76304(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("TAIL")}
   )
   private void venti$keepClearInRain(Camera camera, ClientWorld level, RenderTickCounter deltaTracker, CallbackInfo ci) {
   //?} else {
   /*// Before 1.21.11 the rain fog multiplier is updated inline in applyStartEndModifier: zero it right after the update.
   @Inject(
      method = {"applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/fog/AtmosphericFogModifier;fogMultiplier:F",
         opcode = Opcodes.PUTFIELD,
         shift = At.Shift.AFTER
      )}
   )
   private void venti$keepClearInRain(
      FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld level, float viewDistance, RenderTickCounter deltaTracker, CallbackInfo ci
   ) {
   *///?}
      AtmosphereModule atmosphere = AtmosphereModule.get();
      if (atmosphere != null && atmosphere.keepsSkyInRain()) {
         this.fogMultiplier = 0.0F;
      }
   }
}
