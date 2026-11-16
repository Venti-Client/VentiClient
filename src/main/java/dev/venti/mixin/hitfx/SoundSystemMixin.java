package dev.venti.mixin.hitfx;

import dev.venti.modules.hitfx.LoudSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hit FX's "Ignore Game Volume": play at the sound's own volume instead of scaling by the game's sliders. */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
   @Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
   private void venti$ignoreGameVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      if (sound instanceof LoudSoundInstance loud && loud.ignoreGameVolume()) {
         cir.setReturnValue(Math.max(0.0F, Math.min(1.0F, loud.getVolume())));
      }
   }
}
