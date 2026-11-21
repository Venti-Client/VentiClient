//? if >=26.2 {
package dev.venti.mixin.view;

import dev.venti.modules.view.NoRenderModule;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No Render: hide rain splashes and rain sounds (26.x only; earlier versions tick them in WeatherEffectRenderer,
// see ViewNoRenderWeatherMixin). Listed for 26.x by VentiMixinPlugin.
@Mixin(ClientLevel.class)
public abstract class ViewNoRenderRainTickMixin {
   @Inject(method = "tickWeatherEffects()V", at = @At("HEAD"), cancellable = true)
   private void venti$hideRainSplashes(CallbackInfo ci) {
      if (NoRenderModule.hideWeather()) {
         ci.cancel();
      }
   }
}
//?}
