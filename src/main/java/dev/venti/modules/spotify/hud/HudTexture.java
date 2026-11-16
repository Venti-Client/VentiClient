package dev.venti.modules.spotify.hud;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

/** A raw GL texture ImGui can draw directly (album art). */
record HudTexture(int id, int width, int height) {
   static HudTexture fromPixels(ByteBuffer rgba, int width, int height, boolean pixelated) {
      return new HudTexture(createTexture(rgba, width, height, pixelated), width, height);
   }

   private static int createTexture(ByteBuffer pixels, int width, int height, boolean pixelated) {
      int prevTexture = GL11.glGetInteger(32873);
      int prevUnpackBuffer = GL11.glGetInteger(35055);
      int prevAlignment = GL11.glGetInteger(3317);
      int prevRowLength = GL11.glGetInteger(3314);
      int prevSkipPixels = GL11.glGetInteger(3316);
      int prevSkipRows = GL11.glGetInteger(3315);

      int var11;
      try {
         GL15.glBindBuffer(35052, 0);
         GL11.glPixelStorei(3317, 4);
         GL11.glPixelStorei(3314, 0);
         GL11.glPixelStorei(3316, 0);
         GL11.glPixelStorei(3315, 0);
         int id = GL11.glGenTextures();
         GL11.glBindTexture(3553, id);
         GL11.glTexParameteri(3553, 10241, pixelated ? 9728 : 9987);
         GL11.glTexParameteri(3553, 10240, pixelated ? 9728 : 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexImage2D(3553, 0, 32856, width, height, 0, 6408, 5121, pixels);
         if (!pixelated) {
            GL30.glGenerateMipmap(3553);
         }

         var11 = id;
      } finally {
         GL11.glBindTexture(3553, prevTexture);
         GL15.glBindBuffer(35052, prevUnpackBuffer);
         GL11.glPixelStorei(3317, prevAlignment);
         GL11.glPixelStorei(3314, prevRowLength);
         GL11.glPixelStorei(3316, prevSkipPixels);
         GL11.glPixelStorei(3315, prevSkipRows);
      }

      return var11;
   }

}
