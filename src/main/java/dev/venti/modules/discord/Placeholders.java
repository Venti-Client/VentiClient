package dev.venti.modules.discord;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * A small stand-in for Meteor's Starscript: replaces {@code {name}} tokens in presence messages.
 * Unknown tokens are left untouched so typos are visible in Discord instead of silently vanishing.
 */
final class Placeholders {
   private static final Pattern TOKEN = Pattern.compile("\\{\\s*([a-zA-Z_.]+)\\s*}");
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

   /** The token names that {@link #apply} understands, for the settings description. */
   static final String SUPPORTED = "{player} {server} {health} {hunger} {dimension} {biome} {fps} {ping} {version} {mc_version} {time} "
      + "{server.difficulty} {server.player_count} {x} {y} {z}";

   private Placeholders() {
   }

   static String apply(String message) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Matcher matcher = TOKEN.matcher(message);
      StringBuilder out = new StringBuilder();
      while (matcher.find()) {
         String value = resolve(mc, matcher.group(1).toLowerCase(Locale.ROOT));
         matcher.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : matcher.group()));
      }

      matcher.appendTail(out);
      return out.toString();
   }

   private static String resolve(MinecraftClient mc, String name) {
      return switch (name) {
         case "player", "player.name" -> mc.getSession().getUsername();
         case "server", "server.name", "world" -> serverName(mc);
         case "health", "player.health" -> withPlayer(mc, p -> number(p.player.getHealth()));
         case "hunger", "player.hunger" -> withPlayer(mc, p -> Integer.toString(p.player.getHungerManager().getFoodLevel()));
         case "x", "player.x" -> withPlayer(mc, p -> Integer.toString(p.player.getBlockX()));
         case "y", "player.y" -> withPlayer(mc, p -> Integer.toString(p.player.getBlockY()));
         case "z", "player.z" -> withPlayer(mc, p -> Integer.toString(p.player.getBlockZ()));
         case "dimension", "player.dimension" -> dimension(mc);
         case "biome", "player.biome" -> biome(mc);
         case "fps" -> Integer.toString(mc.getCurrentFps());
         case "ping" -> ping(mc);
         case "version", "venti.version" -> FabricLoader.getInstance()
            .getModContainer("venti")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("");
         case "mc_version" -> SharedConstants.getGameVersion().name();
         case "time" -> LocalTime.now().format(TIME);
         case "server.difficulty" -> mc.world != null ? mc.world.getDifficulty().getName() : "";
         case "server.player_count" -> mc.getNetworkHandler() != null ? Integer.toString(mc.getNetworkHandler().getPlayerList().size()) : "0";
         default -> null;
      };
   }

   private static String withPlayer(MinecraftClient mc, Function<MinecraftClient, String> value) {
      return mc.player != null ? value.apply(mc) : "";
   }

   private static String number(float value) {
      return value == Math.floor(value) ? Integer.toString((int)value) : String.format(Locale.ROOT, "%.1f", value);
   }

   /** Singleplayer world name, server address, or "realms", like Meteor's {@code Utils.getWorldName()}. */
   private static String serverName(MinecraftClient mc) {
      if (mc.isInSingleplayer()) {
         return mc.getServer() != null && mc.world != null ? mc.getServer().getSaveProperties().getLevelName() : "";
      }

      ServerInfo entry = mc.getCurrentServerEntry();
      if (entry == null) {
         return "";
      }

      return entry.isRealm() ? "realms" : entry.address;
   }

   private static String dimension(MinecraftClient mc) {
      if (mc.world == null) {
         return "";
      }

      if (mc.world.getRegistryKey() == World.OVERWORLD) {
         return "Overworld";
      } else if (mc.world.getRegistryKey() == World.NETHER) {
         return "Nether";
      } else if (mc.world.getRegistryKey() == World.END) {
         return "End";
      }

      return title(mc.world.getRegistryKey().getValue().getPath());
   }

   private static String biome(MinecraftClient mc) {
      if (mc.player == null || mc.world == null) {
         return "";
      }

      return mc.world
         .getRegistryManager()
         .getOptional(RegistryKeys.BIOME)
         .map(registry -> {
            Identifier id = registry.getId(mc.world.getBiome(mc.player.getBlockPos()).value());
            return id == null ? "Unknown" : title(id.getPath());
         })
         .orElse("Unknown");
   }

   private static String ping(MinecraftClient mc) {
      if (mc.getNetworkHandler() == null || mc.player == null) {
         return "0";
      }

      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      return Integer.toString(entry != null ? entry.getLatency() : 0);
   }

   private static String title(String path) {
      return Arrays.stream(path.split("_"))
         .filter(part -> !part.isEmpty())
         .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
         .collect(Collectors.joining(" "));
   }
}
