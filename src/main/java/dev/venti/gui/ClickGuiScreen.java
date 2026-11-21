package dev.venti.gui;

import dev.venti.compat.Mc;
import dev.venti.imgui.ImGuiManager;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Holds the cursor and game input while the ImGui click GUI is up. */
public final class ClickGuiScreen extends Screen {
   private final ClickGui gui = new ClickGui();

   public ClickGuiScreen() {
      super(Component.literal("Venti"));
   }

   public static boolean isOpen() {
      return Mc.screen() instanceof ClickGuiScreen;
   }

   /** HUD elements (Spotify card, Venti HUD, Hotkeys) show samples and can be dragged while the Spotify, HUD or Hotkeys page is open. */
   public static boolean editingHud() {
      return Mc.screen() instanceof ClickGuiScreen screen && screen.gui.editingHud();
   }

   @Override
   protected void init() {
      ImGuiManager.getInstance().activate(this.gui::render);
   }

   @Override
   public void tick() {
      if (this.gui.closeRequested()) {
         this.onClose();
      }
   }

   @Override
   public void removed() {
      ImGuiManager.getInstance().deactivate();
      this.gui.saveAll();
   }

   //? if >=26.2 {
   @Override
   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
   }
   //?} else {
   /*@Override
   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
   }
   *///?}

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
