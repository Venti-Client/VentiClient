package dev.venti.compat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Adds the mixins that only exist for this jar's Minecraft version to each Venti mixin config.
 * Mixins every version shares are listed in the JSON configs; a mixin whose target class only exists
 * in some versions is compiled only for those versions and listed here.
 */
public final class VentiMixinPlugin implements IMixinConfigPlugin {
   //? if >=26.2 {
   private static final Map<String, List<String>> VERSION_MIXINS = Map.of(
      "dev.venti.mixin.view", List.of("ViewNoRenderRainTickMixin"),
      "dev.venti.mixin.hand", List.of("MixinHandAuraGameRenderer")
   );
   //?} else {
   /*private static final Map<String, List<String>> VERSION_MIXINS = Map.of(
      "dev.venti.mixin.waypoints", List.of("MixinWaypointsWorldRenderer"),
      "dev.venti.mixin.world", List.of("MixinBlockOverlayLevelRenderer", "MixinJumpCirclesLevelRenderer", "MixinWorldVisualsRenderer")
   );
   *///?}
   private String mixinPackage = "";

   @Override
   public void onLoad(String mixinPackage) {
      this.mixinPackage = mixinPackage;
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return true;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return VERSION_MIXINS.getOrDefault(this.mixinPackage, List.of());
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
