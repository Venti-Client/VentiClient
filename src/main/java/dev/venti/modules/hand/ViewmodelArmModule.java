package dev.venti.modules.hand;

import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.mixin.hand.AccessorHandPlayerModel;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
*///?}
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

/** 3D Hand: draws your own arm holding the item in first person (ported from Anubis by 4ldenz). */
public final class ViewmodelArmModule extends Module {
   public static volatile ViewmodelArmModule active;
   private static final String ALL_ITEMS = "All Items";
   private static final String WEAPONS_TOOLS = "Weapons & Tools";
   private final ModeSetting items = this.add("items", new ModeSetting("Items", ALL_ITEMS, ALL_ITEMS, WEAPONS_TOOLS));
   private final BooleanSetting mainHand = this.add("main-hand", new BooleanSetting("Main Hand", true));
   private final BooleanSetting offHand = this.add("off-hand", new BooleanSetting("Off Hand", true));

   public ViewmodelArmModule() {
      super("viewmodel-arm", "Viewmodel Arm", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", ModuleCategory.VISUALS);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   static boolean wants(boolean mainHand) {
      ViewmodelArmModule module = active;
      return module != null && (mainHand ? module.mainHand.get() : module.offHand.get());
   }

   static boolean covers(ItemStack stack, boolean mainHand) {
      ViewmodelArmModule module = active;
      return wants(mainHand) && module != null && (module.items.is(ALL_ITEMS) || weaponOrTool(stack));
   }

   private static boolean weaponOrTool(ItemStack stack) {
      return stack.isIn(ItemTags.SWORDS)
         || stack.isIn(ItemTags.AXES)
         || stack.isIn(ItemTags.PICKAXES)
         || stack.isIn(ItemTags.SHOVELS)
         || stack.isIn(ItemTags.HOES)
         //? if >=1.21.11
         || stack.isIn(ItemTags.SPEARS)
         || stack.isOf(Items.MACE)
         || stack.isOf(Items.FISHING_ROD)
         || stack.isOf(Items.SHEARS)
         || stack.isOf(Items.FLINT_AND_STEEL)
         || stack.isOf(Items.BRUSH)
         || stack.isOf(Items.CARROT_ON_A_STICK)
         || stack.isOf(Items.WARPED_FUNGUS_ON_A_STICK);
   }

   //? if >=1.21.9 {
   public static void shoulder(MatrixStack pose, boolean right, PlayerEntityRenderer<?> renderer) {
   //?} else {
   /*public static void shoulder(MatrixStack pose, boolean right, PlayerEntityRenderer renderer) {
   *///?}
      FirstPersonArm.shoulder(pose, right, ((AccessorHandPlayerModel)renderer.getModel()).venti$handSlim());
   }

   public static void renderArm(
      //? if >=1.21.9 {
      MatrixStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, OrderedRenderCommandQueue collector, int light, boolean mainHand
      //?} else {
      /*MatrixStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, VertexConsumerProvider collector, int light, boolean mainHand
      *///?}
   ) {
      if (covers(stack, mainHand)) {
         FirstPersonArm.Arm arm = FirstPersonArm.arm();
         if (arm != null) {
            FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, 0.0F, 0.0F, 0.0F);
         }
      }
   }
}
