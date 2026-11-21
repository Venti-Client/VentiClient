package dev.venti.modules.streamhider;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Picks the fake item each style shows for an inventory slot.
 * Slots use player inventory indices: 0-8 hotbar, 9-35 main, 36-39 armor (feet, legs, chest, head), 40 offhand, 41 anything else.
 */
final class FakeItems {
   static final String TOTEM = "Totem";
   static final String RANDOM = "Random";
   static final String EMPTY = "Empty";
   static final String DIRT = "Dirt";
   static final String BARRIER = "Barrier";
   static final String STARTER = "Starter Kit";
   static final String FLEX = "Flex";
   static final String DECOY = "Decoy";
   static final String RAINBOW = "Rainbow";
   static final String CUSTOM = "Custom Item";
   static final String SHUFFLE = "Shuffle";
   static final String[] STYLES = {TOTEM, RANDOM, EMPTY, DIRT, BARRIER, STARTER, FLEX, DECOY, RAINBOW, CUSTOM, SHUFFLE};
   static final int SLOTS = 42;
   private static final long RAINBOW_STEP_MS = 750L;
   private static final Item[] STARTER_HOTBAR = {
      Items.WOODEN_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.WOODEN_SHOVEL, Items.BREAD, Items.TORCH, Items.COBBLESTONE, Items.OAK_LOG,
      Items.CRAFTING_TABLE
   };
   private static final Item[] STARTER_MAIN = {
      Items.OAK_PLANKS, Items.COBBLESTONE, Items.DIRT, Items.STICK, Items.COAL, Items.RAW_IRON, Items.OAK_SAPLING, Items.WHEAT_SEEDS, Items.APPLE,
      Items.ROTTEN_FLESH, Items.GRAVEL, Items.SAND, Items.FLINT, Items.BONE, Items.STRING, Items.OAK_LOG, Items.TORCH, Items.FURNACE,
      Items.CHEST, Items.COBBLESTONE, Items.DIRT, Items.ANDESITE, Items.GRANITE, Items.DIORITE, Items.RAW_COPPER, Items.LEATHER, Items.WOODEN_PICKAXE
   };
   private static final Item[] STARTER_ARMOR = {Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET};
   private static final Item[] FLEX_HOTBAR = {
      Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.END_CRYSTAL, Items.OBSIDIAN, Items.ENDER_PEARL,
      Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING, Items.RESPAWN_ANCHOR
   };
   private static final Item[] FLEX_MAIN = {
      Items.TOTEM_OF_UNDYING, Items.TOTEM_OF_UNDYING, Items.ENCHANTED_GOLDEN_APPLE, Items.END_CRYSTAL, Items.OBSIDIAN, Items.ENDER_PEARL,
      Items.GLOWSTONE, Items.EXPERIENCE_BOTTLE, Items.NETHERITE_INGOT, Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.OBSIDIAN,
      Items.GOLDEN_APPLE, Items.NETHERITE_SHOVEL, Items.ELYTRA, Items.FIREWORK_ROCKET, Items.TOTEM_OF_UNDYING, Items.ENCHANTED_GOLDEN_APPLE,
      Items.DIAMOND_BLOCK, Items.NETHERITE_BLOCK, Items.END_CRYSTAL, Items.TOTEM_OF_UNDYING, Items.ENDER_PEARL, Items.OBSIDIAN,
      Items.TOTEM_OF_UNDYING, Items.BEACON, Items.SHULKER_BOX
   };
   private static final Item[] FLEX_ARMOR = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
   private static final Item[] CHEAP = {
      Items.STICK, Items.COBBLESTONE, Items.DIRT, Items.BREAD, Items.WOODEN_PICKAXE, Items.WOODEN_SWORD, Items.OAK_PLANKS, Items.TORCH
   };
   private static final Item[] RAINBOW_PANES = {
      Items.WHITE_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE, Items.BLACK_STAINED_GLASS_PANE,
      Items.BROWN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE,
      Items.LIME_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.CYAN_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
      Items.BLUE_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.PINK_STAINED_GLASS_PANE
   };
   private static final Item[] RAINBOW_GLASS = {
      Items.WHITE_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.BLACK_STAINED_GLASS,
      Items.BROWN_STAINED_GLASS, Items.RED_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS,
      Items.LIME_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS,
      Items.BLUE_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS, Items.PINK_STAINED_GLASS
   };
   private final ItemStack[] cache = new ItemStack[SLOTS];
   private final long[] cacheKey = new long[SLOTS];
   private long baseSeed = ThreadLocalRandom.current().nextLong();
   private long shuffleEpoch = Long.MIN_VALUE;
   private long shuffleSeed;
   private String customId;
   private Item customItem = Items.BARRIER;

   /** New random picks and stack sizes, used when the module turns on. */
   void reseed() {
      this.baseSeed = ThreadLocalRandom.current().nextLong();
      this.shuffleEpoch = Long.MIN_VALUE;
      Arrays.fill(this.cache, null);
   }

   ItemStack get(String style, int slot, ItemStack real, boolean fakeCounts, float shuffleSeconds, String custom) {
      if (slot < 0 || slot >= SLOTS) {
         slot = SLOTS - 1;
      }

      long seed = this.baseSeed;
      if (SHUFFLE.equals(style)) {
         long period = Math.max(1000L, (long)(shuffleSeconds * 1000.0F));
         long epoch = System.currentTimeMillis() / period;
         if (epoch != this.shuffleEpoch) {
            this.shuffleEpoch = epoch;
            this.shuffleSeed = FakeLoadout.mix(this.baseSeed ^ epoch * 0x9E3779B97F4A7C15L);
         }

         seed = this.shuffleSeed;
      }

      boolean empty = real == null || real.isEmpty();
      Item item = switch (style) {
         case EMPTY -> null;
         case RANDOM, SHUFFLE -> FakeLoadout.item(seed, slot);
         case DIRT -> Items.DIRT;
         case BARRIER -> Items.BARRIER;
         case STARTER -> fixed(STARTER_HOTBAR, STARTER_MAIN, STARTER_ARMOR, Items.TORCH, Items.COBBLESTONE, slot);
         case FLEX -> fixed(FLEX_HOTBAR, FLEX_MAIN, FLEX_ARMOR, Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, slot);
         case DECOY -> empty ? CHEAP[pick(seed, slot, CHEAP.length)] : decoy(real);
         case RAINBOW -> rainbow(slot);
         case CUSTOM -> this.custom(custom);
         default -> Items.TOTEM_OF_UNDYING;
      };
      if (item == null || item == Items.AIR) {
         return ItemStack.EMPTY;
      }

      // Stacks are cached per slot so counts stay put until the item, the seed or the Fake Counts setting changes.
      long key = BuiltInRegistries.ITEM.getId(item) * 31L + seed * 7L + (fakeCounts ? 1L : 0L) + style.hashCode() * 131L;
      ItemStack cached = this.cache[slot];
      if (cached != null && this.cacheKey[slot] == key) {
         return cached;
      }

      ItemStack stack = build(style, item, slot, seed, fakeCounts);
      this.cache[slot] = stack;
      this.cacheKey[slot] = key;
      return stack;
   }

   private static ItemStack build(String style, Item item, int slot, long seed, boolean fakeCounts) {
      ItemStack stack = item.getDefaultInstance();
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      }

      int max = stack.getMaxStackSize();
      if (fakeCounts && max > 1) {
         stack.setCount(1 + (int)Long.remainderUnsigned(FakeLoadout.mix(seed ^ slot ^ 81L), max));
      } else {
         stack.setCount(1);
      }

      if (stack.isDamageableItem()) {
         if (RANDOM.equals(style) || SHUFFLE.equals(style) || STARTER.equals(style)) {
            int half = Math.max(1, stack.getMaxDamage() / 2);
            stack.setDamageValue((int)Long.remainderUnsigned(FakeLoadout.mix(seed ^ slot ^ 82L), half));
         } else if (FLEX.equals(style)) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
         }
      }

      return stack;
   }

   private static Item fixed(Item[] hotbar, Item[] main, Item[] armor, Item offhand, Item other, int slot) {
      if (slot < 9) {
         return hotbar[slot];
      } else if (slot < 36) {
         return main[slot - 9];
      } else if (slot < 40) {
         return armor[slot - 36];
      } else {
         return slot == 40 ? offhand : other;
      }
   }

   private static Item rainbow(int slot) {
      int step = (int)(System.currentTimeMillis() / RAINBOW_STEP_MS % RAINBOW_PANES.length);
      int color = Math.floorMod(slot + step, RAINBOW_PANES.length);
      // Armor and offhand show full glass blocks so the worn pieces read as solid colors.
      return slot >= 36 && slot <= 40 ? RAINBOW_GLASS[color] : RAINBOW_PANES[color];
   }

   private Item custom(String id) {
      String wanted = id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
      if (!wanted.equals(this.customId)) {
         this.customId = wanted;
         this.customItem = parse(wanted);
      }

      return this.customItem;
   }

   private static Item parse(String id) {
      Identifier parsed = id.isEmpty() ? null : Identifier.tryParse(id);
      if (parsed == null || !BuiltInRegistries.ITEM.containsKey(parsed)) {
         return Items.BARRIER;
      }

      Item item = BuiltInRegistries.ITEM.getValue(parsed);
      return item == null || item == Items.AIR ? Items.BARRIER : item;
   }

   /** A cheap item of the same kind as the real one, so the layout looks real but reveals nothing. */
   static Item decoy(ItemStack real) {
      Item item = real.getItem();
      if (real.is(ItemTags.SWORDS)) {
         return Items.WOODEN_SWORD;
      } else if (real.is(ItemTags.PICKAXES)) {
         return Items.WOODEN_PICKAXE;
      } else if (real.is(ItemTags.AXES)) {
         return Items.WOODEN_AXE;
      } else if (real.is(ItemTags.SHOVELS)) {
         return Items.WOODEN_SHOVEL;
      } else if (real.is(ItemTags.HOES)) {
         return Items.WOODEN_HOE;
      } else if (real.is(ItemTags.SPEARS)) {
         return Items.WOODEN_SPEAR;
      } else if (item == Items.MACE || item == Items.TRIDENT) {
         return Items.WOODEN_SWORD;
      } else if (item == Items.BOW || item == Items.CROSSBOW) {
         return Items.BOW;
      } else if (real.is(ItemTags.HEAD_ARMOR)) {
         return Items.LEATHER_HELMET;
      } else if (real.is(ItemTags.CHEST_ARMOR)) {
         return Items.LEATHER_CHESTPLATE;
      } else if (real.is(ItemTags.LEG_ARMOR)) {
         return Items.LEATHER_LEGGINGS;
      } else if (real.is(ItemTags.FOOT_ARMOR)) {
         return Items.LEATHER_BOOTS;
      } else if (real.has(DataComponents.FOOD)) {
         return Items.BREAD;
      } else if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
         // Items.POTION's default stack is a water bottle.
         return Items.POTION;
      } else if (item instanceof BlockItem) {
         return Items.COBBLESTONE;
      } else {
         return Items.STICK;
      }
   }

   private static int pick(long seed, int slot, int size) {
      return (int)Long.remainderUnsigned(FakeLoadout.mix(seed ^ slot * 0x5851F42D4C957F2DL ^ 0xDEC0L), size);
   }
}
