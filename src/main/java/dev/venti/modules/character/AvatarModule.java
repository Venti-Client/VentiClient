package dev.venti.modules.character;

import dev.venti.compat.Mc;
import dev.venti.config.ActionSetting;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.modules.character.kit.CharacterDesign;
import dev.venti.modules.character.kit.CharacterModel;
import dev.venti.modules.character.kit.Detail;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public final class AvatarModule extends Module {
   private static final float MODEL_PIXEL = 0.05859375F;
   private static volatile AvatarModule active;
   private final ModeSetting character = this.add("character", characterSetting());
   private final ModeSetting detail = this.add(
      "detail", new ModeSetting("Detail", Detail.MEDIUM.label, Detail.LOW.label, Detail.MEDIUM.label, Detail.HIGH.label)
   );
   private final BooleanSetting heldItems = this.add("held-items", new BooleanSetting("Held Items", false));
   private int scanTicks;
   private WeakReference<ClientLevel> level = new WeakReference<>(null);

   public AvatarModule() {
      super("avatar", "Avatar", "Replaces your player model with a 3D character.", ModuleCategory.RENDER);
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
         this.chat(Component.literal("Couldn't create ").withStyle(ChatFormatting.RED).append(Component.literal(dir.toString()).withStyle(ChatFormatting.GRAY)));
         return;
      }

      this.refreshCharacters();
      Util.getPlatform().openPath(dir);
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
      this.level = new WeakReference<>(Minecraft.getInstance().level);
   }

   private void watchLevel() {
      ClientLevel current = Minecraft.getInstance().level;
      if (this.level.get() != current) {
         this.reset();
         CharacterCache.releaseTextures();
      }
   }

   public static void extract(Entity entity, AvatarRenderState state) {
      AvatarModule module = active;
      WornCharacter worn = module == null ? null : module.resolve(state);
      CharacterHolder holder = (CharacterHolder)state;
      holder.venti$setCharacter(worn);
      if (worn != null) {
         Entity vehicle = entity.getVehicle();
         holder.venti$setSeat(vehicle == null ? 0.0F : (vehicle instanceof LivingEntity ? 1.35F : 0.3F));
         if (state.nameTagAttachment != null) {
            CharacterModel model = worn.model;
            float over = model.maxY * model.scale - 32.0F;
            if (over > 0.0F) {
               state.nameTagAttachment = state.nameTagAttachment.add(0.0, over * 0.05859375F * state.scale, 0.0);
            }
         }
      }
   }

   static void baking(String file) {
      AvatarModule module = active;
      if (module != null) {
         module.chat(gray("Preparing ").append(name(file)).append(gray(", big models take a few seconds the first time")));
      }
   }

   static void notice(String file, List<String> limbs) {
      AvatarModule module = active;
      if (module != null) {
         Component body = limbs.isEmpty()
            ? name(file).append(gray(" has no skeleton the client recognises, so it moves as one piece"))
            : name(file)
               .append(gray(" only moves its "))
               .append(Component.literal(String.join(" and ", limbs)).withStyle(ChatFormatting.YELLOW))
               .append(gray(", the rest of its skeleton wasn't recognised"));
         module.chat(body);
      }
   }

   static void failed(CharacterDesign design, Throwable failure) {
      AvatarModule module = active;
      if (module != null) {
         Throwable cause = failure instanceof UncheckedIOException && failure.getCause() != null ? failure.getCause() : failure;
         String reason = !(cause instanceof IOException) && !(cause instanceof IllegalStateException)
            ? "the file couldn't be read, see the log"
            : cause.getMessage();
         module.chat(gray("Couldn't load ").append(name(design.name())).append(gray(": ")).append(Component.literal(reason).withStyle(ChatFormatting.RED)));
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

   public static boolean hidesLayer(RenderLayer<?, ?> layer, EntityRenderState state) {
      return worn(state) == null
         ? false
         : layer instanceof HumanoidArmorLayer
            || layer instanceof CapeLayer
            || layer instanceof WingsLayer
            || layer instanceof StuckInBodyLayer
            || layer instanceof CustomHeadLayer
            || layer instanceof ParrotOnShoulderLayer
            || layer instanceof Deadmau5EarsLayer
            || layer instanceof ItemInHandLayer && !showsHeldItems();
   }

   private static boolean showsHeldItems() {
      AvatarModule module = active;
      return module != null && module.heldItems.get();
   }

   static WornCharacter firstPerson() {
      AvatarModule module = active;
      if (module == null) {
         return null;
      } else {
         LocalPlayer self = Minecraft.getInstance().player;
         if (self != null && !self.isSpectator()) {
            WornCharacter worn = CharacterCache.ready(CharacterCatalog.byName(module.character.get()), module.effectiveDetail());
            return worn != null && worn.model.firstPersonArms ? worn : null;
         } else {
            return null;
         }
      }
   }

   private WornCharacter resolve(AvatarRenderState state) {
      if (state.isSpectator) {
         return null;
      } else {
         Minecraft mc = Minecraft.getInstance();
         return !this.wears(mc, state) ? null : CharacterCache.ready(CharacterCatalog.byName(this.character.get()), this.effectiveDetail());
      }
   }

   private boolean wears(Minecraft mc, AvatarRenderState state) {
      LocalPlayer self = mc.player;
      if (self != null && mc.level != null) {
         if (state.id != self.getId()) {
            return false;
         } else {
            return mc.options.getCameraType().isFirstPerson() ? Mc.screen() != null && !self.isSleeping() : true;
         }
      } else {
         return false;
      }
   }

   private static net.minecraft.network.chat.MutableComponent gray(String text) {
      return Component.literal(text).withStyle(ChatFormatting.GRAY);
   }

   private static net.minecraft.network.chat.MutableComponent name(String text) {
      return Component.literal(text).withStyle(ChatFormatting.AQUA);
   }
}
