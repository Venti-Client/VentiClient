package dev.venti.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Every module, in GUI order. Register in {@link dev.venti.VentiClient}. */
public final class Modules {
   private static final List<Module> ALL = new ArrayList<>();
   private static boolean[] bindDown = new boolean[0];

   private Modules() {
   }

   public static <M extends Module> M register(M module) {
      ALL.add(module);
      module.settings().load();
      bindDown = new boolean[ALL.size()];
      return module;
   }

   public static List<Module> all() {
      return Collections.unmodifiableList(ALL);
   }

   @SuppressWarnings("unchecked")
   public static <M extends Module> M get(Class<M> type) {
      for (Module module : ALL) {
         if (type.isInstance(module)) {
            return (M)module;
         }
      }

      return null;
   }

   public static void startAll() {
      ALL.forEach(Module::sync);
   }

   public static void stopAll() {
      ALL.forEach(Module::shutdown);
   }

   public static void tick(Minecraft client) {
      boolean acceptBinds = client.screen == null && client.isWindowActive();
      long window = client.getWindow().handle();

      for (int i = 0; i < ALL.size(); i++) {
         Module module = ALL.get(i);
         int key = module.settings().bind;
         boolean down = key > 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
         if (down && !bindDown[i] && acceptBinds) {
            module.toggle();
         }

         bindDown[i] = down;
         module.tick();
      }
   }
}
