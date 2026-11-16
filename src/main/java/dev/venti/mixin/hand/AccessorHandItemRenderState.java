package dev.venti.mixin.hand;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface AccessorHandItemRenderState {
   @Accessor("layers")
   ItemRenderState.LayerRenderState[] venti$handLayers();

   @Accessor("layerCount")
   int venti$handLayerCount();
}
