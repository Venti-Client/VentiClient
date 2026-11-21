package dev.venti.mixin.character;

import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.platform.Lighting;
import dev.venti.modules.character.CharacterLighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Lighting.class})
public abstract class MixinCharacterLighting {
   @Inject(
      method = {"setupFor"},
      at = {@At("HEAD")}
   )
   private void venti$trackCharacterLighting(Entry entry, CallbackInfo ci) {
      CharacterLighting.setup(entry);
   }
}
