package dev.venti.mixin.hand;

import java.util.function.Supplier;
//? if >=26.2 {
import net.minecraft.client.resources.model.cuboid.ItemTransform;
//?} else {
/*import net.minecraft.client.renderer.block.model.ItemTransform;
*///?}
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface AccessorHandItemLayer {
   //? if >=26.2 {
   @Accessor("itemTransform")
   //?} else {
   /*@Accessor("transform")
   *///?}
   ItemTransform venti$handTransform();

   @Accessor("extents")
   Supplier<Vector3fc[]> venti$handExtents();
}
