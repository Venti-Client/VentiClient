package dev.venti.modules.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface CharacterHolder {
   WornCharacter venti$character();

   void venti$setCharacter(WornCharacter var1);

   float venti$seat();

   void venti$setSeat(float var1);
}
