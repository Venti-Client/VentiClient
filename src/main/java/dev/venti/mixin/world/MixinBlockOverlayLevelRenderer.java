// 26.x renders the level through Fabric's LevelRenderEvents instead: see dev.venti.compat.LevelHooks.
//? if <26.2 {
/*package dev.venti.mixin.world;

import dev.venti.modules.world.BlockOverlayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LevelRenderer.class})
public abstract class MixinBlockOverlayLevelRenderer {
   @Inject(
      method = {"renderBlockOutline"},
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
*///?}
