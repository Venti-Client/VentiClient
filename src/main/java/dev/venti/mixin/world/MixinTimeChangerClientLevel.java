package dev.venti.mixin.world;

import dev.venti.modules.world.TimeChangerModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinTimeChangerClientLevel {
   @WrapOperation(
      method = {"setTime(JJZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/world/ClientWorld$Properties;setTimeOfDay(J)V"
      )}
   )
   private void venti$timeChangerServerDayTime(Properties levelData, long dayTime, Operation<Void> original) {
      TimeChangerModule timeChanger = TimeChangerModule.active;
      if (timeChanger == null) {
         original.call(levelData, dayTime);
      } else {
         ClientWorld level = (ClientWorld)(Object)this;
         long shown = levelData.getTimeOfDay();
         long forced = timeChanger.interceptServerDayTime(level, dayTime);
         original.call(levelData, forced);
         //? if >=1.21.11 {
         if (forced != shown) {
            level.getEnvironmentAttributes().tick();
         }
         //?}
      }
   }
}
