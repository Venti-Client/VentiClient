package dev.venti.mixin.hitfx;

import dev.venti.modules.hitfx.LoudSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hit FX's "Ignore Game Volume": play at the sound's own volume instead of scaling by the game's sliders. */
@Mixin(SoundEngine.class)
public abstract class SoundSystemMixin {
   @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
   private void venti$ignoreGameVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      if (sound instanceof LoudSoundInstance loud && loud.ignoreGameVolume()) {
         cir.setReturnValue(Math.max(0.0F, Math.min(1.0F, loud.getVolume())));
      }
   }
}
