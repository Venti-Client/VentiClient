package dev.venti.modules.hud;

import dev.venti.compat.Mc;
import dev.venti.config.BooleanSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shows an advancement-style toast when it starts raining (from the Anubis Weather Notifier). */
public final class WeatherNotifierModule extends Module {
   private static final int STABLE_TICKS = 20;
   private static final Component TITLE = Component.literal("Weather Changed!");
   private static final Component MESSAGE = Component.literal("It started raining.");
   private final BooleanSetting sound = this.add("sound", new BooleanSetting("Sound", true));
   private ClientLevel level;
   private boolean raining;
   private int changedTicks;

   public WeatherNotifierModule() {
      super("weather-notifier", "Weather Notifier", "Shows an advancement-style toast when it starts raining.", ModuleCategory.MISC);
   }

   @Override
   protected void onEnable() {
      this.level = null;
      this.raining = false;
      this.changedTicks = 0;
   }

   @Override
   protected void onDisable() {
      this.level = null;
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel current = mc.level;
      if (current != this.level) {
         this.level = current;
         this.raining = false;
         this.changedTicks = 0;
      }

      if (current != null && !(Mc.screen() instanceof LevelLoadingScreen)) {
         // The rain state must hold for a second before it counts, so a brief flicker does not toast.
         if (current.isRaining() == this.raining) {
            this.changedTicks = 0;
         } else if (++this.changedTicks >= STABLE_TICKS) {
            this.raining = !this.raining;
            this.changedTicks = 0;
            if (this.raining) {
               Mc.toasts().addToast(new WeatherToast(new ItemStack(Items.TRIDENT), TITLE, MESSAGE, this.sound.get()));
            }
         }
      } else {
         this.changedTicks = 0;
      }
   }
}
