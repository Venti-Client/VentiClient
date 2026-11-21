package dev.venti.module;

import dev.venti.config.BooleanSetting;
import dev.venti.config.Setting;
import dev.venti.config.Settings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A toggleable feature with its own settings page in the click GUI.
 * Subclasses add settings in their constructor, then react in {@link #onEnable()},
 * {@link #onDisable()} and {@link #onTick()}.
 */
public abstract class Module {
   protected static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private final String id;
   private final String name;
   private final String description;
   private final ModuleCategory category;
   private final Settings settings;
   private final BooleanSetting notifications;
   private boolean running;

   protected Module(String id, String name, String description, ModuleCategory category) {
      this(id, name, description, category, false);
   }

   protected Module(String id, String name, String description, ModuleCategory category, boolean enabledByDefault) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.category = category;
      this.settings = new Settings(id, enabledByDefault);
      this.notifications = this.settings.add("notifications", new BooleanSetting("Notifications", true));
   }

   protected final <S extends Setting<?>> S add(String key, S setting) {
      return this.settings.add(key, setting);
   }

   public final String id() {
      return this.id;
   }

   public final String name() {
      return this.name;
   }

   public final String description() {
      return this.description;
   }

   public final ModuleCategory category() {
      return this.category;
   }

   public final Settings settings() {
      return this.settings;
   }

   public final boolean isEnabled() {
      return this.settings.enabled;
   }

   public final void setEnabled(boolean enabled) {
      if (this.settings.enabled != enabled) {
         this.settings.enabled = enabled;
         this.settings.save();
         this.sync();
         if (this.notifications.get()) {
            this.chat(Component.literal(enabled ? "enabled" : "disabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
         }
      }
   }

   public final void toggle() {
      this.setEnabled(!this.settings.enabled);
   }

   /** Starts or stops the module to match the saved enabled flag. */
   final void sync() {
      if (this.settings.enabled && !this.running) {
         this.running = true;
         this.safely("enable", this::onEnable);
      } else if (!this.settings.enabled && this.running) {
         this.running = false;
         this.safely("disable", this::onDisable);
      }
   }

   /** Stops on game exit without touching the saved enabled flag. */
   final void shutdown() {
      if (this.running) {
         this.running = false;
         this.safely("shut down", this::onDisable);
      }
   }

   final void tick() {
      if (this.running) {
         this.safely("tick", this::onTick);
      }
   }

   private void safely(String what, Runnable step) {
      try {
         step.run();
      } catch (RuntimeException | LinkageError e) {
         LOGGER.error("[Venti] {} failed to {}", this.name, what, e);
      }
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   protected void onTick() {
   }

   protected final void chat(Component message) {
      Minecraft mc = Minecraft.getInstance();
      if (this.notifications.get() && mc.gui != null) {
         mc.gui.getChat().addMessage(
            Component.literal("[Venti] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(this.name + " ").withStyle(ChatFormatting.WHITE)).append(message)
         );
      }
   }
}
