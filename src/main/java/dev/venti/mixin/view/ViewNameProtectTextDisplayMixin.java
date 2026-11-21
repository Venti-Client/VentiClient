package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Name Protect: text display entities (holograms); re-splits cached lines whenever the alias table changes. */
@Mixin(Display.TextDisplay.class)
public abstract class ViewNameProtectTextDisplayMixin {
   @Unique
   private int venti$nameProtectGeneration;

   @ModifyExpressionValue(
      method = "cacheDisplay(Lnet/minecraft/world/entity/Display$TextDisplay$LineSplitter;)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;",
      at = @At(
         value = "FIELD",
         opcode = 180,
         ordinal = 0,
         target = "Lnet/minecraft/world/entity/Display$TextDisplay;clientDisplayCache:Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;"
      )
   )
   private Display.TextDisplay.CachedInfo venti$refresh(Display.TextDisplay.CachedInfo cached) {
      int generation = NameProtectModule.generation();
      if (generation == this.venti$nameProtectGeneration) {
         return cached;
      } else {
         this.venti$nameProtectGeneration = generation;
         return null;
      }
   }

   @ModifyExpressionValue(
      method = "cacheDisplay(Lnet/minecraft/world/entity/Display$TextDisplay$LineSplitter;)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Display$TextDisplay$TextRenderState;text()Lnet/minecraft/network/chat/Component;"
      )
   )
   private Component venti$protectText(Component text) {
      return NameProtectModule.protect(text);
   }
}
