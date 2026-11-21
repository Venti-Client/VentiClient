package dev.venti.mixin.hand;

import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerModel.class)
public interface AccessorHandPlayerModel {
   @Accessor("slim")
   boolean venti$handSlim();
}
