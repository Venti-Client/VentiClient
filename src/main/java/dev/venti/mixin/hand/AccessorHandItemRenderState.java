package dev.venti.mixin.hand;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface AccessorHandItemRenderState {
   @Accessor("layers")
   ItemStackRenderState.LayerRenderState[] venti$handLayers();

   @Accessor("activeLayerCount")
   int venti$handLayerCount();
}
