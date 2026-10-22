package net.shoreline.client;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.shoreline.client.api.Identifiable;
import net.shoreline.client.api.file.ClientConfiguration;
import net.shoreline.client.init.Managers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class Shoreline implements ClientModInitializer {
   private static final Logger LOGGER = LogManager.getLogger("Shoreline");
   public static ClientConfiguration CONFIG;
   public static ShutdownHook SHUTDOWN;
   public static Executor EXECUTOR;

   public static void init() {
      info("This build of Shoreline is on Git hash {} and was compiled on {}", "fa4cb0f", "03/07/2026 01:41");
      info("Starting preInit ...");
      EXECUTOR = Executors.newFixedThreadPool(1);
      info("Starting init ...");
      Managers.init();
      info("Starting postInit ...");
      CONFIG = new ClientConfiguration();
      Managers.postInit();
      SHUTDOWN = new ShutdownHook();
      Runtime.getRuntime().addShutdownHook(SHUTDOWN);
      CONFIG.loadClient();
   }

   public void onInitializeClient() {
      init();
   }

   public static void info(String message) {
      LOGGER.info(message);
   }

   public static void info(String message, Object... params) {
      LOGGER.info(message, params);
   }

   public static void info(Identifiable feature, String message) {
      LOGGER.info(String.format("[%s] %s", feature.getId(), message));
   }

   public static void info(Identifiable feature, String message, Object... params) {
      LOGGER.info(String.format("[%s] %s", feature.getId(), message), params);
   }

   public static void error(String message) {
      LOGGER.error(message);
   }

   public static void error(String message, Object... params) {
      LOGGER.error(message, params);
   }

   public static void error(Identifiable feature, String message) {
      LOGGER.error(String.format("[%s] %s", feature.getId(), message));
   }

   public static void error(Identifiable feature, String message, Object... params) {
      LOGGER.error(String.format("[%s] %s", feature.getId(), message), params);
   }
}
