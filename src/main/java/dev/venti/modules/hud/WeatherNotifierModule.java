package dev.venti.modules.hud;

import dev.venti.config.BooleanSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/** Shows an advancement-style toast when it starts raining (from the Anubis Weather Notifier). */
public final class WeatherNotifierModule extends Module {
   private static final int STABLE_TICKS = 20;
   private static final Text TITLE = Text.literal("Weather Changed!");
   private static final Text MESSAGE = Text.literal("It started raining.");
   private final BooleanSetting sound = this.add("sound", new BooleanSetting("Sound", true));
   private ClientWorld level;
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
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientWorld current = mc.world;
      if (current != this.level) {
         this.level = current;
         this.raining = false;
         this.changedTicks = 0;
      }

      if (current != null && !(mc.currentScreen instanceof LevelLoadingScreen)) {
         // The rain state must hold for a second before it counts, so a brief flicker does not toast.
         if (current.isRaining() == this.raining) {
            this.changedTicks = 0;
         } else if (++this.changedTicks >= STABLE_TICKS) {
            this.raining = !this.raining;
            this.changedTicks = 0;
            if (this.raining) {
               mc.getToastManager().add(new WeatherToast(new ItemStack(Items.TRIDENT), TITLE, MESSAGE, this.sound.get()));
            }
         }
      } else {
         this.changedTicks = 0;
      }
   }
}
