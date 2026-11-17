package dev.venti.modules.view;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

/** Press the Coord Snapper key (Controls > Venti, unbound by default) to copy coordinates to the clipboard. */
public final class CopyCoordsModule extends Module {
   private final ModeSetting target = this.add("target", new ModeSetting("Target", "Looked At", "Looked At", "Own Position"));
   private final NumberSetting range = this.add("range", new NumberSetting("Range", 256.0F, 16.0F, 512.0F).step(1.0F));
   private final BooleanSetting stopAtFluids = this.add("stop-at-fluids", new BooleanSetting("Stop at Fluids", false));
   private final ModeSetting format = this.add("format", new ModeSetting("Format", "X Y Z", "X Y Z", "X, Y, Z", "/tp Command"));
   private final BooleanSetting includeDimension = this.add("include-dimension", new BooleanSetting("Include Dimension", true));
   private final BooleanSetting confirm = this.add("confirm", new BooleanSetting("Chat Confirmation", true));
   private final KeyBinding key;

   public CopyCoordsModule() {
      super("copy-coords", "Copy Coords", "Copies the coordinates you look at or stand on with one key (Controls > Venti).", ModuleCategory.MISC);
      this.range.visibleWhen(() -> this.target.is("Looked At"));
      this.stopAtFluids.visibleWhen(() -> this.target.is("Looked At"));
      this.key = ViewKeys.register("key.venti.coord_snapper", GLFW.GLFW_KEY_UNKNOWN);
   }

   @Override
   protected void onEnable() {
      while (this.key.wasPressed()) {
      }
   }

   @Override
   protected void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      boolean pressed = false;
      while (this.key.wasPressed()) {
         pressed = true;
      }

      if (pressed && mc.currentScreen == null && mc.player != null && mc.world != null) {
         this.snap(mc, mc.player, mc.world);
      }
   }

   private void snap(MinecraftClient mc, ClientPlayerEntity player, ClientWorld level) {
      BlockPos pos;
      if (this.target.is("Own Position")) {
         pos = player.getBlockPos();
      } else {
         pos = this.raycast(player, level);
         if (pos == null) {
            this.chat(Text.literal("Nothing copied").formatted(Formatting.RED)
               .append(Text.literal(": no loaded block within " + Math.round(this.range.get()) + " blocks of your aim").formatted(Formatting.GRAY)));
            return;
         }

         if (this.format.is("/tp Command")) {
            pos = pos.up();
         }
      }

      String text = this.formatCoords(pos, level.getRegistryKey());
      mc.keyboard.setClipboard(text);
      if (this.confirm.get()) {
         this.chat(Text.literal("Copied ").formatted(Formatting.GREEN).append(Text.literal(text).formatted(Formatting.WHITE)));
      }
   }

   private BlockPos raycast(ClientPlayerEntity player, ClientWorld level) {
      Vec3d origin = player.getCameraPosVec(1.0F);
      Vec3d direction = player.getRotationVec(1.0F);
      Vec3d end = origin.add(direction.multiply(this.range.get()));
      RaycastContext.FluidHandling fluids = this.stopAtFluids.get() ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
      RaycastContext context = new RaycastContext(origin, end, RaycastContext.ShapeType.OUTLINE, fluids, player);
      BlockPos viewBlock = BlockPos.ofFloored(origin);
      if (startsInside(context, level, viewBlock, origin)) {
         Optional<Vec3d> exit = new Box(viewBlock).raycast(end, origin);
         if (exit.isEmpty()) {
            return null;
         }

         Vec3d start = exit.get().add(direction.multiply(0.001));
         context = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, fluids, player);
      }

      BlockHitResult hit = level.raycast(context);
      return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
   }

   private static boolean startsInside(RaycastContext context, ClientWorld level, BlockPos pos, Vec3d point) {
      return contains(context.getBlockShape(level.getBlockState(pos), level, pos), pos, point)
         || contains(context.getFluidShape(level.getFluidState(pos), level, pos), pos, point);
   }

   private static boolean contains(VoxelShape shape, BlockPos pos, Vec3d point) {
      for (Box box : shape.getBoundingBoxes()) {
         if (box.offset(pos).contains(point)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(BlockPos pos, RegistryKey<World> dimension) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      boolean withDimension = this.includeDimension.get();
      return switch (this.format.get()) {
         case "/tp Command" -> withDimension
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", dimension.getValue(), x, y, z)
            : String.format(Locale.ROOT, "/tp %d %d %d", x, y, z);
         case "X, Y, Z" -> String.format(Locale.ROOT, "%d, %d, %d", x, y, z) + dimensionSuffix(dimension, withDimension);
         default -> String.format(Locale.ROOT, "%d %d %d", x, y, z) + dimensionSuffix(dimension, withDimension);
      };
   }

   private static String dimensionSuffix(RegistryKey<World> dimension, boolean enabled) {
      return enabled ? " (" + dimensionName(dimension) + ")" : "";
   }

   private static String dimensionName(RegistryKey<World> dimension) {
      if (World.OVERWORLD.equals(dimension)) {
         return "Overworld";
      } else if (World.NETHER.equals(dimension)) {
         return "Nether";
      } else {
         return World.END.equals(dimension) ? "End" : dimension.getValue().toString();
      }
   }
}
