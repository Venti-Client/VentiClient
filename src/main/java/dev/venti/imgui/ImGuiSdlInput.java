//? if >=26.3 {
/*package dev.venti.imgui;

import com.mojang.blaze3d.platform.Window;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.ImGuiKey;
import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.system.MemoryStack;

// ImGui platform backend for 26.3, where Minecraft runs on SDL3 and imgui-java's GLFW backend cannot work.
// Minecraft's own SDL event handling feeds it (see dev.venti.mixin.ImGuiInputMixin); per frame it sets the time step,
// the mouse position and focus. Coordinates are framebuffer pixels, as ImGuiManager sets up the display.
@Environment(EnvType.CLIENT)
final class ImGuiSdlInput {
   // SDL_Keymod bits.
   private static final int MOD_SHIFT = 0x0003;
   private static final int MOD_CTRL = 0x00C0;
   private static final int MOD_ALT = 0x0300;
   private static final int MOD_GUI = 0x0C00;
   // SDL keycodes of non-character keys are their scancode with this bit set.
   private static final int SCANCODE_MASK = 1 << 30;
   private static final int[] BY_SCANCODE = new int[512];
   private long lastFrame;
   private boolean focused = true;

   static {
      for (int i = 0; i < 26; i++) {
         BY_SCANCODE[4 + i] = ImGuiKey.A + i;
      }

      BY_SCANCODE[39] = ImGuiKey._0;
      for (int i = 1; i <= 9; i++) {
         BY_SCANCODE[29 + i] = ImGuiKey._1 + i - 1;
      }

      BY_SCANCODE[40] = ImGuiKey.Enter;
      BY_SCANCODE[41] = ImGuiKey.Escape;
      BY_SCANCODE[42] = ImGuiKey.Backspace;
      BY_SCANCODE[43] = ImGuiKey.Tab;
      BY_SCANCODE[44] = ImGuiKey.Space;
      BY_SCANCODE[45] = ImGuiKey.Minus;
      BY_SCANCODE[46] = ImGuiKey.Equal;
      BY_SCANCODE[47] = ImGuiKey.LeftBracket;
      BY_SCANCODE[48] = ImGuiKey.RightBracket;
      BY_SCANCODE[49] = ImGuiKey.Backslash;
      BY_SCANCODE[51] = ImGuiKey.Semicolon;
      BY_SCANCODE[52] = ImGuiKey.Apostrophe;
      BY_SCANCODE[53] = ImGuiKey.GraveAccent;
      BY_SCANCODE[54] = ImGuiKey.Comma;
      BY_SCANCODE[55] = ImGuiKey.Period;
      BY_SCANCODE[56] = ImGuiKey.Slash;
      BY_SCANCODE[57] = ImGuiKey.CapsLock;
      for (int i = 0; i < 12; i++) {
         BY_SCANCODE[58 + i] = ImGuiKey.F1 + i;
         BY_SCANCODE[104 + i] = ImGuiKey.F13 + i;
      }

      BY_SCANCODE[70] = ImGuiKey.PrintScreen;
      BY_SCANCODE[71] = ImGuiKey.ScrollLock;
      BY_SCANCODE[72] = ImGuiKey.Pause;
      BY_SCANCODE[73] = ImGuiKey.Insert;
      BY_SCANCODE[74] = ImGuiKey.Home;
      BY_SCANCODE[75] = ImGuiKey.PageUp;
      BY_SCANCODE[76] = ImGuiKey.Delete;
      BY_SCANCODE[77] = ImGuiKey.End;
      BY_SCANCODE[78] = ImGuiKey.PageDown;
      BY_SCANCODE[79] = ImGuiKey.RightArrow;
      BY_SCANCODE[80] = ImGuiKey.LeftArrow;
      BY_SCANCODE[81] = ImGuiKey.DownArrow;
      BY_SCANCODE[82] = ImGuiKey.UpArrow;
      BY_SCANCODE[83] = ImGuiKey.NumLock;
      BY_SCANCODE[84] = ImGuiKey.KeypadDivide;
      BY_SCANCODE[85] = ImGuiKey.KeypadMultiply;
      BY_SCANCODE[86] = ImGuiKey.KeypadSubtract;
      BY_SCANCODE[87] = ImGuiKey.KeypadAdd;
      BY_SCANCODE[88] = ImGuiKey.KeypadEnter;
      for (int i = 1; i <= 9; i++) {
         BY_SCANCODE[88 + i] = ImGuiKey.Keypad1 + i - 1;
      }

      BY_SCANCODE[98] = ImGuiKey.Keypad0;
      BY_SCANCODE[99] = ImGuiKey.KeypadDecimal;
      BY_SCANCODE[101] = ImGuiKey.Menu;
      BY_SCANCODE[103] = ImGuiKey.KeypadEqual;
      BY_SCANCODE[224] = ImGuiKey.LeftCtrl;
      BY_SCANCODE[225] = ImGuiKey.LeftShift;
      BY_SCANCODE[226] = ImGuiKey.LeftAlt;
      BY_SCANCODE[227] = ImGuiKey.LeftSuper;
      BY_SCANCODE[228] = ImGuiKey.RightCtrl;
      BY_SCANCODE[229] = ImGuiKey.RightShift;
      BY_SCANCODE[230] = ImGuiKey.RightAlt;
      BY_SCANCODE[231] = ImGuiKey.RightSuper;
   }

   void init() {
      ImGuiIO io = ImGui.getIO();
      io.setBackendPlatformName("venti_sdl3");
      Minecraft mc = Minecraft.getInstance();
      io.setGetClipboardTextFn(new ImStrSupplier() {
         @Override
         public String get() {
            return mc.keyboardHandler.getClipboard();
         }
      });
      io.setSetClipboardTextFn(new ImStrConsumer() {
         @Override
         public void accept(String text) {
            mc.keyboardHandler.setClipboard(text);
         }
      });
      this.lastFrame = 0L;
      this.focused = true;
   }

   void newFrame() {
      ImGuiIO io = ImGui.getIO();
      long now = System.nanoTime();
      io.setDeltaTime(this.lastFrame == 0L ? 1.0F / 60.0F : Math.max(1.0E-5F, (now - this.lastFrame) / 1.0E9F));
      this.lastFrame = now;
      Window window = Minecraft.getInstance().getWindow();
      boolean focusedNow = window.isFocused();
      if (focusedNow != this.focused) {
         this.focused = focusedNow;
         io.addFocusEvent(focusedNow);
      }

      if (focusedNow) {
         try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            SDLMouse.SDL_GetMouseState(x, y);
            this.mouseMoved(x.get(0), y.get(0));
         }
      }
   }

   // Window coordinates (as SDL reports them) to ImGui's framebuffer pixels.
   void mouseMoved(double x, double y) {
      Window window = Minecraft.getInstance().getWindow();
      float sx = window.getScreenWidth() > 0 ? (float)window.getWidth() / window.getScreenWidth() : 1.0F;
      float sy = window.getScreenHeight() > 0 ? (float)window.getHeight() / window.getScreenHeight() : 1.0F;
      ImGui.getIO().addMousePosEvent((float)(x * sx), (float)(y * sy));
   }

   // SDL mouse buttons count from 1 (left, middle, right, x1, x2); ImGui's from 0 (left, right, middle, x1, x2).
   void mouseButton(int button, int modifiers, boolean down) {
      int imgui = switch (button) {
         case 1 -> 0;
         case 2 -> 2;
         case 3 -> 1;
         case 4 -> 3;
         case 5 -> 4;
         default -> -1;
      };
      if (imgui >= 0) {
         this.modifiers(modifiers);
         ImGui.getIO().addMouseButtonEvent(imgui, down);
      }
   }

   void scroll(double x, double y) {
      ImGui.getIO().addMouseWheelEvent((float)x, (float)y);
   }

   // action: 1 press, 0 release, -1 repeat (ImGui repeats held keys itself).
   void key(int action, KeyEvent event) {
      if (action == -1) {
         return;
      }

      this.modifiers(event.modifiers());
      int key = imguiKey(event.key(), event.keycode());
      if (key != ImGuiKey.None) {
         ImGui.getIO().addKeyEvent(key, action == 1);
      }
   }

   void text(String text) {
      ImGui.getIO().addInputCharactersUTF8(text);
   }

   private void modifiers(int mods) {
      ImGuiIO io = ImGui.getIO();
      io.addKeyEvent(ImGuiKey.ImGuiMod_Ctrl, (mods & MOD_CTRL) != 0);
      io.addKeyEvent(ImGuiKey.ImGuiMod_Shift, (mods & MOD_SHIFT) != 0);
      io.addKeyEvent(ImGuiKey.ImGuiMod_Alt, (mods & MOD_ALT) != 0);
      io.addKeyEvent(ImGuiKey.ImGuiMod_Super, (mods & MOD_GUI) != 0);
   }

   // Letters and digits follow the keyboard layout (the keycode), as in imgui_impl_sdl3, so shortcuts like Ctrl+A
   // land on the right key; everything else goes by its physical position (the scancode).
   private static int imguiKey(int scancode, int keycode) {
      if (keycode >= 'a' && keycode <= 'z') {
         return ImGuiKey.A + keycode - 'a';
      } else if (keycode >= '0' && keycode <= '9' && (keycode & SCANCODE_MASK) == 0 && scancode < 89) {
         return keycode == '0' ? ImGuiKey._0 : ImGuiKey._1 + keycode - '1';
      } else {
         return scancode > 0 && scancode < BY_SCANCODE.length ? BY_SCANCODE[scancode] : ImGuiKey.None;
      }
   }
}
*///?}
