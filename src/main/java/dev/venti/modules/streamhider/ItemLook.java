package dev.venti.modules.streamhider;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ItemLook extends ItemStackRenderState {
   public final List<Object> identity = new ArrayList<>();
   private final List<LayerRenderState> layers = new ArrayList<>();

   public void clear() {
      super.clear();
      this.identity.clear();
      this.layers.clear();
   }

   public LayerRenderState newLayer() {
      LayerRenderState layer = super.newLayer();
      this.layers.add(layer);
      return layer;
   }

   public void appendModelIdentityElement(Object element) {
      if (!(element instanceof FoilType)) {
         this.identity.add(element);
      }
   }

   public boolean resolve(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();

      try {
         mc.getItemModelResolver().updateForTopItem(this, stack, ItemDisplayContext.GUI, mc.level, mc.player, 0);
      } catch (RuntimeException var5) {
         this.clear();
         return false;
      }

      for (LayerRenderState layer : this.layers) {
         layer.setFoilType(FoilType.NONE);
      }

      if (this.identity.isEmpty()) {
         this.identity.add(stack.getItem());
      }

      return true;
   }
}
