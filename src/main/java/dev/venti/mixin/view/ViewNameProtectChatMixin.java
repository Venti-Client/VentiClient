package dev.venti.mixin.view;

import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Name Protect: chat lines, rewritten when they are wrapped for display (the stored message is untouched). */
@Mixin(GuiMessage.class)
public abstract class ViewNameProtectChatMixin {
   @ModifyArg(
      method = "splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"
      ),
      index = 0
   )
   private FormattedText venti$protectChatLine(FormattedText text) {
      return NameProtectModule.protectChatLine(text);
   }
}
