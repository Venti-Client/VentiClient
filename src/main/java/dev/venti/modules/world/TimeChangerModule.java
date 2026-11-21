package dev.venti.modules.world;

import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Environment(EnvType.CLIENT)
public final class TimeChangerModule extends Module {
   public static volatile TimeChangerModule active;
   private final NumberSetting time = this.add("time", new NumberSetting("Time", 0.0F, -20000.0F, 20000.0F).step(1.0F));
   private ClientLevel trackedLevel;
   private long serverDayTime;
   private long appliedDayTime;

   public TimeChangerModule() {
      super("time-changer", "Time Changer", "Makes you able to set a custom time.", ModuleCategory.VISUALS);
   }

   @Override
   protected void onEnable() {
      active = this;
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         this.applyForcedTime(level);
      }
   }

   @Override
   protected void onDisable() {
      active = null;
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level == this.trackedLevel) {
         writeDayTime(level, this.currentServerDayTime(level));
      }

      this.trackedLevel = null;
   }

   @Override
   protected void onTick() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         this.trackedLevel = null;
      } else {
         this.applyForcedTime(level);
      }
   }

   //? if >=26.2 {
   // 26.x keeps time in world clocks: MixinTimeChangerClientLevel makes the client clock report the forced
   // time while the module is on, so here we only track the level and drop cached sky/light attributes.
   private void applyForcedTime(ClientLevel level) {
      this.trackedLevel = level;
      long forced = this.forcedDayTime();
      if (forced != this.appliedDayTime) {
         this.appliedDayTime = forced;
         level.environmentAttributes().invalidateTickCache();
      }
   }

   private static void writeDayTime(ClientLevel level, long dayTime) {
      level.environmentAttributes().invalidateTickCache();
   }

   private long currentServerDayTime(ClientLevel level) {
      return level.getOverworldClockTime();
   }

   /** The time shown instead of the clock's own while active. */
   public long shownDayTime() {
      return this.forcedDayTime();
   }
   //?} else {
   /*private void applyForcedTime(ClientLevel level) {
      this.serverDayTime = level == this.trackedLevel ? this.currentServerDayTime(level) : level.getDayTime();
      this.trackedLevel = level;
      this.appliedDayTime = this.forcedDayTime();
      writeDayTime(level, this.appliedDayTime);
   }

   private static void writeDayTime(ClientLevel level, long dayTime) {
      if (level.getDayTime() != dayTime) {
         level.getLevelData().setDayTime(dayTime);
         level.environmentAttributes().invalidateTickCache();
      }
   }

   private long currentServerDayTime(ClientLevel level) {
      return this.serverDayTime + (level.getDayTime() - this.appliedDayTime);
   }

   public long interceptServerDayTime(ClientLevel level, long dayTime) {
      this.trackedLevel = level;
      this.serverDayTime = dayTime;
      this.appliedDayTime = this.forcedDayTime();
      return this.appliedDayTime;
   }
   *///?}

   private long forcedDayTime() {
      return (long)this.time.get().floatValue();
   }
}
