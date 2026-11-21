package dev.venti.modules.world;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ColorSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.AABB;

@Environment(EnvType.CLIENT)
public final class EffectParticlesModule extends Module {
   private static final String GLYPH_ANKH = "Charm";
   private static final String GLYPH_SCARAB = "Beetle";
   private static final String GLYPH_SAND = "Sand";
   private static final String GLYPH_SPARK = "Spark";
   private static final String GLYPH_MIXED = "Mixed";
   private static final int CAPACITY = 12000 / GlyphSwarm.WORST_QUADS / 10 * 10;
   private static final float BASE_SIZE = 0.085F;
   private static final double PROJECTILE_RANGE_SQR = 6400.0;
   private static final double RESTING_SQR = 9.0E-4;
   private static final double STEP_MIN_SPEED = 0.07;
   private static final int PEARL = 0;
   private static final int ARROW = 1;
   private static final int TRIDENT = 2;
   private static final int NONE = -1;
   private static final byte[] MIXED_ORDER = new byte[]{0, 3, 1, 3, 2, 3};
   private final BooleanSetting pops = this.add("pops", new BooleanSetting("Totem Pops", true));
   private final ModeSetting popGlyph = this.add("pop-shape", new ModeSetting("Pop Shape", "Mixed", "Mixed", "Charm", "Beetle", "Sand", "Spark"));
   private final NumberSetting popBurst = this.add("pop-burst", new NumberSetting("Pop Burst", 26.0F, 4.0F, 80.0F).step(1.0F));
   private final BooleanSetting ownPops = this.add("own-pops", new BooleanSetting("Own Pops", true));
   private final BooleanSetting projectiles = this.add("projectiles", new BooleanSetting("Projectiles", true));
   private final BooleanSetting pearls = this.add("pearls", new BooleanSetting("Pearls", true));
   private final BooleanSetting arrows = this.add("arrows", new BooleanSetting("Arrows", true));
   private final BooleanSetting tridents = this.add("tridents", new BooleanSetting("Tridents", true));
   private final ModeSetting trailGlyph = this.add("trail-shape", new ModeSetting("Trail Shape", "Spark", "Spark", "Sand", "Charm", "Beetle", "Mixed"));
   private final NumberSetting trailDensity = this.add("trail-density", new NumberSetting("Trail Density", 2.0F, 1.0F, 6.0F).step(1.0F));
   private final BooleanSetting footsteps = this.add("footsteps", new BooleanSetting("Footsteps", false));
   private final ModeSetting stepGlyph = this.add("step-shape", new ModeSetting("Step Shape", "Sand", "Sand", "Spark", "Charm", "Beetle", "Mixed"));
   private final NumberSetting stepDensity = this.add("step-density", new NumberSetting("Step Density", 2.0F, 1.0F, 6.0F).step(1.0F));
   private final NumberSetting size = this.add("size", new NumberSetting("Size", 1.0F, 0.3F, 2.5F).step(0.05F));
   private final NumberSetting lifetime = this.add("lifetime", new NumberSetting("Lifetime", 1.4F, 0.3F, 4.0F).step(0.1F));
   private final NumberSetting limit = this.add("limit", new NumberSetting("Limit", 240.0F, 40.0F, CAPACITY).step(10.0F));
   private final BooleanSetting halo = this.add("halo", new BooleanSetting("Halo", true));
   private final ModeSetting colorMode = this.add("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting mainColor = this.add("main-color", new ColorSetting("Main Color", -12548362));
   private final ColorSetting secondColor = this.add("second-color", new ColorSetting("Second Color", -14992226));
   private final GlyphSwarm swarm = new GlyphSwarm(CAPACITY);
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphTint palette = new GlyphTint();
   private final RandomSource random = RandomSource.create();
   private final Consumer<LivingEntity> popSink = this::burst;
   private ClientLevel boundLevel;
   private long lastFrame;

   public EffectParticlesModule() {
      super("effect-particles", "Effect Particles", "Stylized particles for totem pops, projectiles and footsteps.", ModuleCategory.VISUALS);
      this.popGlyph.visibleWhen(this.pops::get);
      this.popBurst.visibleWhen(this.pops::get);
      this.ownPops.visibleWhen(this.pops::get);
      this.pearls.visibleWhen(this.projectiles::get);
      this.arrows.visibleWhen(this.projectiles::get);
      this.tridents.visibleWhen(this.projectiles::get);
      this.trailGlyph.visibleWhen(this.projectiles::get);
      this.trailDensity.visibleWhen(this.projectiles::get);
      this.stepGlyph.visibleWhen(this.footsteps::get);
      this.stepDensity.visibleWhen(this.footsteps::get);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.swarm.clear();
      this.boundLevel = null;
      this.lastFrame = 0L;
   }

   private boolean bind(ClientLevel level) {
      if (level == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (level != this.boundLevel) {
            this.swarm.clear();
            this.boundLevel = level;
         }

         return true;
      }
   }

   public void onTotemPop(LivingEntity entity) {
      if (this.pops.get()) {
         this.popSink.accept(entity);
      }
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (this.bind(mc.level) && player != null) {
         this.refreshPalette();
         if (this.projectiles.get()) {
            this.trails(mc.level, player);
         }

         if (this.footsteps.get()) {
            this.steps(player);
         }
      }
   }

   public void render3D(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (this.bind(mc.level)) {
         long now = System.nanoTime();
         float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
         this.lastFrame = now;
         if (mc.isPaused()) {
            dt = 0.0F;
         }

         this.swarm.trim(this.particleLimit());
         this.swarm.step(dt);
         if (this.swarm.count() != 0) {
            this.canvas.begin(mc.gameRenderer.getMainCamera());
            this.swarm.draw(this.canvas, 1.0F, this.halo.get());
            this.canvas.submit(false);
         }
      }
   }

   private void burst(LivingEntity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (this.bind(mc.level) && entity.level() == mc.level) {
         if (entity != mc.player || this.ownPops.get()) {
            this.refreshPalette();
            AABB box = entity.getBoundingBox();
            int total = Math.round(this.popBurst.get());
            String mode = this.popGlyph.get();

            for (int i = 0; i < total; i++) {
               byte kind = pick(mode, i);
               double px = box.minX + box.getXsize() * this.random.nextDouble();
               double py = box.minY + box.getYsize() * (0.15 + 0.8 * this.random.nextDouble());
               double pz = box.minZ + box.getZsize() * this.random.nextDouble();
               float heading = this.random.nextFloat() * (float) (Math.PI * 2);
               float out = outward(kind) * (0.45F + this.random.nextFloat());
               float up = upward(kind) * (0.55F + 0.8F * this.random.nextFloat());
               this.emit(
                  kind, px, py, pz, Mth.cos(heading) * out, up, Mth.sin(heading) * out, 1.0F, 1.0F, this.colorFor(kind, -1)
               );
            }
         }
      }
   }

   private void trails(ClientLevel level, LocalPlayer player) {
      int density = Math.round(this.trailDensity.get());
      String mode = this.trailGlyph.get();

      for (Entity entity : level.entitiesForRendering()) {
         int role = this.role(entity);
         if (role != -1) {
            double dx = entity.getX() - entity.xo;
            double dy = entity.getY() - entity.yo;
            double dz = entity.getZ() - entity.zo;
            if (!(dx * dx + dy * dy + dz * dz < 9.0E-4) && !(entity.distanceToSqr(player) > 6400.0)) {
               double mid = entity.getBbHeight() * 0.5;

               for (int i = 0; i < density; i++) {
                  float f = (i + this.random.nextFloat()) / density;
                  byte kind = pick(mode, entity.tickCount * density + i);
                  this.emit(
                     kind,
                     entity.xo + dx * f + this.jitter(0.06F),
                     entity.yo + dy * f + mid + this.jitter(0.06F),
                     entity.zo + dz * f + this.jitter(0.06F),
                     this.jitter(0.3F),
                     this.jitter(0.3F) + 0.15F,
                     this.jitter(0.3F),
                     0.55F,
                     0.8F,
                     this.colorFor(kind, role)
                  );
               }
            }
         }
      }
   }

   private void steps(LocalPlayer player) {
      if (player.onGround() && !player.isSpectator() && !player.isPassenger() && !player.isInLiquid()) {
         double dx = player.getX() - player.xo;
         double dz = player.getZ() - player.zo;
         double speed = Math.sqrt(dx * dx + dz * dz);
         if (!(speed < 0.07)) {
            int count = Math.round(this.stepDensity.get()) + (player.isSprinting() ? 1 : 0);
            String mode = this.stepGlyph.get();

            for (int i = 0; i < count; i++) {
               byte kind = pick(mode, player.tickCount * count + i);
               double back = 0.2 + 0.3 * this.random.nextDouble();
               this.emit(
                  kind,
                  player.getX() - dx / speed * back + this.jitter(0.22F),
                  player.getY() + 0.04,
                  player.getZ() - dz / speed * back + this.jitter(0.22F),
                  (float)(-dx * 7.0) + this.jitter(0.5F),
                  0.8F + this.random.nextFloat() * 1.1F,
                  (float)(-dz * 7.0) + this.jitter(0.5F),
                  0.7F,
                  0.75F,
                  this.colorFor(kind, -1)
               );
            }
         }
      }
   }

   private void emit(byte kind, double x, double y, double z, float vx, float vy, float vz, float lifeScale, float sizeScale, int rgb) {
      float life = this.lifetime.get() * lifeScale * (0.75F + 0.5F * this.random.nextFloat());
      float scale = 0.085F * this.size.get() * sizeScale * sizeOf(kind) * (0.8F + 0.4F * this.random.nextFloat());
      float turn = spinOf(kind) * (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.5F + this.random.nextFloat());
      float angle = kind == 0 ? this.jitter(0.35F) : this.random.nextFloat() * (float) (Math.PI * 2);
      this.swarm.spawn(kind, x, y, z, vx, vy, vz, life, scale, angle, turn, this.random.nextFloat(), rgb, this.particleLimit());
   }

   private int particleLimit() {
      return Math.round(this.limit.get());
   }

   private void refreshPalette() {
      this.palette.resolve(this.colorMode.get(), WorldAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
   }

   private int role(Entity entity) {
      if (entity.isRemoved()) {
         return -1;
      } else if (entity instanceof ThrownEnderpearl) {
         return this.pearls.get() ? 0 : -1;
      } else if (entity instanceof ThrownTrident) {
         return this.tridents.get() ? 2 : -1;
      } else if (entity instanceof AbstractArrow) {
         return this.arrows.get() ? 1 : -1;
      } else {
         return -1;
      }
   }

   private static byte pick(String mode, int index) {
      return switch (mode) {
         case "Charm" -> 0;
         case "Beetle" -> 1;
         case "Sand" -> 2;
         case "Spark" -> 3;
         default -> MIXED_ORDER[Math.floorMod(index, MIXED_ORDER.length)];
      };
   }

   private int colorFor(byte kind, int role) {
      float r = this.random.nextFloat();

      int base = switch (kind) {
         case 0 -> r < 0.6F ? this.palette.primary() : this.palette.highlight();
         case 1 -> r < 0.55F ? this.palette.secondary() : this.palette.companion();
         case 2 -> this.dust(r);
         default -> GlyphTint.mix(this.palette.highlight(), -1, r * 0.35F);
      };

      return switch (role) {
         case 0 -> GlyphTint.mix(base, this.palette.companion(), 0.55F);
         case 2 -> GlyphTint.mix(base, this.palette.secondary(), 0.45F);
         default -> base;
      };
   }

   private int dust(float r) {
      int dust = this.palette.dust();
      if (r < 0.4F) {
         return dust;
      } else {
         return r < 0.75F ? GlyphTint.lift(dust, 0.4F) : GlyphTint.mix(dust, this.palette.secondary(), 0.3F);
      }
   }

   private static float sizeOf(byte kind) {
      return switch (kind) {
         case 0 -> 1.35F;
         case 1 -> 1.15F;
         case 2 -> 0.5F;
         default -> 0.9F;
      };
   }

   private static float spinOf(byte kind) {
      return switch (kind) {
         case 0 -> 0.5F;
         case 1 -> 1.1F;
         case 2 -> 5.0F;
         default -> 2.8F;
      };
   }

   private static float outward(byte kind) {
      return switch (kind) {
         case 0 -> 0.8F;
         case 1 -> 2.1F;
         case 2 -> 1.5F;
         default -> 3.4F;
      };
   }

   private static float upward(byte kind) {
      return switch (kind) {
         case 0 -> 1.1F;
         case 1 -> 1.7F;
         case 2 -> 2.6F;
         default -> 2.4F;
      };
   }

   private float jitter(float amount) {
      return (this.random.nextFloat() * 2.0F - 1.0F) * amount;
   }
}
