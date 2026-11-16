package dev.venti.modules.view;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/** Key bindings for the view modules, listed under the Venti category in Controls. */
public final class ViewKeys {
   /**
    * VentiClient already registered this category via {@code KeyBinding.Category.create} (which throws on a second call),
    * so the modules build an equal record instead of registering it again.
    */
   //? if >=1.21.9 {
   private static final KeyBinding.Category CATEGORY = new KeyBinding.Category(Identifier.of("venti", "keys"));
   //?} else {
   /*private static final String CATEGORY = "key.category.venti.keys"; // categories are translation keys before 1.21.9
   *///?}

   private ViewKeys() {
   }

   public static KeyBinding register(String translationKey, int glfwKey) {
      return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, CATEGORY));
   }
}
