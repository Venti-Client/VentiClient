package dev.venti.modules.noinvleak;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** The Random style's item pools, ported from Anubis. */
final class FakeLoadout {
   private static final Item[] HOTBAR = new Item[]{
      Items.IRON_SWORD,
      Items.DIAMOND_SWORD,
      Items.NETHERITE_SWORD,
      Items.IRON_PICKAXE,
      Items.DIAMOND_PICKAXE,
      Items.NETHERITE_PICKAXE,
      Items.IRON_AXE,
      Items.DIAMOND_AXE,
      Items.NETHERITE_AXE,
      Items.IRON_SHOVEL,
      Items.DIAMOND_SHOVEL,
      Items.NETHERITE_SHOVEL,
      Items.BOW,
      Items.WATER_BUCKET,
      Items.COBBLESTONE,
      Items.TORCH,
      Items.COOKED_BEEF,
      Items.GOLDEN_CARROT,
      Items.BREAD,
      Items.ENDER_PEARL,
      Items.OAK_PLANKS,
      Items.DIRT,
      Items.FLINT_AND_STEEL
   };
   private static final Item[] MAIN = new Item[]{
      Items.COBBLESTONE,
      Items.STONE,
      Items.DIRT,
      Items.OAK_LOG,
      Items.OAK_PLANKS,
      Items.STICK,
      Items.COAL,
      Items.IRON_INGOT,
      Items.GOLD_INGOT,
      Items.DIAMOND,
      Items.REDSTONE,
      Items.LAPIS_LAZULI,
      Items.ARROW,
      Items.STRING,
      Items.BONE,
      Items.ROTTEN_FLESH,
      Items.GUNPOWDER,
      Items.SAND,
      Items.GRAVEL,
      Items.WHEAT_SEEDS,
      Items.WHEAT,
      Items.LADDER,
      Items.OBSIDIAN,
      Items.GLASS,
      Items.LEATHER,
      Items.FEATHER,
      Items.FLINT,
      Items.SPIDER_EYE,
      Items.APPLE,
      Items.COOKED_PORKCHOP,
      Items.BUCKET,
      Items.BOOK,
      Items.PAPER,
      Items.SUGAR_CANE,
      Items.CRAFTING_TABLE,
      Items.CHEST,
      Items.FURNACE
   };
   private static final Item[] OFFHAND = new Item[]{
      Items.SHIELD, Items.TORCH, Items.WATER_BUCKET, Items.ARROW, Items.COOKED_BEEF, Items.GOLDEN_APPLE
   };
   private static final Item[][] ARMOR = new Item[][]{
      {Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS},
      {Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
      {Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
      {Items.IRON_HELMET, Items.CHAINMAIL_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET}
   };
   private FakeLoadout() {
   }

   /** The Random style's item for an inventory slot (0-8 hotbar, 9-35 main, 36-39 armor feet to head, 40 offhand, 41 anything else). */
   static Item item(long seed, int slot) {
      if (slot < 9) {
         return pick(HOTBAR, seed, slot);
      } else if (slot < 36 || slot >= 41) {
         return pick(MAIN, seed, slot);
      } else if (slot == 40) {
         return pick(OFFHAND, seed, slot);
      } else {
         return ARMOR[slot - 36][(int)Long.remainderUnsigned(mix(seed ^ 167L), ARMOR[0].length)];
      }
   }

   private static Item pick(Item[] pool, long seed, int slot) {
      return pool[(int)Long.remainderUnsigned(mix(seed ^ slot * -7046029254386353131L), pool.length)];
   }

   static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }
}
