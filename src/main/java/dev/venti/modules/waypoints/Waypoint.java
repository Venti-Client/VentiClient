package dev.venti.modules.waypoints;

import com.google.gson.JsonObject;

/** One saved waypoint: block coordinates in a dimension, with a name, color and visibility flag. */
public final class Waypoint {
   public static final String OVERWORLD = "minecraft:overworld";
   public static final String NETHER = "minecraft:the_nether";
   public static final String END = "minecraft:the_end";

   public String name;
   public int x;
   public int y;
   public int z;
   public String dimension;
   /** ARGB. */
   public int color;
   public boolean visible = true;
   public long created;
   public boolean death;

   public Waypoint(String name, int x, int y, int z, String dimension, int color, boolean death) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
      this.dimension = dimension;
      this.color = color;
      this.death = death;
      this.created = System.currentTimeMillis();
   }

   /** Short dimension tag for lists and labels. */
   public String dimensionTag() {
      return tag(this.dimension);
   }

   public static String tag(String dimension) {
      if (OVERWORLD.equals(dimension)) {
         return "OW";
      } else if (NETHER.equals(dimension)) {
         return "Nether";
      } else if (END.equals(dimension)) {
         return "End";
      }

      int colon = dimension.indexOf(':');
      return colon >= 0 ? dimension.substring(colon + 1) : dimension;
   }

   public String coords() {
      return this.x + " " + this.y + " " + this.z;
   }

   JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("name", this.name);
      o.addProperty("x", this.x);
      o.addProperty("y", this.y);
      o.addProperty("z", this.z);
      o.addProperty("dimension", this.dimension);
      o.addProperty("color", String.format("#%08X", this.color));
      o.addProperty("visible", this.visible);
      o.addProperty("created", this.created);
      o.addProperty("type", this.death ? "death" : "normal");
      return o;
   }

   static Waypoint fromJson(JsonObject o) {
      String name = o.has("name") ? o.get("name").getAsString() : "Waypoint";
      int x = o.has("x") ? o.get("x").getAsInt() : 0;
      int y = o.has("y") ? o.get("y").getAsInt() : 64;
      int z = o.has("z") ? o.get("z").getAsInt() : 0;
      String dim = o.has("dimension") ? o.get("dimension").getAsString() : OVERWORLD;
      int color = 0xFF55FFFF;
      if (o.has("color")) {
         try {
            color = (int)Long.parseLong(o.get("color").getAsString().replace("#", ""), 16);
         } catch (RuntimeException ignored) {
         }
      }

      boolean death = o.has("type") && "death".equals(o.get("type").getAsString());
      Waypoint w = new Waypoint(name, x, y, z, dim, color, death);
      if (o.has("visible")) {
         w.visible = o.get("visible").getAsBoolean();
      }

      if (o.has("created")) {
         w.created = o.get("created").getAsLong();
      }

      return w;
   }
}
