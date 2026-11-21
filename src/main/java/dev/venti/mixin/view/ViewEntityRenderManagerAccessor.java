package dev.venti.mixin.view;

import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Skin Change: pick the wide/slim player renderer that matches the worn skin for the first-person arm. */
@Mixin(EntityRenderDispatcher.class)
public interface ViewEntityRenderManagerAccessor {
   @Accessor("playerRenderers")
   Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> venti$playerRenderers();
}
