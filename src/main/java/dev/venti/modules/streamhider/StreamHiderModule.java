package dev.venti.modules.streamhider;

import com.sun.jna.Platform;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.StringSetting;
import dev.venti.imgui.ImGuiManager;
import dev.venti.mixin.streamhider.CreativeSlotAccessor;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector2f;

/**
 * No Inv Leak, ported from Anubis (4ldenz, GPL-3.0): draws fake items in place of yours for streams and screen shares.
 * Stream Only (Windows) draws your real items in capture-excluded overlay windows so only you see them.
 */
public final class StreamHiderModule extends Module {
   private static volatile StreamHiderModule active;
   private static boolean bareModel;
   private static final Vector2f POINT = new Vector2f();
   private final ModeSetting style = this.add("style", new ModeSetting("Style", FakeItems.TOTEM, FakeItems.STYLES));
   private final StringSetting customItem = this.add("custom-item", new StringSetting("Custom Item ID", "minecraft:diamond"));
   private final NumberSetting shuffleSeconds = this.add("shuffle-seconds", new NumberSetting("Shuffle Seconds", 5.0F, 1.0F, 60.0F).step(1.0F));
   private final BooleanSetting fakeCounts = this.add("fake-counts", new BooleanSetting("Fake Counts", true));
   private final BooleanSetting fillEmpty = this.add("fill-empty", new BooleanSetting("Fill Empty Slots", true));
   private final BooleanSetting streamOnly = this.add("stream-only", new BooleanSetting("Stream Only", false));
   private final BooleanSetting borderless = this.add("borderless-fullscreen", new BooleanSetting("Borderless Fullscreen", true));
   private final BooleanSetting tooltips = this.add("hide-tooltips", new BooleanSetting("Hide Tooltips", true));
   private final BooleanSetting itemName = this.add("hide-item-name", new BooleanSetting("Hide Item Name", true));
   private final BooleanSetting selectedSlot = this.add("hide-selected-slot", new BooleanSetting("Hide Selected Slot", true));
   private final BooleanSetting hud = this.add("venti-hud", new BooleanSetting("Venti HUD", true));
   private final FakeItems items = new FakeItems();

   public StreamHiderModule() {
      super("stream-hider", "Stream Hider", "Hides your items on screen for screen shares. Stream Only keeps them visible to you alone.", ModuleCategory.MISC);
      this.customItem.visibleWhen(() -> this.style.is(FakeItems.CUSTOM));
      this.shuffleSeconds.visibleWhen(() -> this.style.is(FakeItems.SHUFFLE));
      this.fakeCounts.visibleWhen(() -> !this.style.is(FakeItems.EMPTY));
      this.fillEmpty.visibleWhen(() -> !this.style.is(FakeItems.EMPTY));
      this.streamOnly.visibleWhen(Platform::isWindows);
      this.streamOnly.onChange(() -> {
         if (this.streamOnly.get()) {
            StreamOverlay.retry();
         } else {
            StreamOverlay.stop();
         }
      });
      this.borderless.visibleWhen(() -> Platform.isWindows() && this.streamOnly.get());
      ImGuiManager.getInstance().setStreamRenderer(StreamOverlay::drawFakes, StreamOverlay::hasFakes);
      // Runs whether or not the module is on, so turning it off also leaves borderless fullscreen.
      ClientTickEvents.END_CLIENT_TICK.register(client -> BorderlessFullscreen.tick());
   }

   public static boolean wantsBorderless() {
      StreamHiderModule module = Modules.get(StreamHiderModule.class);
      return module != null && module.streamOnly.get() && module.borderless.get() && module.isEnabled();
   }

   @Override
   protected void onEnable() {
      this.items.reseed();
      StreamOverlay.retry();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      StreamOverlay.stop();
   }

   public static ItemStack hotbar(ItemStack stack, int x, int y, int seed) {
      StreamHiderModule module = active;
      if (module != null && stack != null) {
         int slot = seed == 10 ? 40 : (seed >= 1 && seed <= 9 ? seed - 1 : 41);
         if (module.streamOverlaying() && StreamOverlay.recordsHotbar()) {
            StreamOverlay.hotbar(slot, x, y, stack);
            return ItemStack.EMPTY;
         } else {
            return module.shown(stack, slot);
         }
      } else {
         return stack;
      }
   }

   public static ItemStack container(ItemStack stack, Slot slot, int x, int y, boolean hovered) {
      StreamHiderModule module = active;
      if (module != null && stack != null) {
         int index = inventoryIndex(slot);
         if (module.streamOverlaying()) {
            StreamOverlay.container(index, x, y, stack, hovered);
            return ItemStack.EMPTY;
         } else {
            return module.shown(stack, index);
         }
      } else {
         return stack;
      }
   }

   private static int inventoryIndex(Slot slot) {
      Slot real = slot instanceof CreativeSlotAccessor wrapper ? wrapper.venti$noInvLeakTarget() : slot;
      return real.getIndex();
   }

   public static ItemStack dragged(ItemStack stack) {
      StreamHiderModule module = active;
      if (module != null && stack != null && !stack.isEmpty()) {
         return module.streamOverlaying() ? ItemStack.EMPTY : module.fake(stack, 41);
      } else {
         return stack;
      }
   }

   public static boolean hidesModelOf(LivingEntity entity) {
      return active != null && entity != null && entity == MinecraftClient.getInstance().player;
   }

   public static void bareModel(boolean on) {
      bareModel = on;
   }

   public static ItemStack modelItem(LivingEntity entity, ItemStack stack) {
      return bareModel && stack != null && !stack.isEmpty() && entity == MinecraftClient.getInstance().player ? ItemStack.EMPTY : stack;
   }

   public static int screenX(DrawContext context, int localX, int localY, int fallback) {
      if (context != null && active != null) {
         context.getMatrices().transformPosition(localX, localY, POINT);
         return Math.round(POINT.x);
      } else {
         return fallback;
      }
   }

   public static int screenY(DrawContext context, int localX, int localY, int fallback) {
      if (context != null && active != null) {
         context.getMatrices().transformPosition(localX, localY, POINT);
         return Math.round(POINT.y);
      } else {
         return fallback;
      }
   }

   public static ItemStack carried(ItemStack stack, int x, int y) {
      StreamHiderModule module = active;
      if (module == null || stack == null || stack.isEmpty()) {
         return stack;
      } else if (module.streamOverlaying()) {
         StreamOverlay.carried(stack, x, y);
         return ItemStack.EMPTY;
      } else {
         return module.fake(stack, 41);
      }
   }

   /** True while HUD elements that show the player's items (armor status and the like) should show fakes instead. */
   public static boolean hidesHudItems() {
      StreamHiderModule module = active;
      return module != null && module.hud.get();
   }

   /** What a Venti HUD element should draw for an equipped stack: a fake while {@link #hidesHudItems()} is on, otherwise the stack itself. */
   public static ItemStack shownInHud(ItemStack stack, EquipmentSlot slot) {
      StreamHiderModule module = active;
      return module != null && module.hud.get() && stack != null && !stack.isEmpty() ? module.fake(stack, inventorySlot(slot)) : stack;
   }

   /** The fake drawn over a Stream Only slot; {@code real} is the slot's real stack, which only Decoy looks at. */
   public static ItemStack fakeFor(int slot, ItemStack real) {
      StreamHiderModule module = active;
      return module == null ? ItemStack.EMPTY : module.fake(real == null ? ItemStack.EMPTY : real, slot);
   }

   public static boolean fillsEmpty() {
      StreamHiderModule module = active;
      return module != null && module.fillEmpty.get() && !module.style.is(FakeItems.EMPTY);
   }

   public static boolean overlaying() {
      StreamHiderModule module = active;
      return module != null && module.streamOnly.get() && StreamOverlay.usable();
   }

   private boolean streamOverlaying() {
      return this.streamOnly.get() && StreamOverlay.usable();
   }

   public static boolean ownSlot(Slot slot) {
      return active != null && slot != null && slot.inventory instanceof PlayerInventory;
   }

   public static boolean hidesTooltips() {
      StreamHiderModule module = active;
      return module != null && module.tooltips.get();
   }

   public static boolean hidesItemName() {
      StreamHiderModule module = active;
      return module != null && module.itemName.get();
   }

   public static boolean hidesSelectedSlot() {
      StreamHiderModule module = active;
      return module != null && module.selectedSlot.get();
   }

   public static boolean overlayWanted() {
      StreamHiderModule module = active;
      return module != null && module.streamOnly.get();
   }

   public static void streamNotice(String headline, String detail) {
      StreamHiderModule module = active;
      if (module != null) {
         module.chat(Text.literal(headline).formatted(Formatting.YELLOW).append(Text.literal(detail).formatted(Formatting.GRAY)));
      }
   }

   public static boolean opaqueCells() {
      StreamHiderModule module = active;
      return module != null && !module.style.is(FakeItems.EMPTY);
   }

   private ItemStack shown(ItemStack stack, int slot) {
      return stack.isEmpty() && !this.fillEmpty.get() ? stack : this.fake(stack, slot);
   }

   private ItemStack fake(ItemStack stack, int slot) {
      return this.items.get(this.style.get(), slot, stack, this.fakeCounts.get(), this.shuffleSeconds.get(), this.customItem.get());
   }

   private static int inventorySlot(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> 39;
         case CHEST -> 38;
         case LEGS -> 37;
         case FEET -> 36;
         case OFFHAND -> 40;
         case MAINHAND -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            yield player != null ? player.getInventory().getSelectedSlot() : 41;
         }
         default -> 41;
      };
   }
}
