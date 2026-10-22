package net.shoreline.client.api.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.font.FontFile;
import net.shoreline.client.api.macro.MacroFile;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.file.ModuleConfigFile;
import net.shoreline.client.api.module.file.ModuleFile;
import net.shoreline.client.api.social.SocialFile;
import net.shoreline.client.api.social.SocialRelation;
import net.shoreline.client.api.waypoint.WaypointFile;
import net.shoreline.client.impl.gui.click.ClickGuiFile;
import net.shoreline.client.impl.module.misc.InvCleanerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.chat.ChatUtil;

public class ClientConfiguration implements Globals {
   private final Set<ConfigFile> files = new HashSet<>();
   private Path clientDir;
   private ModuleConfigFile modulesFile;
   private MacroFile macrosFile;
   private final ClickGuiFile clickGuiFile;
   private final FontFile fontFile;
   private final ExecutorService executor = Executors.newSingleThreadExecutor();

   public ClientConfiguration() {
      Path runningDir = mc.gameDirectory.toPath();

      try {
         File homeDir = new File(System.getProperty("user.home"));
         this.clientDir = homeDir.toPath();
      } catch (Exception var19) {
         Shoreline.error("Could not access home dir, defaulting to running dir");
         var19.printStackTrace();
         this.clientDir = runningDir;
      } finally {
         if (this.clientDir == null || !Files.exists(this.clientDir) || !Files.isWritable(this.clientDir)) {
            this.clientDir = runningDir;
         }

         this.clientDir = mc.gameDirectory.toPath().resolve("shoreline");
         if (!Files.exists(this.clientDir)) {
            try {
               Files.createDirectory(this.clientDir);
            } catch (IOException var18) {
               Shoreline.error("Could not create client dir");
               var18.printStackTrace();
            }
         }

         Path configDir = this.clientDir.resolve("Configs");
         Path keybindsDir = this.clientDir.resolve("Macros");
         if (!Files.exists(configDir)) {
            try {
               Files.createDirectory(configDir);
               Files.createDirectory(keybindsDir);
            } catch (IOException var17) {
               Shoreline.error("Could not create config/macro dir");
               var17.printStackTrace();
            }
         }
      }

      this.files.add(new MacroFile(this.clientDir));

      for (Module module : Managers.MODULE.getModules()) {
         this.files.add(new ModuleFile(this.clientDir.resolve("Modules"), module));
      }

      this.files.add(InvCleanerModule.getInstance().getBlacklistFile(this.clientDir));
      this.files.add(new WaypointFile(this.clientDir));

      for (SocialRelation relation : SocialRelation.values()) {
         this.files.add(new SocialFile(this.clientDir, relation));
      }

      this.clickGuiFile = new ClickGuiFile(this.clientDir);
      this.fontFile = new FontFile(this.clientDir);
   }

   public void saveClient() {
      for (ConfigFile file : this.files) {
         file.save();
      }
   }

   public void saveClientModules() {
      for (ConfigFile file : this.files) {
         if (file instanceof ModuleFile) {
            file.save();
         }
      }
   }

   public void loadClient() {
      this.executor.submit(() -> {
         for (ConfigFile file : this.files) {
            file.load();
         }
      });
   }

   public void loadClientModules() {
      this.executor.submit(() -> {
         for (ConfigFile file : this.files) {
            if (file instanceof ModuleFile) {
               file.load();
            }
         }
      });
   }

   public void saveModuleConfiguration(String configFile) {
      this.modulesFile = new ModuleConfigFile(this.clientDir.resolve("Configs"), configFile);
      this.modulesFile.save();
   }

   public boolean loadModuleConfiguration(String configFile) {
      Path configDir = this.clientDir.resolve("Configs");
      this.modulesFile = new ModuleConfigFile(configDir, configFile);
      if (!Files.exists(configDir.resolve(configFile + ".json"))) {
         ChatUtil.error("Could not find config file: " + configFile);
         return false;
      } else {
         this.executor.submit(() -> this.modulesFile.load());
         return true;
      }
   }

   public void saveKeybindConfiguration(String configFile) {
      this.macrosFile = new MacroFile(this.clientDir.resolve("Macros"), configFile);
      this.macrosFile.save();
   }

   public boolean loadKeybindConfiguration(String configFile) {
      Path macrosDir = this.clientDir.resolve("Macros");
      this.macrosFile = new MacroFile(macrosDir, configFile);
      if (!Files.exists(macrosDir.resolve(configFile + ".json"))) {
         ChatUtil.error("Could not find config file: " + configFile);
         return false;
      } else {
         this.macrosFile.load();
         return true;
      }
   }

   public void saveClickGui() {
      this.clickGuiFile.save();
   }

   public void loadClickGui() {
      this.clickGuiFile.load();
   }

   public void saveFonts() {
      this.fontFile.save();
   }

   public void loadFonts() {
      this.fontFile.load();
   }

   public Set<ConfigFile> getFiles() {
      return this.files;
   }

   public void addFile(ConfigFile configFile) {
      this.files.add(configFile);
   }

   public void removeFile(ConfigFile configFile) {
      this.files.remove(configFile);
   }

   public Path getClientDirectory() {
      return this.clientDir;
   }
}
