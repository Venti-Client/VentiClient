package dev.venti.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** One module's ordered settings, enabled flag, key bind and HUD placement, saved to config/venti/modules/&lt;id&gt;.json. */
public final class Settings {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final List<Setting<?>> all = new ArrayList<>();
   private final Path file;
   public boolean enabled;
   public int bind = -1;
   public float hudX = Float.NaN;
   public float hudY = Float.NaN;
   public boolean hudRight = true;
   public float hudScale = 1.0F;

   /** Modules that were renamed: new id to old id, so saved settings carry over. */
   private static final java.util.Map<String, String> RENAMED = java.util.Map.ofEntries(
      java.util.Map.entry("viewmodel-arm", "3d-hand"),
      java.util.Map.entry("avatar", "character-3d"),
      java.util.Map.entry("viewmodel", "hand-pose"),
      java.util.Map.entry("hand-glow", "hand-aura"),
      java.util.Map.entry("swing-speed", "slow-swing"),
      java.util.Map.entry("aurora-sky", "atmosphere"),
      java.util.Map.entry("orbit-orbs", "floating-relics"),
      java.util.Map.entry("effect-particles", "particles"),
      java.util.Map.entry("totem-ghost", "totem-spirit"),
      java.util.Map.entry("copy-coords", "coord-snapper"),
      java.util.Map.entry("stream-hider", "no-inv-leak")
   );

   public Settings(String id, boolean enabledByDefault) {
      Path dir = FabricLoader.getInstance().getConfigDir().resolve("venti").resolve("modules");
      this.file = dir.resolve(id + ".json");
      String old = RENAMED.get(id);
      if (old != null && !Files.exists(this.file) && Files.exists(dir.resolve(old + ".json"))) {
         try {
            Files.copy(dir.resolve(old + ".json"), this.file);
         } catch (IOException e) {
            LOGGER.warn("[Venti] Could not carry over settings from {}: {}", old, e.toString());
         }
      }

      this.enabled = enabledByDefault;
   }

   public <S extends Setting<?>> S add(String key, S setting) {
      setting.bind(key);
      this.all.add(setting);
      return setting;
   }

   public List<Setting<?>> all() {
      return this.all;
   }

   public void resetAll() {
      for (Setting<?> setting : this.all) {
         setting.reset();
      }
   }

   public void load() {
      if (!Files.isRegularFile(this.file)) {
         return;
      }

      try {
         JsonObject root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8)).getAsJsonObject();
         if (root.has("enabled")) {
            this.enabled = root.get("enabled").getAsBoolean();
         }

         if (root.has("bind")) {
            this.bind = root.get("bind").getAsInt();
         }

         if (root.has("hud")) {
            JsonObject hud = root.getAsJsonObject("hud");
            this.hudX = hud.has("x") ? hud.get("x").getAsFloat() : Float.NaN;
            this.hudY = hud.has("y") ? hud.get("y").getAsFloat() : Float.NaN;
            this.hudScale = hud.has("scale") ? hud.get("scale").getAsFloat() : 1.0F;
            this.hudRight = !hud.has("right") || hud.get("right").getAsBoolean();
         }

         JsonObject values = root.has("settings") ? root.getAsJsonObject("settings") : new JsonObject();
         for (Setting<?> setting : this.all) {
            JsonElement value = values.get(setting.key());
            if (value != null) {
               try {
                  setting.fromJson(value);
               } catch (RuntimeException e) {
                  LOGGER.warn("[Venti] Ignoring bad value for {}: {}", setting.key(), value);
               }
            }
         }
      } catch (IOException | RuntimeException e) {
         LOGGER.warn("[Venti] Could not read {}: {}", this.file, e.toString());
      }
   }

   public void save() {
      JsonObject root = new JsonObject();
      root.addProperty("enabled", this.enabled);
      root.addProperty("bind", this.bind);
      JsonObject hud = new JsonObject();
      if (!Float.isNaN(this.hudX)) {
         hud.addProperty("x", this.hudX);
         hud.addProperty("y", this.hudY);
         hud.addProperty("right", this.hudRight);
      }

      hud.addProperty("scale", this.hudScale);
      root.add("hud", hud);
      JsonObject values = new JsonObject();
      for (Setting<?> setting : this.all) {
         values.add(setting.key(), setting.toJson());
      }

      root.add("settings", values);

      try {
         Files.createDirectories(this.file.getParent());
         Files.writeString(this.file, GSON.toJson(root), StandardCharsets.UTF_8);
      } catch (IOException e) {
         LOGGER.warn("[Venti] Could not save {}: {}", this.file, e.toString());
      }
   }
}
