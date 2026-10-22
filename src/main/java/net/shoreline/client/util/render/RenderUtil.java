package net.shoreline.client.util.render;

import net.shoreline.client.util.Globals;

public class RenderUtil implements Globals {
   public static void reloadRenders(boolean softReload) {
      if (softReload) {
         int x = (int)mc.player.getX();
         int y = (int)mc.player.getY();
         int z = (int)mc.player.getZ();
         int d = mc.options.renderDistance().get() * 16;
         mc.levelRenderer.setBlocksDirty(x - d, y - d, z - d, x + d, y + d, z + d);
      } else {
         mc.levelRenderer.allChanged();
      }
   }
}
