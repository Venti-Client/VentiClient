package dev.venti.modules.character;

import dev.venti.modules.character.kit.CharacterModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class WornCharacter {
   final CharacterModel model;
   final RenderType solid;
   final RenderType decal;
   final RenderType ghost;

   WornCharacter(CharacterModel model, Identifier texture) {
      this.model = model;
      this.solid = RenderTypes.entitySolid(texture);
      this.decal = RenderTypes.entityTranslucent(texture);
      //? if >=26.3 {
      /*// 26.3 has no separate item entity target.
      this.ghost = RenderTypes.entityTranslucentCull(texture);
      *///?} elif >=26.2 {
      this.ghost = RenderTypes.entityTranslucentCullItemTarget(texture);
      //?} else {
      /*this.ghost = RenderTypes.itemEntityTranslucentCull(texture);
      *///?}
   }
}
