//? if >=26.3 {
/*package dev.venti.compat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

// Keyboard keys on 26.3. Minecraft 26.3 replaced GLFW with SDL3: its key codes are SDL scancodes, modifier bits and
// mouse buttons changed too. Venti's config keeps module binds as GLFW key codes, so one config works with every
// Minecraft version; these helpers translate a stored bind to the key Minecraft knows.
@Environment(EnvType.CLIENT)
public final class Keys {
   // GLFW_KEY_LAST + 1: the size of arrays indexed by GLFW key code.
   public static final int GLFW_KEY_COUNT = 349;
   public static final int GLFW_KEY_SPACE = 32;
   public static final int GLFW_KEY_ESCAPE = 256;
   public static final int GLFW_KEY_BACKSPACE = 259;
   private static final int[] TO_SCANCODE = new int[GLFW_KEY_COUNT];
   private static final int[] TO_GLFW = new int[512];

   static {
      map(32, 44); // space
      map(39, 52); // apostrophe
      map(44, 54); // comma
      map(45, 45); // minus
      map(46, 55); // period
      map(47, 56); // slash
      map(48, 39); // 0
      for (int i = 1; i <= 9; i++) {
         map(48 + i, 29 + i); // 1-9
      }

      map(59, 51); // semicolon
      map(61, 46); // equal
      for (int i = 0; i < 26; i++) {
         map(65 + i, 4 + i); // A-Z
      }

      map(91, 47); // left bracket
      map(92, 49); // backslash
      map(93, 48); // right bracket
      map(96, 53); // grave accent
      map(161, 100); // world 1 (non-US backslash)
      map(256, 41); // escape
      map(257, 40); // enter
      map(258, 43); // tab
      map(259, 42); // backspace
      map(260, 73); // insert
      map(261, 76); // delete
      map(262, 79); // right
      map(263, 80); // left
      map(264, 81); // down
      map(265, 82); // up
      map(266, 75); // page up
      map(267, 78); // page down
      map(268, 74); // home
      map(269, 77); // end
      map(280, 57); // caps lock
      map(281, 71); // scroll lock
      map(282, 83); // num lock
      map(283, 70); // print screen
      map(284, 72); // pause
      for (int i = 0; i < 12; i++) {
         map(290 + i, 58 + i); // F1-F12
         map(302 + i, 104 + i); // F13-F24
      }

      map(320, 98); // keypad 0
      for (int i = 1; i <= 9; i++) {
         map(320 + i, 88 + i); // keypad 1-9
      }

      map(330, 99); // keypad decimal
      map(331, 84); // keypad divide
      map(332, 85); // keypad multiply
      map(333, 86); // keypad subtract
      map(334, 87); // keypad add
      map(335, 88); // keypad enter
      map(336, 103); // keypad equal
      map(340, 225); // left shift
      map(341, 224); // left control
      map(342, 226); // left alt
      map(343, 227); // left super
      map(344, 229); // right shift
      map(345, 228); // right control
      map(346, 230); // right alt
      map(347, 231); // right super
      map(348, 101); // menu (application key)
   }

   private Keys() {
   }

   private static void map(int glfw, int scancode) {
      TO_SCANCODE[glfw] = scancode;
      TO_GLFW[scancode] = glfw;
   }

   // The SDL scancode for a stored GLFW key code, or 0 (unknown) when it has none.
   public static int scancode(int glfwKey) {
      return glfwKey > 0 && glfwKey < GLFW_KEY_COUNT ? TO_SCANCODE[glfwKey] : 0;
   }

   // The GLFW key code to store for an SDL scancode, or -1 when it has none.
   public static int glfwKey(int scancode) {
      int glfw = scancode > 0 && scancode < TO_GLFW.length ? TO_GLFW[scancode] : 0;
      return glfw > 0 ? glfw : -1;
   }

   // Whether the key of a stored bind is held.
   public static boolean isDown(int glfwKey) {
      int scancode = scancode(glfwKey);
      return scancode > 0 && InputConstants.isKeyDown(scancode);
   }

   // Which GLFW key codes are held right now (indexed by GLFW key code).
   public static boolean[] glfwKeysDown() {
      boolean[] down = new boolean[GLFW_KEY_COUNT];
      for (int key = GLFW_KEY_SPACE; key < GLFW_KEY_COUNT; key++) {
         down[key] = isDown(key);
      }

      return down;
   }

   // The display name of a stored bind's key, upper case.
   public static String name(int glfwKey) {
      int scancode = scancode(glfwKey);
      return InputConstants.Type.KEYSYM.getOrCreate(scancode).getDisplayName().getString().toUpperCase(Locale.ROOT);
   }
}
*///?}
