package dev.venti.modules.spotify.sky;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface SkyImage extends AutoCloseable {
   int width();

   int height();

   @Override
   void close();
}
