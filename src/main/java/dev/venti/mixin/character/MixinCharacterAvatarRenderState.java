package dev.venti.mixin.character;

import dev.venti.modules.character.CharacterHolder;
import dev.venti.modules.character.WornCharacter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderState.class})
public abstract class MixinCharacterAvatarRenderState implements CharacterHolder {
   @Unique
   private WornCharacter venti$character;
   @Unique
   private float venti$seat;

   public WornCharacter venti$character() {
      return this.venti$character;
   }

   public void venti$setCharacter(WornCharacter character) {
      this.venti$character = character;
   }

   public float venti$seat() {
      return this.venti$seat;
   }

   public void venti$setSeat(float knee) {
      this.venti$seat = knee;
   }
}
