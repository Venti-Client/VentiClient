package dev.venti.modules.view;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.venti.compat.Mc;
import dev.venti.config.BooleanSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=26.2 {
import net.minecraft.util.LightCoordsUtil;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
*///?}
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//? if >=26.2 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Restyles the player nametags vanilla already decided to draw (Anubis panel look).
 *
 * <p>Fair by construction: it only runs inside vanilla's {@code renderLabelIfPresent} for players whose render state has a
 * vanilla display name (so vanilla's distance, invisibility, sneaking, team-visibility and F1 rules all apply), it hides the
 * tag when blocks stand between the camera and the player, and it draws everything depth-tested (never see-through).
 * It shows only the name, held item, ping and distance.
 */
public final class NametagsModule extends Module {
   private static final int PANEL_RGB = 0x16181D;
   private static final int BORDER = 0x18FFFFFF;
   private static final int TEXT_NAME = 0xFFE9EAEE;
   private static final int TEXT_LABEL = 0xFFA3A3AB;
   private static final int STATUS_GOOD = 0xFF31D158;
   private static final int STATUS_WARN = 0xFFFFD60A;
   private static final int STATUS_BAD = 0xFFFF453A;
   private static final int PADDING = 4;
   private static final int ROW_GAP = 3;
   private static final int ICON_SIZE = 10;
   private static final int TEXT_GAP = 4;
   private static NametagsModule instance;
   private final BooleanSetting ping = this.add("ping", new BooleanSetting("Ping", true));
   private final BooleanSetting heldItem = this.add("held-item", new BooleanSetting("Held Item", true));
   private final BooleanSetting distance = this.add("distance", new BooleanSetting("Distance", true));
   private final NumberSetting scale = this.add("scale", new NumberSetting("Scale", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final NumberSetting backgroundAlpha = this.add("background-alpha", new NumberSetting("Background Alpha", 0.45F, 0.0F, 1.0F).step(0.05F));

   public NametagsModule() {
      super("nametags", "Nametags", "Restyles vanilla player nametags with held item, ping and distance. Never shows through walls.", ModuleCategory.VISUALS);
      instance = this;
   }

   public static boolean active() {
      NametagsModule module = instance;
      return module != null && module.isEnabled();
   }

   /** Per-frame data captured while vanilla builds the player's render state. */
   public static final class Tag {
      boolean visible;
      String pingText;
      int pingColor;
      String distanceText;
      ItemStackRenderState item;

      /** False when blocks hide the player from the camera; the tag is then not drawn at all. */
      public boolean visible() {
         return this.visible;
      }
   }

   /**
    * Called at the end of PlayerEntityRenderer.updateRenderState, after vanilla decided whether this player has a nametag
    * ({@code state.displayName}). Returns null when there is nothing for the module to restyle.
    */
   public static Tag capture(AbstractClientPlayer player, AvatarRenderState state, float tickProgress) {
      NametagsModule module = instance;
      Minecraft mc = Minecraft.getInstance();
      if (module == null || !module.isEnabled() || state.nameTag == null || mc.level == null) {
         return null;
      }

      Tag tag = new Tag();
      Camera camera = Mc.camera();
      Vec3 eye = camera.position();
      tag.visible = canSee(mc.level, eye, player, tickProgress);
      if (!tag.visible) {
         return tag;
      }

      if (module.ping.get() && mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
         if (info != null) {
            int latency = info.getLatency();
            tag.pingText = latency + "ms";
            tag.pingColor = latency < 60 ? STATUS_GOOD : (latency < 130 ? STATUS_WARN : STATUS_BAD);
         }
      }

      if (module.distance.get()) {
         tag.distanceText = Math.round((float)Math.sqrt(state.distanceToCameraSq)) + "m";
      }

      if (module.heldItem.get()) {
         ItemStack main = player.getMainHandItem();
         if (!main.isEmpty()) {
            ItemStackRenderState item = new ItemStackRenderState();
            mc.getItemModelResolver().updateForLiving(item, main, ItemDisplayContext.GUI, player);
            if (!item.isEmpty()) {
               tag.item = item;
            }
         }
      }

      return tag;
   }

   /**
    * Line of sight from the camera to the player's head, eyes or body centre; any one clear ray counts.
    * Uses the camera-collision ("visual") block shapes, so glass and similar see-through blocks do not hide the tag.
    */
   private static boolean canSee(Level world, Vec3 from, AbstractClientPlayer player, float tickProgress) {
      Vec3 base = player.getPosition(tickProgress);
      double height = player.getBbHeight();
      Vec3[] targets = {
         base.add(0.0, height + 0.1, 0.0),
         base.add(0.0, player.getEyeHeight(), 0.0),
         base.add(0.0, height * 0.5, 0.0)
      };
      Minecraft mc = Minecraft.getInstance();
      for (Vec3 to : targets) {
         ClipContext context = new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.getCameraEntity() != null ? mc.getCameraEntity() : player);
         if (world.clip(context).getType() == HitResult.Type.MISS) {
            return true;
         }
      }

      return false;
   }

   /** Draws the restyled tag at vanilla's label anchor. Everything uses depth-tested layers (no see-through pass). */
   public static void render(AvatarRenderState state, Tag tag, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
      NametagsModule module = instance;
      if (module == null || state.nameTagAttachment == null) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      Font font = mc.font;
      Component name = state.nameTag;
      int nameW = font.width(name);
      int pingW = tag.pingText != null ? font.width(tag.pingText) : 0;
      int distW = tag.distanceText != null ? font.width(tag.distanceText) : 0;
      boolean hasItem = tag.item != null;
      boolean secondRow = hasItem || tag.distanceText != null;
      int row1W = nameW + (tag.pingText != null ? 5 + pingW : 0);
      int row2W = (hasItem ? ICON_SIZE : 0) + (hasItem && distW > 0 ? TEXT_GAP : 0) + distW;
      int panelW = Math.max(row1W, row2W) + PADDING * 2;
      int panelH = PADDING * 2 + 9 + (secondRow ? ROW_GAP + ICON_SIZE : 0);
      float fade = state.isDiscrete ? 0.5F : 1.0F;
      float surface = Math.max(0.0F, Math.min(1.0F, module.backgroundAlpha.get())) * fade;
      float chrome = Math.min(1.0F, surface * 2.0F);
      boolean shadow = surface < 0.55F;
      //? if >=26.2 {
      int light = LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 2);
      //?} else {
      /*int light = LightTexture.lightCoordsWithEmission(state.lightCoords, 2);
      *///?}
      float s = 0.025F * Math.max(0.5F, Math.min(2.0F, module.scale.get()));

      matrices.pushPose();
      matrices.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5, state.nameTagAttachment.z);
      matrices.mulPose(cameraState.orientation);
      matrices.scale(s, -s, s);
      int x = -panelW / 2;
      int y = 9 + PADDING - panelH;

      // Panel slightly behind the text plane so the text never z-fights with it.
      matrices.pushPose();
      matrices.translate(0.0F, 0.0F, -0.5F);
      int panel = Math.round(surface * 255.0F) << 24 | PANEL_RGB;
      int border = scaleAlpha(BORDER, chrome);
      queue.submitCustomGeometry(matrices, RenderTypes.textBackground(), (entry, vc) -> {
         Matrix4f m = entry.pose();
         roundedRect(vc, m, x, y, x + panelW, y + panelH, panel, light);
         roundedBorder(vc, m, x, y, x + panelW, y + panelH, border, light);
      });
      matrices.popPose();

      int left = x + PADDING;
      int textY = y + PADDING;
      queue.submitText(matrices, left, textY, name.getVisualOrderText(), shadow, DisplayMode.NORMAL, light, scaleAlpha(TEXT_NAME, fade), 0, 0);
      if (tag.pingText != null) {
         queue.submitText(matrices, left + nameW + 5, textY, Component.literal(tag.pingText).getVisualOrderText(), shadow, DisplayMode.NORMAL, light, scaleAlpha(tag.pingColor, fade), 0, 0);
      }

      if (secondRow) {
         int rowY = textY + 9 + ROW_GAP;
         int cursor = left;
         if (hasItem) {
            matrices.pushPose();
            matrices.translate(cursor + ICON_SIZE / 2.0F, rowY + ICON_SIZE / 2.0F, 0.5F);
            // Same orientation as GUI item icons; depth squashed so 3D block models stay in front of the panel.
            matrices.scale(ICON_SIZE, -ICON_SIZE, 0.5F);
            tag.item.submit(matrices, queue, light, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
            cursor += ICON_SIZE + TEXT_GAP;
         }

         if (tag.distanceText != null) {
            queue.submitText(matrices, cursor, rowY + 2, Component.literal(tag.distanceText).getVisualOrderText(), shadow, DisplayMode.NORMAL, light, scaleAlpha(TEXT_LABEL, fade), 0, 0);
         }
      }

      matrices.popPose();
   }

   private static int scaleAlpha(int argb, float factor) {
      int alpha = Math.round((argb >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, factor)));
      return alpha << 24 | argb & 0xFFFFFF;
   }

   private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, int color, int light) {
      if (x2 <= x1 || y2 <= y1) {
         return;
      }

      vc.addVertex(m, x1, y1, 0.0F).setColor(color).setLight(light);
      vc.addVertex(m, x1, y2, 0.0F).setColor(color).setLight(light);
      vc.addVertex(m, x2, y2, 0.0F).setColor(color).setLight(light);
      vc.addVertex(m, x2, y1, 0.0F).setColor(color).setLight(light);
   }

   private static void roundedRect(VertexConsumer vc, Matrix4f m, int x1, int y1, int x2, int y2, int color, int light) {
      if (color >>> 24 != 0) {
         quad(vc, m, x1 + 4, y1, x2 - 4, y1 + 1, color, light);
         quad(vc, m, x1 + 2, y1 + 1, x2 - 2, y1 + 2, color, light);
         quad(vc, m, x1 + 1, y1 + 2, x2 - 1, y1 + 4, color, light);
         quad(vc, m, x1, y1 + 4, x2, y2 - 4, color, light);
         quad(vc, m, x1 + 1, y2 - 4, x2 - 1, y2 - 2, color, light);
         quad(vc, m, x1 + 2, y2 - 2, x2 - 2, y2 - 1, color, light);
         quad(vc, m, x1 + 4, y2 - 1, x2 - 4, y2, color, light);
      }
   }

   private static void roundedBorder(VertexConsumer vc, Matrix4f m, int x1, int y1, int x2, int y2, int color, int light) {
      if (color >>> 24 != 0) {
         quad(vc, m, x1 + 4, y1, x2 - 4, y1 + 1, color, light);
         quad(vc, m, x1 + 4, y2 - 1, x2 - 4, y2, color, light);
         quad(vc, m, x1, y1 + 4, x1 + 1, y2 - 4, color, light);
         quad(vc, m, x2 - 1, y1 + 4, x2, y2 - 4, color, light);
         quad(vc, m, x1 + 2, y1 + 1, x1 + 4, y1 + 2, color, light);
         quad(vc, m, x1 + 1, y1 + 2, x1 + 2, y1 + 4, color, light);
         quad(vc, m, x2 - 4, y1 + 1, x2 - 2, y1 + 2, color, light);
         quad(vc, m, x2 - 2, y1 + 2, x2 - 1, y1 + 4, color, light);
         quad(vc, m, x1 + 1, y2 - 4, x1 + 2, y2 - 2, color, light);
         quad(vc, m, x1 + 2, y2 - 2, x1 + 4, y2 - 1, color, light);
         quad(vc, m, x2 - 2, y2 - 4, x2 - 1, y2 - 2, color, light);
         quad(vc, m, x2 - 4, y2 - 2, x2 - 2, y2 - 1, color, light);
      }
   }
}
