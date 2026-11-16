package dev.venti.mixin.view;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.venti.modules.view.NameProtectModule;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Name Protect: boss bar titles. */
@Mixin(BossBarHud.class)
public abstract class ViewNameProtectBossBarMixin {
   @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ClientBossBar;getName()Lnet/minecraft/text/Text;")
   )
   private Text venti$protectBossName(Text original) {
      return NameProtectModule.protect(original);
   }
}
