package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.module.Modules;
import dev.venti.modules.view.SkinChangeModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Skin Change: your first-person arm wears the chosen skin too. */
@Mixin(HeldItemRenderer.class)
public abstract class ViewHeldItemRendererMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private EntityRenderManager entityRenderDispatcher;

   @WrapOperation(
      method = {
         "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V",
         "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V"
      },
      at = {
         @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
         ),
         @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
         )
      }
   )
   private void venti$skinChangeArm(
      PlayerEntityRenderer<?> renderer,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      int light,
      Identifier texture,
      boolean sleeve,
      Operation<Void> original
   ) {
      SkinChangeModule module = Modules.get(SkinChangeModule.class);
      ClientPlayerEntity player = this.client.player;
      if (module != null && module.isEnabled() && player != null) {
         SkinTextures own = player.getSkin();
         SkinTextures skin = module.skinFor(player.getId(), own);
         if (skin != own) {
            texture = skin.body().texturePath();
            PlayerEntityRenderer<AbstractClientPlayerEntity> byModel = ((ViewEntityRenderManagerAccessor)this.entityRenderDispatcher).venti$playerRenderers().get(skin.model());
            if (byModel != null) {
               renderer = byModel;
            }
         }
      }

      original.call(renderer, matrices, queue, light, texture, sleeve);
   }
}
