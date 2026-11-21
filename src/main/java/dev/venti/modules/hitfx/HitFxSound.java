package dev.venti.modules.hitfx;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Hit sound packs, ported from LiquidBounce's {@code HitFXRegistry} (GPL-3.0, CCBlueX).
 * Custom sounds live in {@code assets/venti/sounds/hitfx/} and are declared in {@code assets/venti/sounds.json}.
 * They are not put into the sound event registry: a client-only {@link SoundEvent} is enough to play them locally.
 */
public enum HitFxSound {
   HIT("Hit", SoundEvents.ARROW_HIT),
   ORB("Orb", SoundEvents.EXPERIENCE_ORB_PICKUP),
   BONK("Bonk", "bonk"),
   BOYKISSER("Boykisser", "boykisser-1", "boykisser-2", "boykisser-3", "boykisser-4", "boykisser-5", "boykisser-6"),
   BRING("Bring", "bring"),
   GLASS("Glass", "glass-1", "glass-2", "glass-3"),
   CLICK("Click", "click-1", "click-2", "click-3"),
   MEOW("Meow", "meow"),
   MOAN("Moan", "moan-1", "moan-2", "moan-3", "moan-4"),
   MAGIC_SQUASH("MagicSquash", "magic_squash"),
   NYA("NYA", "nya"),
   POP("Pop", "pop"),
   SOFT("Soft", "soft"),
   SQUASH("Squash", "squash"),
   TUNG("Tung", "tung"),
   UWU("UWU", "uwu");

   public final String tag;
   private final SoundEvent[] sounds;

   HitFxSound(String tag, SoundEvent vanilla) {
      this.tag = tag;
      this.sounds = new SoundEvent[]{vanilla};
   }

   HitFxSound(String tag, String... customIds) {
      this.tag = tag;
      this.sounds = new SoundEvent[customIds.length];

      for (int i = 0; i < customIds.length; i++) {
         this.sounds[i] = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("venti", "hitfx." + customIds[i]));
      }
   }

   public SoundEvent random() {
      return this.sounds[ThreadLocalRandom.current().nextInt(this.sounds.length)];
   }

   /** Lower-case key used for the setting id, e.g. {@code magic-squash}. */
   public String key() {
      return this.name().toLowerCase().replace('_', '-');
   }
}
