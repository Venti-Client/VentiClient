package dev.venti.mixin.hand;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntityModel.class)
public interface AccessorHandPlayerModel {
   @Accessor("thinArms")
   boolean venti$handSlim();
}
