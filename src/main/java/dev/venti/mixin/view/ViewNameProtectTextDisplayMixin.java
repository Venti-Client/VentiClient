package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Name Protect: text display entities (holograms); re-splits cached lines whenever the alias table changes. */
@Mixin(DisplayEntity.TextDisplayEntity.class)
public abstract class ViewNameProtectTextDisplayMixin {
   @Unique
   private int venti$nameProtectGeneration;

   @ModifyExpressionValue(
      method = "splitLines(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$LineSplitter;)Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;",
      at = @At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;textLines:Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;"
      )
   )
   private DisplayEntity.TextDisplayEntity.TextLines venti$refresh(DisplayEntity.TextDisplayEntity.TextLines cached) {
      int generation = NameProtectModule.generation();
      if (generation == this.venti$nameProtectGeneration) {
         return cached;
      } else {
         this.venti$nameProtectGeneration = generation;
         return null;
      }
   }

   @ModifyExpressionValue(
      method = "splitLines(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$LineSplitter;)Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$Data;text()Lnet/minecraft/text/Text;"
      )
   )
   private Text venti$protectText(Text text) {
      return NameProtectModule.protect(text);
   }
}
