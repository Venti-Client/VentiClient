package dev.venti.modules.character;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import dev.venti.modules.character.kit.CharacterDesign;
import dev.venti.modules.character.kit.CharacterModel;
import dev.venti.modules.character.kit.Detail;
import dev.venti.config.ActionSetting;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckObjectsFeatureRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.ShoulderParrotFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;

@Environment(EnvType.CLIENT)
public final class CharacterModule extends Module {
   private static final float MODEL_PIXEL = 0.05859375F;
   private static volatile CharacterModule active;
   private final ModeSetting character = this.add("character", characterSetting());
   private final ModeSetting detail = this.add(
      "detail", new ModeSetting("Detail", Detail.MEDIUM.label, Detail.LOW.label, Detail.MEDIUM.label, Detail.HIGH.label)
   );
   private final BooleanSetting heldItems = this.add("held-items", new BooleanSetting("Held Items", false));
   private int scanTicks;
   private WeakReference<ClientWorld> level = new WeakReference<>(null);

   public CharacterModule() {
      super("character-3d", "3D Character", "Replaces your player model with a 3D character.", ModuleCategory.RENDER);
      this.add("characters-folder", new ActionSetting("Open Characters Folder", this::openFolder));
      this.character.onChange(this::prepare);
      this.detail.onChange(this::prepare);
   }

   private static ModeSetting characterSetting() {
      CharacterCatalog.refresh();
      List<String> names = CharacterCatalog.names();
      return new ModeSetting("Character", names.get(0), names.toArray(new String[0]));
   }

   private void refreshCharacters() {
      if (CharacterCatalog.refresh()) {
         this.character.setModes(CharacterCatalog.names());
      }
   }

   private void openFolder() {
      Path dir = CustomCharacters.folder();

      try {
         Files.createDirectories(dir);
      } catch (IOException var3) {
         this.chat(Text.literal("Couldn't create ").formatted(Formatting.RED).append(Text.literal(dir.toString()).formatted(Formatting.GRAY)));
         return;
      }

      this.refreshCharacters();
      Util.getOperatingSystem().open(dir);
   }

   private void prepare() {
      if (this.isEnabled()) {
         CharacterCache.prepare(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private Detail effectiveDetail() {
      return Detail.byLabel(this.detail.get());
   }

   @Override
   protected void onEnable() {
      CharacterCache.listen();
      this.reset();
      this.refreshCharacters();
      this.prepare();
      active = this;
   }

   @Override
   protected void onTick() {
      this.watchLevel();
      if (++this.scanTicks % 40 == 0) {
         this.refreshCharacters();
      }
   }

   @Override
   protected void onDisable() {
      if (active == this) {
         active = null;
      }

      this.reset();
      CharacterCache.clear();
   }

   private void reset() {
      CharacterMotion.clear();
      this.level = new WeakReference<>(MinecraftClient.getInstance().world);
   }

   private void watchLevel() {
      ClientWorld current = MinecraftClient.getInstance().world;
      if (this.level.get() != current) {
         this.reset();
         CharacterCache.releaseTextures();
      }
   }

   public static void extract(Entity entity, PlayerEntityRenderState state) {
      CharacterModule module = active;
      WornCharacter worn = module == null ? null : module.resolve(state);
      CharacterHolder holder = (CharacterHolder)state;
      holder.venti$setCharacter(worn);
      if (worn != null) {
         Entity vehicle = entity.getVehicle();
         holder.venti$setSeat(vehicle == null ? 0.0F : (vehicle instanceof LivingEntity ? 1.35F : 0.3F));
         if (state.nameLabelPos != null) {
            CharacterModel model = worn.model;
            float over = model.maxY * model.scale - 32.0F;
            if (over > 0.0F) {
               state.nameLabelPos = state.nameLabelPos.add(0.0, over * 0.05859375F * state.baseScale, 0.0);
            }
         }
      }
   }

   static void baking(String file) {
      CharacterModule module = active;
      if (module != null) {
         module.chat(gray("Preparing ").append(name(file)).append(gray(", big models take a few seconds the first time")));
      }
   }

   static void notice(String file, List<String> limbs) {
      CharacterModule module = active;
      if (module != null) {
         Text body = limbs.isEmpty()
            ? name(file).append(gray(" has no skeleton the client recognises, so it moves as one piece"))
            : name(file)
               .append(gray(" only moves its "))
               .append(Text.literal(String.join(" and ", limbs)).formatted(Formatting.YELLOW))
               .append(gray(", the rest of its skeleton wasn't recognised"));
         module.chat(body);
      }
   }

   static void failed(CharacterDesign design, Throwable failure) {
      CharacterModule module = active;
      if (module != null) {
         Throwable cause = failure instanceof UncheckedIOException && failure.getCause() != null ? failure.getCause() : failure;
         String reason = !(cause instanceof IOException) && !(cause instanceof IllegalStateException)
            ? "the file couldn't be read, see the log"
            : cause.getMessage();
         module.chat(gray("Couldn't load ").append(name(design.name())).append(gray(": ")).append(Text.literal(reason).formatted(Formatting.RED)));
      }
   }

   static WornCharacter worn(EntityRenderState state) {
      return state instanceof CharacterHolder holder ? holder.venti$character() : null;
   }

   static float seat(EntityRenderState state) {
      return state instanceof CharacterHolder holder ? holder.venti$seat() : 0.0F;
   }

   public static boolean hidesModel(EntityRenderState state) {
      return worn(state) != null;
   }

   public static boolean hidesLayer(FeatureRenderer<?, ?> layer, EntityRenderState state) {
      return worn(state) == null
         ? false
         : layer instanceof ArmorFeatureRenderer
            || layer instanceof CapeFeatureRenderer
            || layer instanceof ElytraFeatureRenderer
            || layer instanceof StuckObjectsFeatureRenderer
            || layer instanceof HeadFeatureRenderer
            || layer instanceof ShoulderParrotFeatureRenderer
            || layer instanceof Deadmau5FeatureRenderer
            || layer instanceof HeldItemFeatureRenderer && !showsHeldItems();
   }

   private static boolean showsHeldItems() {
      CharacterModule module = active;
      return module != null && module.heldItems.get();
   }

   static WornCharacter firstPerson() {
      CharacterModule module = active;
      if (module == null) {
         return null;
      } else {
         ClientPlayerEntity self = MinecraftClient.getInstance().player;
         if (self != null && !self.isSpectator()) {
            WornCharacter worn = CharacterCache.ready(CharacterCatalog.byName(module.character.get()), module.effectiveDetail());
            return worn != null && worn.model.firstPersonArms ? worn : null;
         } else {
            return null;
         }
      }
   }

   private WornCharacter resolve(PlayerEntityRenderState state) {
      if (state.spectator) {
         return null;
      } else {
         MinecraftClient mc = MinecraftClient.getInstance();
         return !this.wears(mc, state) ? null : CharacterCache.ready(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private boolean wears(MinecraftClient mc, PlayerEntityRenderState state) {
      ClientPlayerEntity self = mc.player;
      if (self != null && mc.world != null) {
         if (state.id != self.getId()) {
            return false;
         } else {
            return mc.options.getPerspective().isFirstPerson() ? mc.currentScreen != null && !self.isSleeping() : true;
         }
      } else {
         return false;
      }
   }

   private static net.minecraft.text.MutableText gray(String text) {
      return Text.literal(text).formatted(Formatting.GRAY);
   }

   private static net.minecraft.text.MutableText name(String text) {
      return Text.literal(text).formatted(Formatting.AQUA);
   }
}
