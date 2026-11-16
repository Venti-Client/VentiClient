package dev.venti.gui;

import dev.venti.imgui.ImGuiManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Holds the cursor and game input while the ImGui click GUI is up. */
public final class ClickGuiScreen extends Screen {
   private final ClickGui gui = new ClickGui();

   public ClickGuiScreen() {
      super(Text.literal("Venti"));
   }

   public static boolean isOpen() {
      return MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen;
   }

   /** HUD elements (Spotify card, Venti HUD, Hotkeys) show samples and can be dragged while the Spotify, HUD or Hotkeys page is open. */
   public static boolean editingHud() {
      return MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen screen && screen.gui.editingHud();
   }

   @Override
   protected void init() {
      ImGuiManager.getInstance().activate(this.gui::render);
   }

   @Override
   public void tick() {
      if (this.gui.closeRequested()) {
         this.close();
      }
   }

   @Override
   public void removed() {
      ImGuiManager.getInstance().deactivate();
      this.gui.saveAll();
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
