package dev.venti.mixin.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({ClientLevel.class})
public interface AccessorAuroraSkyClientLevel {
   @Accessor("skyFlashTime")
   int venti$atmosphereSkyFlash();
}
