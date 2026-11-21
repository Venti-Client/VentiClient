package dev.venti.mixin.world;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface AccessorWorldGameRenderer {
   @Invoker("shouldRenderBlockOutline")
   boolean venti$shouldRenderBlockOutline();
}
