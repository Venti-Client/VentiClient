package dev.venti.modules.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The waypoints of the world you are in, saved to config/venti/waypoints/&lt;key&gt;.json.
 * The key is "sp_&lt;save folder&gt;" in singleplayer and "mp_&lt;server address&gt;" in multiplayer.
 */
public final class WaypointStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("Venti");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final long SAVE_DELAY_MS = 750L;

   private final List<Waypoint> waypoints = new ArrayList<>();
   private String key;
   private boolean dirty;
   private long dirtySince;

   public boolean loaded() {
      return this.key != null;
   }

   public String key() {
      return this.key;
   }

   public List<Waypoint> all() {
      return this.waypoints;
   }

   /** Works out the key for the world just joined and loads its file. */
   public void load(Minecraft mc, ServerData serverInfo) {
      this.saveNow();
      this.waypoints.clear();
      this.key = worldKey(mc, serverInfo);
      this.dirty = false;
      Path file = this.file();
      if (!Files.isRegularFile(file)) {
         LOGGER.info("[Venti] Waypoints: new world {}", this.key);
         return;
      }

      try {
         JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
         JsonArray list = root.isJsonObject() && root.getAsJsonObject().has("waypoints")
            ? root.getAsJsonObject().getAsJsonArray("waypoints")
            : (root.isJsonArray() ? root.getAsJsonArray() : new JsonArray());
         for (JsonElement e : list) {
            if (e.isJsonObject()) {
               this.waypoints.add(Waypoint.fromJson(e.getAsJsonObject()));
            }
         }

         LOGGER.info("[Venti] Waypoints: loaded {} for {}", this.waypoints.size(), this.key);
      } catch (IOException | RuntimeException e) {
         LOGGER.error("[Venti] Waypoints: could not read {}", file, e);
      }
   }

   /** Saves and forgets the current world. */
   public void unload() {
      this.saveNow();
      this.waypoints.clear();
      this.key = null;
   }

   /** Marks the list changed; it is written shortly after (see {@link #tick()}). */
   public void changed() {
      if (this.key != null) {
         if (!this.dirty) {
            this.dirtySince = System.currentTimeMillis();
         }

         this.dirty = true;
      }
   }

   /** Called every client tick: writes pending changes once edits pause for a moment. */
   public void tick() {
      if (this.dirty && System.currentTimeMillis() - this.dirtySince >= SAVE_DELAY_MS) {
         this.saveNow();
      }
   }

   public void saveNow() {
      if (!this.dirty || this.key == null) {
         this.dirty = false;
         return;
      }

      this.dirty = false;
      JsonObject root = new JsonObject();
      root.addProperty("version", 1);
      root.addProperty("world", this.key);
      JsonArray list = new JsonArray();
      for (Waypoint w : this.waypoints) {
         list.add(w.toJson());
      }

      root.add("waypoints", list);
      Path file = this.file();
      try {
         Files.createDirectories(file.getParent());
         Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
         Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
         try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException atomicFailed) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException | RuntimeException e) {
         LOGGER.error("[Venti] Waypoints: could not save {}", file, e);
      }
   }

   private Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("venti").resolve("waypoints").resolve(this.key + ".json");
   }

   static String worldKey(Minecraft mc, ServerData serverInfo) {
      IntegratedServer server = mc.getSingleplayerServer();
      if (server != null) {
         String folder = null;
         try {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            if (root.getFileName() != null) {
               folder = root.getFileName().toString();
            }
         } catch (RuntimeException ignored) {
         }

         if (folder == null || folder.isBlank()) {
            folder = server.getWorldData().getLevelName();
         }

         return "sp_" + sanitize(folder);
      }

      ServerData info = serverInfo != null ? serverInfo : mc.getCurrentServer();
      if (info != null && info.ip != null && !info.ip.isBlank()) {
         return (info.isRealm() ? "realm_" : "mp_") + sanitize(info.ip.trim());
      }

      return "mp_unknown";
   }

   static String sanitize(String raw) {
      String s = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
      while (s.startsWith(".")) {
         s = s.substring(1);
      }

      if (s.isEmpty()) {
         s = "unnamed";
      }

      return s.length() > 96 ? s.substring(0, 96) : s;
   }
}
