package dev.venti.mixin.hand;

import java.util.function.Supplier;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface AccessorHandItemLayer {
   @Accessor("transform")
   Transformation venti$handTransform();

   @Accessor("vertices")
   Supplier<Vector3fc[]> venti$handExtents();
}
