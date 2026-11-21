package dev.venti.modules.view;

import com.mojang.blaze3d.platform.InputConstants;
import dev.venti.compat.Fapi;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Key bindings for the view modules, listed under the Venti category in Controls. */
public final class ViewKeys {
   /**
    * VentiClient already registered this category via {@code KeyBinding.Category.create} (which throws on a second call),
    * so the modules build an equal record instead of registering it again.
    */
   private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("venti", "keys"));

   private ViewKeys() {
   }

   public static KeyMapping register(String translationKey, int glfwKey) {
      return Fapi.registerKey(new KeyMapping(translationKey, InputConstants.Type.KEYSYM, glfwKey, CATEGORY));
   }
}
