package dev.venti.modules.totem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.venti.config.BooleanSetting;
import dev.venti.config.NumberSetting;
import dev.venti.config.StringSetting;
import dev.venti.gui.Theme;
import dev.venti.gui.image.PictureFrames;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Swaps the Totem of Undying texture for a picture or animated GIF. It writes a small resource pack in
 * config/venti/totem-pack that is always loaded on top, so the totem changes in hand, in inventories and in
 * the pop animation alike.
 */
public final class CustomTotemModule extends Module {
   public static final String PACK_ID = "venti:custom_totem";
   private static final String TEXTURE = "assets/minecraft/textures/item/totem_of_undying.png";
   private static final int MAX_FRAMES = 64;
   private final StringSetting image = this.add("image", new StringSetting("Totem Image", "").files(Theme.IMAGE_TYPES));
   private final NumberSetting resolution = this.add("resolution", new NumberSetting("Resolution", 128.0F, 16.0F, 256.0F).step(16.0F));
   private final BooleanSetting animate = this.add("animate", new BooleanSetting("Play GIF", true));
   private int reloadIn = -1;

   public CustomTotemModule() {
      super("custom-totem", "Custom Totem", "Sets the totem of undying (and its pop animation) to any picture or animated GIF you choose.", ModuleCategory.RENDER);
      this.image.onChange(this::scheduleRebuild);
      this.resolution.onChange(this::scheduleRebuild);
      this.animate.onChange(this::scheduleRebuild);
   }

   public static Path packDir() {
      return FabricLoader.getInstance().getConfigDir().resolve("venti").resolve("totem-pack");
   }

   /** The always-on pack profile, added to the client's pack list by ResourcePackManagerMixin. */
   public static ResourcePackProfile profile() {
      Path dir = packDir();
      writeMeta(dir);
      ResourcePackInfo info = new ResourcePackInfo(PACK_ID, Text.literal("Venti Custom Totem"), ResourcePackSource.BUILTIN, java.util.Optional.empty());
      ResourcePackPosition position = new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, true);
      return ResourcePackProfile.create(info, new DirectoryResourcePack.DirectoryBackedFactory(dir), ResourceType.CLIENT_RESOURCES, position);
   }

   /** Writes the texture for the saved settings before the first resource load, so no reload is needed at startup. */
   public void prepare() {
      this.rebuild(false);
   }

   @Override
   protected void onEnable() {
      this.scheduleRebuild();
   }

   @Override
   protected void onDisable() {
      this.scheduleRebuild();
   }

   @Override
   protected void onTick() {
   }

   /** Settings fire on every slider step, so wait a moment before the (slow) resource reload. */
   private void scheduleRebuild() {
      this.reloadIn = 10;
   }

   /** Called every client tick, even while disabled, so turning the module off restores the vanilla totem. */
   public void tickReload() {
      if (this.reloadIn > 0 && --this.reloadIn == 0) {
         this.reloadIn = -1;
         this.rebuild(true);
      }
   }

   private void rebuild(boolean reload) {
      Path texture = packDir().resolve(TEXTURE);
      Path animation = packDir().resolve(TEXTURE + ".mcmeta");
      try {
         Files.deleteIfExists(texture);
         Files.deleteIfExists(animation);
         if (this.isEnabled() && !this.image.get().isBlank()) {
            this.writeTexture(texture, animation);
         }
      } catch (IOException | RuntimeException e) {
         LOGGER.warn("[Venti] Custom Totem could not build its texture", e);
         this.chat(Text.literal("couldn't load " + this.image.get()).formatted(Formatting.RED));
      }

      if (reload && MinecraftClient.getInstance().getResourceManager() != null) {
         MinecraftClient.getInstance().reloadResources();
      }
   }

   /** Frames are stacked top to bottom on square cells, the layout vanilla animated sprites use. */
   private void writeTexture(Path texture, Path animation) throws IOException {
      int side = Math.round(this.resolution.get());
      List<PictureFrames.Frame> frames = PictureFrames.load(this.image.get(), side, this.animate.get() ? MAX_FRAMES : 1);
      Files.createDirectories(texture.getParent());
      try (NativeImage sheet = new NativeImage(side, side * frames.size(), true)) {
         for (int f = 0; f < frames.size(); f++) {
            PictureFrames.Frame frame = frames.get(f);
            int left = (side - frame.width()) / 2;
            int top = f * side + (side - frame.height()) / 2;
            for (int y = 0; y < frame.height(); y++) {
               for (int x = 0; x < frame.width(); x++) {
                  sheet.setColorArgb(left + x, top + y, frame.argb()[y * frame.width() + x]);
               }
            }
         }

         sheet.writeTo(texture);
      }

      if (frames.size() > 1) {
         JsonArray order = new JsonArray();
         for (int f = 0; f < frames.size(); f++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("index", f);
            entry.addProperty("time", Math.max(1, Math.round(frames.get(f).delayMs() / 50.0F)));
            order.add(entry);
         }

         JsonObject anim = new JsonObject();
         anim.addProperty("frametime", 1);
         anim.add("frames", order);
         JsonObject root = new JsonObject();
         root.add("animation", anim);
         Files.writeString(animation, root.toString(), StandardCharsets.UTF_8);
      }
   }

   private static void writeMeta(Path dir) {
      try {
         Files.createDirectories(dir);
         int format = SharedConstants.RESOURCE_PACK_VERSION;
         JsonObject pack = new JsonObject();
         pack.addProperty("description", "Venti custom totem");
         pack.addProperty("pack_format", format);
         pack.addProperty("min_format", format);
         pack.addProperty("max_format", format);
         JsonObject root = new JsonObject();
         root.add("pack", pack);
         Files.writeString(dir.resolve("pack.mcmeta"), root.toString(), StandardCharsets.UTF_8);
      } catch (IOException e) {
         LOGGER.warn("[Venti] Custom Totem could not write its resource pack", e);
      }
   }

   public static CustomTotemModule get() {
      return Modules.get(CustomTotemModule.class);
   }
}
