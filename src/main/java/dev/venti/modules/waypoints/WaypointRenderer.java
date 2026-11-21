package dev.venti.modules.waypoints;

import dev.venti.imgui.ImGuiManager;
import dev.venti.modules.world.WorldQuads;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Draws waypoints in two places:
 * <ul>
 *   <li>the beam, as camera-facing quads through {@link WorldQuads} during the world pass (depth tested, so terrain hides it);</li>
 *   <li>the diamond and label, on an ImGui HUD layer: each waypoint is projected to the screen with the view and projection
 *       matrices captured from {@code WorldRenderer.render}, so the label keeps a fixed on-screen size at any distance and
 *       shows through terrain.</li>
 * </ul>
 */
public final class WaypointRenderer {
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static double camX;
   private static double camY;
   private static double camZ;
   private static boolean captured;
   private static final List<Visible> SCRATCH = new ArrayList<>();
   private static final Vector4f TMP = new Vector4f();

   private WaypointRenderer() {
   }

   /** Called from the waypoints mixin at the start of every world render. */
   public static void capture(Matrix4f view, Matrix4f projection, Vec3 camera) {
      VIEW.set(view);
      PROJECTION.set(projection);
      camX = camera.x;
      camY = camera.y;
      camZ = camera.z;
      captured = true;
   }

   /** A waypoint resolved to the current dimension's coordinates. */
   record Visible(Waypoint waypoint, double x, double y, double z, double distance, boolean converted) {
   }

   /** The waypoints to draw this frame, in this dimension's coordinates, farthest first. */
   static List<Visible> collect(WaypointsModule module, ClientLevel world, LocalPlayer player) {
      SCRATCH.clear();
      String here = world.dimension().identifier().toString();
      double maxDistance = module.maxDistance();
      for (Waypoint w : module.store().all()) {
         if (!w.visible) {
            continue;
         }

         double x = w.x + 0.5;
         double z = w.z + 0.5;
         boolean converted = false;
         if (!here.equals(w.dimension)) {
            if (!module.showOtherDimensions()) {
               continue;
            }

            if (Waypoint.OVERWORLD.equals(w.dimension) && Waypoint.NETHER.equals(here)) {
               x = w.x / 8.0 + 0.5;
               z = w.z / 8.0 + 0.5;
            } else if (Waypoint.NETHER.equals(w.dimension) && Waypoint.OVERWORLD.equals(here)) {
               x = w.x * 8.0 + 0.5;
               z = w.z * 8.0 + 0.5;
            } else {
               continue;
            }

            converted = true;
         }

         double y = w.y + 0.5;
         double dx = x - player.getX();
         double dy = y - player.getY();
         double dz = z - player.getZ();
         double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (maxDistance > 0.0 && distance > maxDistance) {
            continue;
         }

         SCRATCH.add(new Visible(w, x, y, z, distance, converted));
      }

      SCRATCH.sort((a, b) -> Double.compare(b.distance, a.distance));
      return SCRATCH;
   }

   // ---- beam (world pass) ----

   static void renderBeams(WaypointsModule module, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel world = mc.level;
      LocalPlayer player = mc.player;
      if (world == null || player == null || !module.beam()) {
         return;
      }

      Vec3 cam = mc.gameRenderer.getMainCamera().position();
      double bottom = world.getMinY();
      double top = world.getMaxY() + 1.0;
      // Keep the beam inside the far plane: anything farther is pulled in along the same direction.
      double maxDraw = Math.max(48.0, mc.options.getEffectiveRenderDistance() * 16.0 * 0.85);
      WorldQuads layer = WorldQuads.layer(false);
      for (Visible v : collect(module, world, player)) {
         double dx = v.x - cam.x;
         double dz = v.z - cam.z;
         double horizontal = Math.sqrt(dx * dx + dz * dz);
         if (horizontal < 0.75) {
            continue; // standing inside it
         }

         double bx = v.x;
         double bz = v.z;
         double drawn = horizontal;
         if (horizontal > maxDraw) {
            double k = maxDraw / horizontal;
            bx = cam.x + dx * k;
            bz = cam.z + dz * k;
            drawn = maxDraw;
         }

         // Perpendicular to the view direction, so the beam always faces the camera; wider far away so it stays visible.
         double half = Math.max(0.12, drawn * 0.0022);
         double px = -dz / horizontal;
         double pz = dx / horizontal;
         int rgb = v.waypoint.color & 0xFFFFFF;
         int alpha = (v.waypoint.color >>> 24) & 0xFF;
         int outer = (Math.max(3, alpha * 40 / 255) << 24) | rgb;
         int core = (Math.max(3, alpha * 90 / 255) << 24) | rgb;
         beamQuad(layer, bx, bz, px, pz, half * 2.2, bottom, top, outer);
         beamQuad(layer, bx, bz, px, pz, half * 0.6, bottom, top, core);
      }
   }

   private static void beamQuad(WorldQuads layer, double x, double z, double px, double pz, double half, double bottom, double top, int argb) {
      double ax = x - px * half;
      double az = z - pz * half;
      double bx = x + px * half;
      double bz = z + pz * half;
      layer.quad(ax, bottom, az, bx, bottom, bz, bx, top, bz, ax, top, az, argb);
   }

   // ---- labels (ImGui HUD layer) ----

   static boolean hasLabelWork(WaypointsModule module) {
      Minecraft mc = Minecraft.getInstance();
      return module != null
         && module.isEnabled()
         && captured
         && mc.level != null
         && mc.player != null
         && !mc.options.hideGui
         && (mc.screen == null || mc.screen instanceof ChatScreen)
         && !module.store().all().isEmpty();
   }

   static void renderLabels(WaypointsModule module) {
      if (!hasLabelWork(module)) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      ImDrawList dl = ImGui.getBackgroundDrawList();
      float sw = ImGui.getIO().getDisplaySizeX();
      float sh = ImGui.getIO().getDisplaySizeY();
      ImGuiManager imgui = ImGuiManager.getInstance();
      ImFont font = imgui.hudTextFont();
      float scale = imgui.uiScale() * module.labelScale();
      int fontPx = Math.max(8, Math.round(ImGuiManager.HUD_TEXT_FONT_SIZE * scale));
      int smallPx = Math.max(7, Math.round(ImGuiManager.HUD_LABEL_FONT_SIZE * scale));
      Vec3 look = mc.player.getViewVector(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
      double cone = Math.cos(Math.toRadians(module.coneAngle()));

      for (Visible v : collect(module, mc.level, mc.player)) {
         double rx = v.x - camX;
         double ry = v.y - camY;
         double rz = v.z - camZ;
         if (module.onlyInViewCone()) {
            double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (len > 0.5 && (rx * look.x + ry * look.y + rz * look.z) / len < cone) {
               continue;
            }
         }

         // Only the direction matters for a screen-space label, so far waypoints are pulled in to stay in float range.
         double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
         if (len > 512.0) {
            double k = 512.0 / len;
            rx *= k;
            ry *= k;
            rz *= k;
         }

         TMP.set((float)rx, (float)ry, (float)rz, 1.0F);
         VIEW.transform(TMP);
         PROJECTION.transform(TMP);
         if (TMP.w <= 0.05F) {
            continue; // behind the camera
         }

         float sx = (TMP.x / TMP.w * 0.5F + 0.5F) * sw;
         float sy = (1.0F - (TMP.y / TMP.w * 0.5F + 0.5F)) * sh;
         if (sx < -200.0F || sx > sw + 200.0F || sy < -200.0F || sy > sh + 200.0F) {
            continue;
         }

         drawLabel(dl, font, fontPx, smallPx, scale, sx, sy, v);
      }
   }

   private static void drawLabel(ImDrawList dl, ImFont font, int fontPx, int smallPx, float scale, float sx, float sy, Visible v) {
      Waypoint w = v.waypoint;
      int color = abgr(w.color | 0xFF000000);
      String name = w.name == null || w.name.isEmpty() ? "?" : w.name;
      String dist = formatDistance(v.distance);
      if (v.converted) {
         dist = dist + " (" + w.dimensionTag() + ")";
      }

      ImVec2 nameSize = font.calcTextSizeA(fontPx, Float.MAX_VALUE, 0.0F, name);
      ImVec2 distSize = font.calcTextSizeA(smallPx, Float.MAX_VALUE, 0.0F, dist);
      float gap = 5.0F * scale;
      float padX = 5.0F * scale;
      float padY = 2.0F * scale;
      float textW = nameSize.x + gap + distSize.x;
      float boxW = textW + padX * 2.0F;
      float boxH = Math.max(nameSize.y, distSize.y) + padY * 2.0F;

      // Diamond on the waypoint itself, label box just above it.
      float d = 5.0F * scale;
      dl.addQuadFilled(sx, sy - d - 1.0F, sx + d + 1.0F, sy, sx, sy + d + 1.0F, sx - d - 1.0F, sy, 0xC0000000);
      dl.addQuadFilled(sx, sy - d, sx + d, sy, sx, sy + d, sx - d, sy, color);

      float bx = sx - boxW * 0.5F;
      float by = sy - d - 4.0F * scale - boxH;
      dl.addRectFilled(bx, by, bx + boxW, by + boxH, 0x99000000, 4.0F * scale);
      dl.addRectFilled(bx, by + boxH - 1.5F * scale, bx + boxW, by + boxH, color, 0.0F);
      float tx = bx + padX;
      dl.addText(font, fontPx, tx, by + padY + (boxH - padY * 2.0F - nameSize.y) * 0.5F, color, name);
      dl.addText(font, smallPx, tx + nameSize.x + gap, by + padY + (boxH - padY * 2.0F - distSize.y) * 0.5F, 0xFFDDDDDD, dist);
   }

   static String formatDistance(double meters) {
      if (meters >= 10000.0) {
         return String.format(java.util.Locale.ROOT, "%.1fkm", meters / 1000.0);
      }

      return Math.round(meters) + "m";
   }

   /** ARGB to ImGui's ABGR. */
   static int abgr(int argb) {
      return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
   }
}
