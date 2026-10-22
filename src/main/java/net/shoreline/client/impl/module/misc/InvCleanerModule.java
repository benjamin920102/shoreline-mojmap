package net.shoreline.client.impl.module.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.file.ConfigFile;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class InvCleanerModule extends ToggleModule {
   private static InvCleanerModule INSTANCE;
   Config<List<Item>> blacklistConfig = this.register(new ItemListConfig<>("Blacklist", "The items to throw"));
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "The delay between removing items from the inventory", 0.05F, 0.0F, 1.0F));
   Config<Boolean> hotbarConfig = this.register(new BooleanConfig("Hotbar", "Cleans the hotbar inventory slots", true));
   private final Timer invCleanTimer = new CacheTimer();

   public InvCleanerModule() {
      super("InvCleaner", "Automatically cleans the player inventory", ModuleCategory.MISCELLANEOUS);
   }

   public static InvCleanerModule getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new InvCleanerModule();
      }

      return INSTANCE;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         for (Item item : this.blacklistConfig.getValue()) {
            for (int i = 35; i >= (this.hotbarConfig.getValue() ? 0 : 9); i--) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               if (!stack.isEmpty() && stack.getItem() == item && this.invCleanTimer.passed(this.delayConfig.getValue() * 1000.0F)) {
                  mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, -999, 0, ClickType.PICKUP, mc.player);
                  this.invCleanTimer.reset();
                  break;
               }
            }
         }
      }
   }

   public ConfigFile getBlacklistFile(Path clientDir) {
      return new InvCleanerModule.InvCleanerFile(clientDir);
   }

   public class InvCleanerFile extends ConfigFile {
      public InvCleanerFile(Path clientDir) {
         super(clientDir, "inv-cleaner");
      }

      @Override
      public void save() {
         try {
            Path filepath = this.getFilepath();
            if (!Files.exists(filepath)) {
               Files.createFile(filepath);
            }

            JsonObject json = new JsonObject();
            JsonArray itemArray = new JsonArray();

            for (Item item : InvCleanerModule.this.blacklistConfig.getValue()) {
               itemArray.add(item.getDescriptionId());
            }

            json.add("items", itemArray);
            this.write(filepath, this.serialize(json));
         } catch (IOException var6) {
            Shoreline.error("Could not save file for inv cleaner!");
            var6.printStackTrace();
         }
      }

      @Override
      public void load() {
         try {
            Path filepath = this.getFilepath();
            if (Files.exists(filepath)) {
               String content = this.read(filepath);
               JsonObject object = this.parseObject(content);
               if (object != null && object.has("items")) {
                  for (JsonElement var6 : object.getAsJsonArray("items")) {
                     ;
                  }
               }
            }
         } catch (IOException var7) {
            Shoreline.error("Could not read file for inv cleaner!");
            var7.printStackTrace();
         }
      }
   }
}
