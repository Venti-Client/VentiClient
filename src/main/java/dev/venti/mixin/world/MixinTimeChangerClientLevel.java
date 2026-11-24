package dev.venti.mixin.world;

import dev.venti.modules.world.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.2 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientClockManager;
//?} else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientLevel;
*///?}

/** Time Changer: shows the chosen time of day instead of the server's. */
@Environment(EnvType.CLIENT)
//? if >=26.3 {
/*@Mixin({ClientClockManager.ClientClockInstance.class})
public abstract class MixinTimeChangerClientLevel {
   // 26.3: every client reader of a world clock (sky, light, moon) goes through ClientClockInstance.totalTicks.
   @ModifyReturnValue(method = "totalTicks()J", at = @At("RETURN"))
   private long venti$timeChangerClock(long ticks) {
      TimeChangerModule timeChanger = TimeChangerModule.active;
      return timeChanger == null ? ticks : timeChanger.shownDayTime();
   }
}
*///?} elif >=26.2 {
@Mixin({ClientClockManager.class})
public abstract class MixinTimeChangerClientLevel {
   // 26.x: every client reader of a world clock (sky, light, moon) goes through getTotalTicks.
   @ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
   private long venti$timeChangerClock(long ticks) {
      TimeChangerModule timeChanger = TimeChangerModule.active;
      return timeChanger == null ? ticks : timeChanger.shownDayTime();
   }
}
//?} else {
/*@Mixin({ClientLevel.class})
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
*///?}
