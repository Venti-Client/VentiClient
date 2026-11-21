package dev.venti.mixin.world;

import dev.venti.modules.world.AuroraSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({CloudRenderer.class})
public abstract class MixinAuroraSkyCloudRenderer {
   @ModifyVariable(
      method = {"render"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private int venti$cloudColour(int color) {
      AuroraSkyModule atmosphere = AuroraSkyModule.get();
      return atmosphere != null ? atmosphere.clouds(color) : color;
   }
}
