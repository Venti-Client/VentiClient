package dev.venti.mixin.view;

import java.util.Map;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//? if >=1.21.9 {
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerSkinType;
//?} else {
/*import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Skin Change: pick the wide/slim player renderer that matches the worn skin for the first-person arm. */
//? if >=1.21.9 {
@Mixin(EntityRenderManager.class)
public interface ViewEntityRenderManagerAccessor {
   @Accessor("playerRenderers")
   Map<PlayerSkinType, PlayerEntityRenderer<AbstractClientPlayerEntity>> venti$playerRenderers();
//?} else {
/*@Mixin(EntityRenderDispatcher.class)
public interface ViewEntityRenderManagerAccessor {
   @Accessor("modelRenderers")
   Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity, ?>> venti$playerRenderers();
*///?}
}
