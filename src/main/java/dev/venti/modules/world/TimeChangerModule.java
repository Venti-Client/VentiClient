package dev.venti.modules.world;

import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.config.NumberSetting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class TimeChangerModule extends Module {
   public static volatile TimeChangerModule active;
   private final NumberSetting time = this.add("time", new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F).step(1.0F));
   private ClientWorld trackedLevel;
   private long serverDayTime;
   private long appliedDayTime;

   public TimeChangerModule() {
      super("time-changer", "Time Changer", "Makes you able to set a custom time.", ModuleCategory.VISUALS);
   }

   @Override
   protected void onEnable() {
      active = this;
      ClientWorld level = MinecraftClient.getInstance().world;
      if (level != null) {
         this.applyForcedTime(level);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      ClientWorld level = MinecraftClient.getInstance().world;
      if (level != null && level == this.trackedLevel) {
         writeDayTime(level, this.currentServerDayTime(level));
      }

      this.trackedLevel = null;
   }

   @Override
   protected void onTick() {
      ClientWorld level = MinecraftClient.getInstance().world;
      if (level == null) {
         this.trackedLevel = null;
      } else {
         this.applyForcedTime(level);
      }
   }

   private void applyForcedTime(ClientWorld level) {
      this.serverDayTime = level == this.trackedLevel ? this.currentServerDayTime(level) : level.getTimeOfDay();
      this.trackedLevel = level;
      this.appliedDayTime = this.forcedDayTime();
      writeDayTime(level, this.appliedDayTime);
   }

   private static void writeDayTime(ClientWorld level, long dayTime) {
      if (level.getTimeOfDay() != dayTime) {
         level.getLevelProperties().setTimeOfDay(dayTime);
         level.getEnvironmentAttributes().tick();
      }
   }

   private long currentServerDayTime(ClientWorld level) {
      return this.serverDayTime + (level.getTimeOfDay() - this.appliedDayTime);
   }

   public long interceptServerDayTime(ClientWorld level, long dayTime) {
      this.trackedLevel = level;
      this.serverDayTime = dayTime;
      this.appliedDayTime = this.forcedDayTime();
      return this.appliedDayTime;
   }

   private long forcedDayTime() {
      return (long)this.time.get().floatValue();
   }
}
