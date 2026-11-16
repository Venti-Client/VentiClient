package dev.venti.mixin.character;

import dev.venti.modules.character.CharacterLighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.DiffuseLighting.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({DiffuseLighting.class})
public abstract class MixinCharacterLighting {
   @Inject(
      method = {"setShaderLights"},
      at = {@At("HEAD")}
   )
   private void venti$trackCharacterLighting(Type entry, CallbackInfo ci) {
      CharacterLighting.setup(entry);
   }
}
