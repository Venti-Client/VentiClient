package dev.venti.modules.view;

import dev.venti.compat.Mc;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

/** Press the Coord Snapper key (Controls > Venti, unbound by default) to copy coordinates to the clipboard. */
public final class CopyCoordsModule extends Module {
   private final ModeSetting target = this.add("target", new ModeSetting("Target", "Looked At", "Looked At", "Own Position"));
   private final NumberSetting range = this.add("range", new NumberSetting("Range", 256.0F, 16.0F, 512.0F).step(1.0F));
   private final BooleanSetting stopAtFluids = this.add("stop-at-fluids", new BooleanSetting("Stop at Fluids", false));
   private final ModeSetting format = this.add("format", new ModeSetting("Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));
   private final BooleanSetting includeDimension = this.add("include-dimension", new BooleanSetting("Include Dimension", true));
   private final BooleanSetting confirm = this.add("confirm", new BooleanSetting("Chat Confirmation", true));
   private final KeyMapping key;

   public CopyCoordsModule() {
      super("copy-coords", "Copy Coords", "Copies the coordinates you look at or stand on with one key (Controls > Venti).", ModuleCategory.MISC);
      this.range.visibleWhen(() -> this.target.is("Looked At"));
      this.stopAtFluids.visibleWhen(() -> this.target.is("Looked At"));
      this.key = ViewKeys.register("key.venti.coord_snapper", GLFW.GLFW_KEY_UNKNOWN);
   }

   @Override
   protected void onEnable() {
      while (this.key.consumeClick()) {
      }
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      boolean pressed = false;
      while (this.key.consumeClick()) {
         pressed = true;
      }

      if (pressed && Mc.screen() == null && mc.player != null && mc.level != null) {
         this.snap(mc, mc.player, mc.level);
      }
   }

   private void snap(Minecraft mc, LocalPlayer player, ClientLevel level) {
      BlockPos pos;
      if (this.target.is("Own Position")) {
         pos = player.blockPosition();
      } else {
         pos = this.raycast(player, level);
         if (pos == null) {
            this.chat(Component.literal("Nothing copied").withStyle(ChatFormatting.RED)
               .append(Component.literal(": no loaded block within " + Math.round(this.range.get()) + " blocks of your aim").withStyle(ChatFormatting.GRAY)));
            return;
         }

         if (this.format.is("/tp Command")) {
            pos = pos.above();
         }
      }

      String text = this.formatCoords(pos, level.dimension());
      mc.keyboardHandler.setClipboard(text);
      if (this.confirm.get()) {
         this.chat(Component.literal("Copied ").withStyle(ChatFormatting.GREEN).append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
      }
   }

   private BlockPos raycast(LocalPlayer player, ClientLevel level) {
      Vec3 origin = player.getEyePosition(1.0F);
      Vec3 direction = player.getViewVector(1.0F);
      Vec3 end = origin.add(direction.scale(this.range.get()));
      ClipContext.Fluid fluids = this.stopAtFluids.get() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
      ClipContext context = new ClipContext(origin, end, ClipContext.Block.OUTLINE, fluids, player);
      BlockPos viewBlock = BlockPos.containing(origin);
      if (startsInside(context, level, viewBlock, origin)) {
         Optional<Vec3> exit = new AABB(viewBlock).clip(end, origin);
         if (exit.isEmpty()) {
            return null;
         }

         Vec3 start = exit.get().add(direction.scale(0.001));
         context = new ClipContext(start, end, ClipContext.Block.OUTLINE, fluids, player);
      }

      BlockHitResult hit = level.clip(context);
      return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
   }

   private static boolean startsInside(ClipContext context, ClientLevel level, BlockPos pos, Vec3 point) {
      return contains(context.getBlockShape(level.getBlockState(pos), level, pos), pos, point)
         || contains(context.getFluidShape(level.getFluidState(pos), level, pos), pos, point);
   }

   private static boolean contains(VoxelShape shape, BlockPos pos, Vec3 point) {
      for (AABB box : shape.toAabbs()) {
         if (box.move(pos).contains(point)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(BlockPos pos, ResourceKey<Level> dimension) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      boolean withDimension = this.includeDimension.get();
      return switch (this.format.get()) {
         case "/tp Command" -> withDimension
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", dimension.identifier(), x, y, z)
            : String.format(Locale.ROOT, "/tp %d %d %d", x, y, z);
         case "X, Y, Z" -> String.format(Locale.ROOT, "%d, %d, %d", x, y, z) + dimensionSuffix(dimension, withDimension);
         default -> String.format(Locale.ROOT, "%d %d %d", x, y, z) + dimensionSuffix(dimension, withDimension);
      };
   }

   private static String dimensionSuffix(ResourceKey<Level> dimension, boolean enabled) {
      return enabled ? " (" + dimensionName(dimension) + ")" : "";
   }

   private static String dimensionName(ResourceKey<Level> dimension) {
      if (Level.OVERWORLD.equals(dimension)) {
         return "Overworld";
      } else if (Level.NETHER.equals(dimension)) {
         return "Nether";
      } else {
         return Level.END.equals(dimension) ? "End" : dimension.identifier().toString();
      }
   }
}
