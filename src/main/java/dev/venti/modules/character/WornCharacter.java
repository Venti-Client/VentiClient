package dev.venti.modules.character;

import dev.venti.modules.character.kit.CharacterModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if >=1.21.11
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class WornCharacter {
   final CharacterModel model;
   final RenderLayer solid;
   final RenderLayer decal;
   final RenderLayer ghost;

   WornCharacter(CharacterModel model, Identifier texture) {
      this.model = model;
      //? if >=1.21.11 {
      this.solid = RenderLayers.entitySolid(texture);
      this.decal = RenderLayers.entityTranslucent(texture);
      this.ghost = RenderLayers.itemEntityTranslucentCull(texture);
      //?} else {
      /*this.solid = RenderLayer.getEntitySolid(texture);
      this.decal = RenderLayer.getEntityTranslucent(texture);
      this.ghost = RenderLayer.getItemEntityTranslucentCull(texture);
      *///?}
   }
}
