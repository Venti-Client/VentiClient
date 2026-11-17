package dev.venti.mixin.streamhider;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Item icons are drawn off screen with fog turned off. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
   @Accessor("fogRenderer")
   FogRenderer venti$noInvLeakFogRenderer();
}
