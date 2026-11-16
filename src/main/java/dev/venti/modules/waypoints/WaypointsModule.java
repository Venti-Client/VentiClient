package dev.venti.modules.waypoints;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.venti.config.BooleanSetting;
import dev.venti.config.NumberSetting;
import dev.venti.gui.CustomPage;
import dev.venti.imgui.ImGuiManager;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Per-world waypoints: labels and beams in the world, an "Add Waypoint" key, automatic death points,
 * a /waypoint command and an editable list on the module's GUI page.
 */
public final class WaypointsModule extends Module implements CustomPage {
   /** Equal to the category VentiClient registered (KeyBinding.Category.create throws on a second call). */
   //? if >=1.21.9 {
   private static final KeyBinding.Category CATEGORY = new KeyBinding.Category(Identifier.of("venti", "keys"));
   //?} else {
   /*private static final String CATEGORY = "key.category.venti.keys"; // categories are translation keys before 1.21.9
   *///?}
   private static final int DEATH_COLOR = 0xFFFF4444;
   private static final int[] PALETTE = {
      0xFF55FFFF, 0xFFFFAA00, 0xFF55FF55, 0xFFFF55FF, 0xFFFFFF55, 0xFF5599FF, 0xFFFF7777, 0xFFAAFFAA, 0xFFFFFFFF, 0xFFB088FF
   };

   private final BooleanSetting beam = this.add("beam", new BooleanSetting("Beam", true));
   private final BooleanSetting deathWaypoints = this.add("death-waypoints", new BooleanSetting("Death Waypoints", true));
   private final NumberSetting keepDeaths = this.add("keep-deaths", new NumberSetting("Keep Deaths", 3.0F, 1.0F, 10.0F).step(1.0F));
   private final BooleanSetting otherDimensions = this.add("other-dimensions", new BooleanSetting("Show Other Dimensions", false));
   private final BooleanSetting viewCone = this.add("only-in-view-cone", new BooleanSetting("Only In View Cone", false));
   private final NumberSetting coneAngle = this.add("cone-angle", new NumberSetting("Cone Angle", 30.0F, 5.0F, 90.0F).step(1.0F));
   private final NumberSetting maxDistance = this.add("max-distance", new NumberSetting("Max Distance", 0.0F, 0.0F, 20000.0F).step(50.0F));
   private final NumberSetting labelScale = this.add("label-scale", new NumberSetting("Label Scale", 1.0F, 0.5F, 2.0F).step(0.05F));

   private final WaypointStore store = new WaypointStore();
   private final KeyBinding addKey;
   private final ImString nameBuffer = new ImString(128);
   private final ImInt intBuffer = new ImInt();
   private final float[] colorBuffer = new float[4];
   private long lastDeathAt;

   public WaypointsModule() {
      super("waypoints", "Waypoints", "Per-world waypoints with labels, beams and death points. Add with B (Controls > Venti) or /waypoint.", ModuleCategory.MISC, true);
      this.keepDeaths.visibleWhen(this.deathWaypoints::get);
      this.coneAngle.visibleWhen(this.viewCone::get);
      this.keepDeaths.onChange(this::trimDeaths);
      this.nameBuffer.inputData.isResizable = true;
      this.addKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.venti.add_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));

      ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> this.store.load(client, handler.getServerInfo())));
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this.store::unload));
      ClientTickEvents.END_CLIENT_TICK.register(client -> this.store.tick());
      ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
         ClientCommandManager.literal("waypoint")
            .then(ClientCommandManager.literal("add")
               .executes(ctx -> this.commandAdd(ctx, null))
               .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                  .executes(ctx -> this.commandAdd(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(ClientCommandManager.literal("remove")
               .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                  .suggests((ctx, builder) -> {
                     String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
                     for (Waypoint w : this.store.all()) {
                        if (w.name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                           builder.suggest(w.name);
                        }
                     }

                     return builder.buildFuture();
                  })
                  .executes(ctx -> this.commandRemove(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(ClientCommandManager.literal("list").executes(this::commandList))
      ));
      ImGuiManager.getInstance().addHudRenderer(() -> WaypointRenderer.renderLabels(this), () -> WaypointRenderer.hasLabelWork(this));
   }

   public static WaypointsModule get() {
      return Modules.get(WaypointsModule.class);
   }

   WaypointStore store() {
      return this.store;
   }

   boolean beam() {
      return this.beam.get();
   }

   boolean showOtherDimensions() {
      return this.otherDimensions.get();
   }

   boolean onlyInViewCone() {
      return this.viewCone.get();
   }

   float coneAngle() {
      return this.coneAngle.get();
   }

   double maxDistance() {
      return this.maxDistance.get();
   }

   float labelScale() {
      return this.labelScale.get();
   }

   @Override
   protected void onEnable() {
      while (this.addKey.wasPressed()) {
      }
   }

   @Override
   protected void onTick() {
      MinecraftClient mc = MinecraftClient.getInstance();
      boolean pressed = false;
      while (this.addKey.wasPressed()) {
         pressed = true;
      }

      if (pressed && mc.currentScreen == null && mc.player != null && this.store.loaded()) {
         Waypoint w = this.addAtPlayer(null);
         if (w != null) {
            this.chat(Text.literal("added ").formatted(Formatting.GRAY).append(Text.literal(w.name).withColor(w.color & 0xFFFFFF))
               .append(Text.literal(" at " + w.coords()).formatted(Formatting.GRAY)));
         }
      }
   }

   /** Called from the world render hook (WorldHooks.render3D) while the module is on. */
   public void render3D(float partialTick) {
      WaypointRenderer.renderBeams(this, partialTick);
   }

   /** Called on the client thread when the server tells us our player died. */
   public void onPlayerDeath() {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity player = mc.player;
      if (!this.isEnabled() || !this.deathWaypoints.get() || player == null || mc.world == null || !this.store.loaded()) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - this.lastDeathAt < 1000L) {
         return;
      }

      this.lastDeathAt = now;
      Waypoint w = new Waypoint(
         "Death", player.getBlockX(), player.getBlockY(), player.getBlockZ(), mc.world.getRegistryKey().getValue().toString(), DEATH_COLOR, true
      );
      this.store.all().add(w);
      this.trimDeaths();
      this.store.changed();
      this.store.saveNow();
      this.chat(Text.literal("death point saved at " + w.coords()).formatted(Formatting.GRAY));
   }

   private void trimDeaths() {
      int keep = Math.round(this.keepDeaths.get());
      List<Waypoint> deaths = new ArrayList<>();
      for (Waypoint w : this.store.all()) {
         if (w.death) {
            deaths.add(w);
         }
      }

      if (deaths.size() > keep) {
         deaths.sort((a, b) -> Long.compare(a.created, b.created));
         for (int i = 0; i < deaths.size() - keep; i++) {
            this.store.all().remove(deaths.get(i));
         }

         this.store.changed();
      }
   }

   /** Adds a waypoint at the player's feet; a null name picks "Waypoint N". */
   private Waypoint addAtPlayer(String name) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ClientPlayerEntity player = mc.player;
      if (player == null || mc.world == null || !this.store.loaded()) {
         return null;
      }

      int normal = 0;
      for (Waypoint w : this.store.all()) {
         if (!w.death) {
            normal++;
         }
      }

      if (name == null || name.isBlank()) {
         int n = normal + 1;
         while (this.findByName("Waypoint " + n) != null) {
            n++;
         }

         name = "Waypoint " + n;
      }

      Waypoint w = new Waypoint(
         name.trim(), player.getBlockX(), player.getBlockY(), player.getBlockZ(), mc.world.getRegistryKey().getValue().toString(), PALETTE[normal % PALETTE.length], false
      );
      this.store.all().add(w);
      this.store.changed();
      ImGuiManager.getInstance().requestGlyphs(w.name);
      return w;
   }

   private Waypoint findByName(String name) {
      for (Waypoint w : this.store.all()) {
         if (w.name.equalsIgnoreCase(name)) {
            return w;
         }
      }

      return null;
   }

   // ---- /waypoint ----

   private int commandAdd(CommandContext<FabricClientCommandSource> ctx, String name) {
      if (!this.store.loaded()) {
         ctx.getSource().sendError(Text.literal("[Venti] Not in a world."));
         return 0;
      }

      Waypoint w = this.addAtPlayer(name);
      if (w == null) {
         ctx.getSource().sendError(Text.literal("[Venti] Could not add a waypoint here."));
         return 0;
      }

      ctx.getSource().sendFeedback(prefix().append(Text.literal("Added ").formatted(Formatting.GRAY))
         .append(Text.literal(w.name).withColor(w.color & 0xFFFFFF))
         .append(Text.literal(" at " + w.coords() + " (" + w.dimensionTag() + ")").formatted(Formatting.GRAY)));
      return Command.SINGLE_SUCCESS;
   }

   private int commandRemove(CommandContext<FabricClientCommandSource> ctx, String name) {
      Waypoint w = this.findByName(name.trim());
      if (w == null) {
         ctx.getSource().sendError(Text.literal("[Venti] No waypoint named \"" + name + "\"."));
         return 0;
      }

      this.store.all().remove(w);
      this.store.changed();
      ctx.getSource().sendFeedback(prefix().append(Text.literal("Removed " + w.name).formatted(Formatting.GRAY)));
      return Command.SINGLE_SUCCESS;
   }

   private int commandList(CommandContext<FabricClientCommandSource> ctx) {
      List<Waypoint> all = this.store.all();
      if (all.isEmpty()) {
         ctx.getSource().sendFeedback(prefix().append(Text.literal("No waypoints in this world.").formatted(Formatting.GRAY)));
         return Command.SINGLE_SUCCESS;
      }

      ctx.getSource().sendFeedback(prefix().append(Text.literal(all.size() + " waypoint(s):").formatted(Formatting.GRAY)));
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      String here = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getRegistryKey().getValue().toString() : "";
      for (Waypoint w : all) {
         String distance = "";
         if (player != null && here.equals(w.dimension)) {
            double dx = w.x + 0.5 - player.getX();
            double dy = w.y - player.getY();
            double dz = w.z + 0.5 - player.getZ();
            distance = ", " + WaypointRenderer.formatDistance(Math.sqrt(dx * dx + dy * dy + dz * dz));
         }

         ctx.getSource().sendFeedback(Text.literal(" - ").formatted(Formatting.DARK_GRAY)
            .append(Text.literal(w.name).withColor(w.color & 0xFFFFFF))
            .append(Text.literal(" " + w.coords() + " (" + w.dimensionTag() + distance + ")" + (w.visible ? "" : " [hidden]")).formatted(Formatting.GRAY)));
      }

      return Command.SINGLE_SUCCESS;
   }

   private static net.minecraft.text.MutableText prefix() {
      return Text.literal("[Venti] ").formatted(Formatting.LIGHT_PURPLE);
   }

   // ---- GUI page ----

   @Override
   public void renderPage(float width, float scale, int accent) {
      float startX = ImGui.getCursorPosX();
      float spacing = ImGui.getStyle().getItemSpacingX();
      if (!this.store.loaded()) {
         ImGui.textDisabled("Join a world to edit its waypoints.");
         return;
      }

      if (ImGui.button("Add at my position", width, 0.0F)) {
         this.addAtPlayer(null);
      }

      List<Waypoint> all = this.store.all();
      if (all.isEmpty()) {
         ImGui.setCursorPosX(startX);
         ImGui.textDisabled("No waypoints in this world yet.");
         return;
      }

      boolean wide = width >= 420.0F * scale;
      float frame = ImGui.getFrameHeight();
      Waypoint remove = null;
      for (int i = 0; i < all.size(); i++) {
         Waypoint w = all.get(i);
         ImGui.pushID(i);
         ImGui.setCursorPosX(startX);
         ImGui.separator();
         ImGui.setCursorPosX(startX);

         // Line 1: color, visible, name.
         this.colorBuffer[0] = (w.color >> 16 & 0xFF) / 255.0F;
         this.colorBuffer[1] = (w.color >> 8 & 0xFF) / 255.0F;
         this.colorBuffer[2] = (w.color & 0xFF) / 255.0F;
         this.colorBuffer[3] = (w.color >>> 24 & 0xFF) / 255.0F;
         if (ImGui.colorEdit4("##color", this.colorBuffer, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel | ImGuiColorEditFlags.AlphaBar)) {
            w.color = channel(this.colorBuffer[3]) << 24 | channel(this.colorBuffer[0]) << 16 | channel(this.colorBuffer[1]) << 8 | channel(this.colorBuffer[2]);
            this.store.changed();
         }

         ImGui.sameLine();
         if (ImGui.checkbox("##visible", w.visible)) {
            w.visible = !w.visible;
            this.store.changed();
         }

         ImGui.sameLine();
         this.nameBuffer.set(w.name);
         ImGui.setNextItemWidth(Math.max(40.0F * scale, startX + width - ImGui.getCursorPosX()));
         if (ImGui.inputText("##name", this.nameBuffer)) {
            w.name = this.nameBuffer.get();
            this.store.changed();
         }

         if (ImGui.isItemDeactivatedAfterEdit()) {
            ImGuiManager.getInstance().requestGlyphs(w.name);
         }

         // Line 2 (and 3 when narrow): x y z, dimension, copy, delete.
         ImGui.setCursorPosX(startX);
         String tag = w.dimensionTag() + (w.death ? " †" : "");
         float tagW = ImGui.calcTextSize(tag).x;
         float copyW = ImGui.calcTextSize("Copy").x + ImGui.getStyle().getFramePaddingX() * 2.0F;
         float delW = ImGui.calcTextSize("Del").x + ImGui.getStyle().getFramePaddingX() * 2.0F;
         float coordsW = wide ? width - tagW - copyW - delW - spacing * 3.0F : width;
         float fieldW = Math.max(24.0F * scale, (coordsW - spacing * 2.0F) / 3.0F);
         w.x = this.intField("##x", w.x, fieldW);
         ImGui.sameLine();
         w.y = this.intField("##y", w.y, fieldW);
         ImGui.sameLine();
         w.z = this.intField("##z", w.z, fieldW);
         if (wide) {
            ImGui.sameLine();
         } else {
            ImGui.setCursorPosX(startX);
         }

         ImGui.alignTextToFramePadding();
         ImGui.textDisabled(tag);
         ImGui.sameLine();
         if (!wide) {
            ImGui.setCursorPosX(startX + width - copyW - delW - spacing);
         }

         if (ImGui.button("Copy", copyW, frame)) {
            MinecraftClient.getInstance().keyboard.setClipboard(w.coords());
         }

         ImGui.sameLine();
         if (ImGui.button("Del", delW, frame)) {
            remove = w;
         }

         ImGui.popID();
      }

      if (remove != null) {
         all.remove(remove);
         this.store.changed();
      }
   }

   private int intField(String id, int value, float width) {
      this.intBuffer.set(value);
      ImGui.setNextItemWidth(width);
      if (ImGui.inputInt(id, this.intBuffer, 0, 0) && this.intBuffer.get() != value) {
         this.store.changed();
         return this.intBuffer.get();
      }

      return value;
   }

   private static int channel(float v) {
      return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
   }
}
