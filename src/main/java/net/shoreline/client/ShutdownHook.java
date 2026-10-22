package net.shoreline.client;

public class ShutdownHook extends Thread {
   public ShutdownHook() {
      this.setName("Shoreline-ShutdownHook");
   }

   @Override
   public void run() {
      Shoreline.info("Saving configurations and shutting down!");
      Shoreline.CONFIG.saveClient();
      Shoreline.CONFIG.saveClickGui();
      Shoreline.CONFIG.saveFonts();
   }
}
