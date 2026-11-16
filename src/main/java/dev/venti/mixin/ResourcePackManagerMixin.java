package dev.venti.mixin;

import dev.venti.modules.totem.CustomTotemModule;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds Venti's generated Custom Totem pack to the client's resource packs (never to data packs). */
@Mixin(ResourcePackManager.class)
public abstract class ResourcePackManagerMixin {
   @Inject(method = "providePackProfiles", at = @At("RETURN"), cancellable = true)
   private void venti$addTotemPack(CallbackInfoReturnable<Map<String, ResourcePackProfile>> cir) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.getResourcePackManager() == (Object)this) {
         ResourcePackProfile totem = CustomTotemModule.profile();
         if (totem != null) {
            Map<String, ResourcePackProfile> profiles = new LinkedHashMap<>(cir.getReturnValue());
            profiles.put(totem.getId(), totem);
            cir.setReturnValue(profiles);
         }
      }
   }
}
