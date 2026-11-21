package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Name Protect: tab list names, header/footer and the heads of renamed players. */
//? if >=26.2 {
@Mixin(PlayerTabOverlay.class)
public abstract class ViewNameProtectTabMixin {
   @ModifyReturnValue(method = "getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"))
   private Component venti$protectTabName(Component original) {
      return NameProtectModule.protect(original);
   }

   @ModifyArg(
      method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"),
      index = 0
   )
   private FormattedText venti$protectTabHeaderFooter(FormattedText text) {
      return NameProtectModule.protectText(text);
   }

   @WrapOperation(
      method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerInfo;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;")
   )
   private PlayerSkin venti$protectTabHead(PlayerInfo entry, Operation<PlayerSkin> original) {
      PlayerSkin real = original.call(entry);
      return NameProtectModule.hidesName(entry.getProfile().name()) ? NameProtectModule.hiddenHead(entry.getProfile().id(), real) : real;
   }
}
//?} else {
/*@Mixin(PlayerTabOverlay.class)
public abstract class ViewNameProtectTabMixin {
   @ModifyReturnValue(method = "getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"))
   private Component venti$protectTabName(Component original) {
      return NameProtectModule.protect(original);
   }

   @ModifyArg(
      method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"),
      index = 0
   )
   private FormattedText venti$protectTabHeaderFooter(FormattedText text) {
      return NameProtectModule.protectText(text);
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerInfo;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;")
   )
   private PlayerSkin venti$protectTabHead(PlayerInfo entry, Operation<PlayerSkin> original) {
      PlayerSkin real = original.call(entry);
      return NameProtectModule.hidesName(entry.getProfile().name()) ? NameProtectModule.hiddenHead(entry.getProfile().id(), real) : real;
   }
}
*///?}
