package dev.venti.modules.hitfx;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.config.NumberSetting;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import dev.venti.module.Modules;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/**
 * Hit FX, ported from LiquidBounce's {@code ModuleHitFX} (GPL-3.0, CCBlueX).
 * When the server confirms a hit on the entity you last attacked (by sending a vanilla hit sound near it),
 * spawns particles on that entity and optionally swaps the hit sound for a custom one. Venti adds the same
 * effect when you get hit, and lets the custom sound play up to 500% loud.
 */
@Environment(EnvType.CLIENT)
public final class HitFxModule extends Module {
   /** Vanilla sounds the server sends for a landed hit. */
   private static final Set<SoundEvent> VANILLA_HIT_SOUNDS = Set.of(
      SoundEvents.ARROW_HIT_PLAYER,
      SoundEvents.PLAYER_ATTACK_NODAMAGE,
      SoundEvents.PLAYER_ATTACK_KNOCKBACK,
      SoundEvents.PLAYER_ATTACK_CRIT,
      SoundEvents.PLAYER_ATTACK_STRONG,
      SoundEvents.PLAYER_ATTACK_SWEEP,
      SoundEvents.PLAYER_ATTACK_WEAK,
      SoundEvents.SPEAR_HIT.value(),
      SoundEvents.PLAYER_HURT
   );

   public enum Particle {
      BLOOD("Blood"),
      FIRE("Fire"),
      HEART("Heart"),
      WATER("Water"),
      SMOKE("Smoke"),
      MAGIC("Magic"),
      CRITS("Crits");

      public final String tag;

      Particle(String tag) {
         this.tag = tag;
      }
   }

   private static final String YOUR_HITS = "Your Hits";
   private static final String GETTING_HIT = "Getting Hit";
   private static final String BOTH = "Both";
   private static boolean hooked;
   private final ModeSetting triggerOn = this.add("trigger-on", new ModeSetting("Trigger On", BOTH, YOUR_HITS, GETTING_HIT, BOTH));
   private final NumberSetting soundVolume = this.add("sound-volume", new NumberSetting("Sound Volume %", 100.0F, 0.0F, 500.0F).step(5.0F));
   private final BooleanSetting ignoreGameVolume = this.add("ignore-game-volume", new BooleanSetting("Ignore Game Volume", false));
   private final Map<Particle, BooleanSetting> particles = new EnumMap<>(Particle.class);
   private final NumberSetting particleAmountMin;
   private final NumberSetting particleAmountMax;
   private final Map<HitFxSound, BooleanSetting> otherSound = new EnumMap<>(HitFxSound.class);
   private Integer lastTargetId;

   public HitFxModule() {
      super("hitfx", "Hit FX", "Particles and custom sounds when your hits land or when you get hit.", ModuleCategory.RENDER);

      for (Particle particle : Particle.values()) {
         this.particles.put(particle, this.add("particle-" + particle.name().toLowerCase(), new BooleanSetting("Particle: " + particle.tag, particle == Particle.FIRE)));
      }

      this.particleAmountMin = this.add("particle-amount-min", new NumberSetting("Particle Amount Min", 1.0F, 1.0F, 20.0F).step(1.0F));
      this.particleAmountMax = this.add("particle-amount-max", new NumberSetting("Particle Amount Max", 1.0F, 1.0F, 20.0F).step(1.0F));

      for (HitFxSound sound : HitFxSound.values()) {
         this.otherSound.put(sound, this.add("other-sound-" + sound.key(), new BooleanSetting("Other Sound: " + sound.tag, sound == HitFxSound.POP)));
      }

      init();
   }

   private static void init() {
      if (hooked) {
         return;
      }

      hooked = true;
      AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
         if (world.isClientSide() && player == Minecraft.getInstance().player) {
            HitFxModule module = Modules.get(HitFxModule.class);
            if (module != null && module.isEnabled()) {
               module.onAttack(entity);
            }
         }

         return InteractionResult.PASS;
      });
   }

   @Override
   protected void onDisable() {
      this.lastTargetId = null;
   }

   private void onAttack(Entity target) {
      if (target instanceof LivingEntity living && living.isAlive()) {
         this.lastTargetId = living.getId();
      }
   }

   /** Called on the client thread for every server sound; returns true to drop the original sound. */
   public static boolean onServerSound(ClientboundSoundPacket packet) {
      HitFxModule module = Modules.get(HitFxModule.class);
      return module != null && module.isEnabled() && module.handleSound(packet);
   }

   private boolean handleSound(ClientboundSoundPacket packet) {
      if (this.triggerOn.is(GETTING_HIT)) {
         return false;
      }

      SoundSource category = packet.getSource();
      if (category != SoundSource.PLAYERS && category != SoundSource.NEUTRAL && category != SoundSource.HOSTILE) {
         return false;
      }

      if (!VANILLA_HIT_SOUNDS.contains(packet.getSound().value()) || this.lastTargetId == null) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      ClientLevel world = mc.level;
      if (world == null || !(world.getEntity(this.lastTargetId) instanceof LivingEntity target)) {
         return false;
      }

      if (!target.isAlive() || target.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) > 8.0) {
         return false;
      }

      this.playEffect(mc, world, target);
      this.lastTargetId = null;
      SoundEvent replacement = pick(this.otherSound);
      if (replacement == null) {
         return false;
      }

      this.playLoud(mc, replacement, packet.getSource(), packet.getVolume(), packet.getPitch(), packet.getSeed(), packet.getX(), packet.getY(), packet.getZ());
      return true;
   }

   /** Called on the client thread for every damage packet; reacts when the damaged entity is you. */
   public static void onEntityDamage(ClientboundDamageEventPacket packet) {
      HitFxModule module = Modules.get(HitFxModule.class);
      if (module != null && module.isEnabled() && !module.triggerOn.is(YOUR_HITS)) {
         module.handleSelfHit(packet);
      }
   }

   private void handleSelfHit(ClientboundDamageEventPacket packet) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel world = mc.level;
      if (world == null || mc.player == null || packet.entityId() != mc.player.getId() || !mc.player.isAlive()) {
         return;
      }

      this.playEffect(mc, world, mc.player);
      SoundEvent sound = pick(this.otherSound);
      if (sound != null) {
         this.playLoud(mc, sound, SoundSource.PLAYERS, 1.0F, 1.0F, RandomSource.create().nextLong(), mc.player.getX(), mc.player.getY(), mc.player.getZ());
      }
   }

   /**
    * Plays the sound at Sound Volume %. One sound can't be louder than 100%, so above that the same sound is
    * started several times at once with the same seed and pitch; identical copies add up, 5 copies = 500%.
    */
   private void playLoud(Minecraft mc, SoundEvent sound, SoundSource category, float volume, float pitch, long seed, double x, double y, double z) {
      float total = volume * this.soundVolume.get() / 100.0F;
      if (total > 0.0F) {
         int copies = Math.max(1, (int)Math.ceil(total));
         for (int i = 0; i < copies; i++) {
            mc.getSoundManager().play(new LoudSoundInstance(sound, category, total / copies, pitch, RandomSource.create(seed), x, y, z, this.ignoreGameVolume.get()));
         }
      }
   }

   private void playEffect(Minecraft mc, ClientLevel world, LivingEntity target) {
      List<Particle> enabled = new ArrayList<>();
      this.particles.forEach((particle, setting) -> {
         if (setting.get()) {
            enabled.add(particle);
         }
      });
      if (enabled.isEmpty()) {
         return;
      }

      int a = Math.round(this.particleAmountMin.get());
      int b = Math.round(this.particleAmountMax.get());
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int amount = random.nextInt(Math.min(a, b), Math.max(a, b) + 1);

      for (int i = 0; i < amount; i++) {
         Particle particle = enabled.get(random.nextInt(enabled.size()));
         if (particle == Particle.BLOOD) {
            world.addDestroyBlockEffect(target.blockPosition().above(1), Blocks.REDSTONE_BLOCK.defaultBlockState());
         } else {
            mc.particleEngine.createTrackingEmitter(target, emitterParticle(particle));
         }
      }
   }

   private static ParticleOptions emitterParticle(Particle particle) {
      return switch (particle) {
         case FIRE -> ParticleTypes.LAVA;
         case HEART -> ParticleTypes.HEART;
         case WATER -> ParticleTypes.FALLING_WATER;
         case SMOKE -> ParticleTypes.SMOKE;
         case MAGIC -> ParticleTypes.ENCHANTED_HIT;
         case CRITS, BLOOD -> ParticleTypes.CRIT;
      };
   }

   private static SoundEvent pick(Map<HitFxSound, BooleanSetting> choices) {
      List<HitFxSound> enabled = new ArrayList<>();
      choices.forEach((sound, setting) -> {
         if (setting.get()) {
            enabled.add(sound);
         }
      });
      return enabled.isEmpty() ? null : enabled.get(ThreadLocalRandom.current().nextInt(enabled.size())).random();
   }
}
