package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.module.Modules;
import dev.venti.modules.view.SkinChangeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Skin Change: your first-person arm wears the chosen skin too. */
@Mixin(ItemInHandRenderer.class)
public abstract class ViewHeldItemRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;

   @WrapOperation(
      method = {
         "renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V",
         "renderMapHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/world/entity/HumanoidArm;)V"
      },
      at = {
         @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
         ),
         @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
         )
      }
   )
   private void venti$skinChangeArm(
      AvatarRenderer<?> renderer,
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      Identifier texture,
      boolean sleeve,
      Operation<Void> original
   ) {
      SkinChangeModule module = Modules.get(SkinChangeModule.class);
      LocalPlayer player = this.minecraft.player;
      if (module != null && module.isEnabled() && player != null) {
         PlayerSkin own = player.getSkin();
         PlayerSkin skin = module.skinFor(player.getId(), own);
         if (skin != own) {
            texture = skin.body().texturePath();
            AvatarRenderer<AbstractClientPlayer> byModel = ((ViewEntityRenderManagerAccessor)this.entityRenderDispatcher).venti$playerRenderers().get(skin.model());
            if (byModel != null) {
               renderer = byModel;
            }
         }
      }

      original.call(renderer, matrices, queue, light, texture, sleeve);
   }
}
