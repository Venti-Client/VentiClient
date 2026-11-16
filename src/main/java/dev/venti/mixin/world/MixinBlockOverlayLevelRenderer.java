package dev.venti.mixin.world;

import dev.venti.modules.world.BlockOverlayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinBlockOverlayLevelRenderer {
   @Inject(
      method = {"renderTargetBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void venti$hideVanillaOutline(CallbackInfo ci) {
      BlockOverlayModule module = dev.venti.module.Modules.get(BlockOverlayModule.class);
      if (module != null && module.isEnabled()) {
         ci.cancel();
      }
   }
}
