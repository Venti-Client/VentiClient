package dev.venti.mixin.view;

import dev.venti.modules.view.NameProtectModule;
//? if >=1.21.11 {
import net.minecraft.client.gui.hud.ChatHudLine;
//?} else {
/*import net.minecraft.client.gui.hud.ChatHud;
*///?}
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Name Protect: chat lines, rewritten when they are wrapped for display (the stored message is untouched). */
//? if >=1.21.11 {
@Mixin(ChatHudLine.class)
//?} else {
/*@Mixin(ChatHud.class) // lines are wrapped in ChatHud.addVisibleMessage before 1.21.11
*///?}
public abstract class ViewNameProtectChatMixin {
   @ModifyArg(
      //? if >=1.21.11 {
      method = "breakLines(Lnet/minecraft/client/font/TextRenderer;I)Ljava/util/List;",
      //?} else {
      /*method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
      *///?}
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/ChatMessages;breakRenderedChatMessageLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/client/font/TextRenderer;)Ljava/util/List;"
      ),
      index = 0
   )
   private StringVisitable venti$protectChatLine(StringVisitable text) {
      return NameProtectModule.protectChatLine(text);
   }
}
