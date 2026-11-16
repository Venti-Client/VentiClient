package dev.venti.gui;

/**
 * A module that draws extra content on its settings page (for things settings rows can't express, like the
 * waypoint list). Called inside the current ImGui window, after the settings rows, in both GUI styles.
 */
public interface CustomPage {
   /**
    * @param width  usable width in pixels, starting at the current cursor x
    * @param scale  the GUI scale factor (multiply your pixel sizes by it)
    * @param accent the menu color, already in ImGui's ABGR order
    */
   void renderPage(float width, float scale, int accent);
}
