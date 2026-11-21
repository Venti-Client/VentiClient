package dev.venti.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.world.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientLevel.class})
public abstract class MixinTimeChangerClientLevel {
   @WrapOperation(
      method = {"setTimeFromServer(JJZ)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;setDayTime(J)V"
      )}
   )
   private void venti$timeChangerServerDayTime(ClientLevelData levelData, long dayTime, Operation<Void> original) {
      TimeChangerModule timeChanger = TimeChangerModule.active;
      if (timeChanger == null) {
         original.call(levelData, dayTime);
      } else {
         ClientLevel level = (ClientLevel)(Object)this;
         long shown = levelData.getDayTime();
         long forced = timeChanger.interceptServerDayTime(level, dayTime);
         original.call(levelData, forced);
         if (forced != shown) {
            level.environmentAttributes().invalidateTickCache();
         }
      }
   }
}
