package dev.venti.modules.hitfx;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A Hit FX sound. With {@code ignoreGameVolume} its loudness skips the Master and category sliders
 * (SoundSystemMixin), so it can be louder than Minecraft's own volume allows.
 */
public final class LoudSoundInstance extends SimpleSoundInstance {
   private final boolean ignoreGameVolume;

   public LoudSoundInstance(SoundEvent sound, SoundSource category, float volume, float pitch, RandomSource random, double x, double y, double z, boolean ignoreGameVolume) {
      super(sound, category, volume, pitch, random, x, y, z);
      this.ignoreGameVolume = ignoreGameVolume;
   }

   public boolean ignoreGameVolume() {
      return this.ignoreGameVolume;
   }
}
