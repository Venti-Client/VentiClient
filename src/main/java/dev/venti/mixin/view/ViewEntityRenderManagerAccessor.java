package dev.venti.mixin.view;

import java.util.Map;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerSkinType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Skin Change: pick the wide/slim player renderer that matches the worn skin for the first-person arm. */
@Mixin(EntityRenderManager.class)
public interface ViewEntityRenderManagerAccessor {
   @Accessor("playerRenderers")
   Map<PlayerSkinType, PlayerEntityRenderer<AbstractClientPlayerEntity>> venti$playerRenderers();
}
