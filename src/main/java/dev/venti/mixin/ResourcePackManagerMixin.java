package dev.venti.mixin;

import dev.venti.modules.totem.CustomTotemModule;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds Venti's generated Custom Totem pack to the client's resource packs (never to data packs). */
@Mixin(PackRepository.class)
public abstract class ResourcePackManagerMixin {
   @Inject(method = "discoverAvailable", at = @At("RETURN"), cancellable = true)
   private void venti$addTotemPack(CallbackInfoReturnable<Map<String, Pack>> cir) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getResourcePackRepository() == (Object)this) {
         Pack totem = CustomTotemModule.profile();
         if (totem != null) {
            Map<String, Pack> profiles = new LinkedHashMap<>(cir.getReturnValue());
            profiles.put(totem.getId(), totem);
            cir.setReturnValue(profiles);
         }
      }
   }
}
