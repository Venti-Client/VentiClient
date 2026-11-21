package dev.venti.mixin.view;

import dev.venti.modules.view.NametagState;
import dev.venti.modules.view.NametagsModule;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the Nametags module's per-frame data on the player's render state. */
@Mixin(AvatarRenderState.class)
public abstract class ViewPlayerRenderStateMixin implements NametagState {
   @Unique
   private NametagsModule.Tag venti$nametag;

   @Override
   public NametagsModule.Tag venti$nametag() {
      return this.venti$nametag;
   }

   @Override
   public void venti$setNametag(NametagsModule.Tag tag) {
      this.venti$nametag = tag;
   }
}
