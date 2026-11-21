package dev.venti.mixin.view;

import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Name Protect: chat lines, rewritten when they are wrapped for display (the stored message is untouched). */
@Mixin(ChatHudLine.class)
public abstract class ViewNameProtectChatMixin {
   @ModifyArg(
      method = "breakLines(Lnet/minecraft/client/font/TextRenderer;I)Ljava/util/List;",
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
