package dev.venti.modules.view;

/** Extra nametag data carried on a player's render state (implemented by a mixin on PlayerEntityRenderState). */
public interface NametagState {
   NametagsModule.Tag venti$nametag();

   void venti$setNametag(NametagsModule.Tag tag);
}
