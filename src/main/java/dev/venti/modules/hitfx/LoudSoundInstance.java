package dev.venti.modules.hitfx;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

/**
 * A Hit FX sound. With {@code ignoreGameVolume} its loudness skips the Master and category sliders
 * (SoundSystemMixin), so it can be louder than Minecraft's own volume allows.
 */
public final class LoudSoundInstance extends PositionedSoundInstance {
   private final boolean ignoreGameVolume;

   public LoudSoundInstance(SoundEvent sound, SoundCategory category, float volume, float pitch, Random random, double x, double y, double z, boolean ignoreGameVolume) {
      super(sound, category, volume, pitch, random, x, y, z);
      this.ignoreGameVolume = ignoreGameVolume;
   }

   public boolean ignoreGameVolume() {
      return this.ignoreGameVolume;
   }
}
