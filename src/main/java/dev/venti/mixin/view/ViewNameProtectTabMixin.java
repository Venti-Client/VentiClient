package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
//? if >=1.21.9 {
import net.minecraft.entity.player.SkinTextures;
//?} else {
/*import net.minecraft.client.util.SkinTextures;
*///?}
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Name Protect: tab list names, header/footer and the heads of renamed players. */
@Mixin(PlayerListHud.class)
public abstract class ViewNameProtectTabMixin {
   @ModifyReturnValue(method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;", at = @At("RETURN"))
   private Text venti$protectTabName(Text original) {
      return NameProtectModule.protect(original);
   }

   @ModifyArg(
      method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"),
      index = 0
   )
   private StringVisitable venti$protectTabHeaderFooter(StringVisitable text) {
      return NameProtectModule.protectText(text);
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      //? if >=1.21.9 {
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/PlayerListEntry;getSkinTextures()Lnet/minecraft/entity/player/SkinTextures;")
      //?} else {
      /*at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/PlayerListEntry;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;")
      *///?}
   )
   private SkinTextures venti$protectTabHead(PlayerListEntry entry, Operation<SkinTextures> original) {
      SkinTextures real = original.call(entry);
      //? if >=1.21.9 {
      return NameProtectModule.hidesName(entry.getProfile().name()) ? NameProtectModule.hiddenHead(entry.getProfile().id(), real) : real;
      //?} else {
      /*return NameProtectModule.hidesName(entry.getProfile().getName()) ? NameProtectModule.hiddenHead(entry.getProfile().getId(), real) : real;
      *///?}
   }
}
