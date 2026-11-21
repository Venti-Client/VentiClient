package dev.venti.modules.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.venti.config.BooleanSetting;
import dev.venti.config.ModeSetting;
import dev.venti.mixin.hand.AccessorHandPlayerModel;
import dev.venti.module.Module;
import dev.venti.module.ModuleCategory;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
      return stack.is(ItemTags.SWORDS)
         || stack.is(ItemTags.AXES)
         || stack.is(ItemTags.PICKAXES)
         || stack.is(ItemTags.SHOVELS)
         || stack.is(ItemTags.HOES)
         || stack.is(ItemTags.SPEARS)
         || stack.is(Items.MACE)
         || stack.is(Items.FISHING_ROD)
         || stack.is(Items.SHEARS)
         || stack.is(Items.FLINT_AND_STEEL)
         || stack.is(Items.BRUSH)
         || stack.is(Items.CARROT_ON_A_STICK)
         || stack.is(Items.WARPED_FUNGUS_ON_A_STICK);
   }

   public static void shoulder(PoseStack pose, boolean right, AvatarRenderer<?> renderer) {
      FirstPersonArm.shoulder(pose, right, ((AccessorHandPlayerModel)renderer.getModel()).venti$handSlim());
   }

   public static void renderArm(
      PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand
   ) {
      if (covers(stack, mainHand)) {
         FirstPersonArm.Arm arm = FirstPersonArm.arm();
         if (arm != null) {
            FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, 0.0F, 0.0F, 0.0F);
         }
      }
   }
}
