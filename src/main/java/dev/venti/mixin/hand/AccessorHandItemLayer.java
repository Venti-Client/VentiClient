package dev.venti.mixin.hand;

import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface AccessorHandItemLayer {
   @Accessor("transform")
   ItemTransform venti$handTransform();

   @Accessor("extents")
   Supplier<Vector3fc[]> venti$handExtents();
}
