package net.shoreline.client;

import net.fabricmc.loader.api.FabricLoader;

public class ShorelineMod {
   public static final String MOD_NAME = "Shoreline";
   public static final String MOD_VER = "1.0";
   public static final String MOD_MC_VER = "1.21.1";

   public ShorelineMod() {
      System.exit(-1);
   }

   public void onInitializeClient() {
      Shoreline.init();
   }

   public static boolean isBaritonePresent() {
      return FabricLoader.getInstance().getModContainer("baritone").isPresent();
   }
}
