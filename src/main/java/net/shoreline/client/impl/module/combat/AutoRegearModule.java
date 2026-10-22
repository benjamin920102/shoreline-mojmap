package net.shoreline.client.impl.module.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AutoRegearModule extends ToggleModule {
   private static AutoRegearModule INSTANCE;
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "The item swap delay", 0.0F, 0.15F, 2.0F));
   private final Timer clickTimer = new CacheTimer();
   private final Map<Integer, Item> regearInventory = new ConcurrentHashMap<>();

   public AutoRegearModule() {
      super("AutoRegear", "Automatically refills your inventory with gear", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoRegearModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (this.regearInventory.isEmpty()) {
         this.sendModuleError("No regear configuration set! Use the .regear command to set your regear inventory.");
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.player.containerMenu instanceof ChestMenu containerScreen) {
            for (Entry<Integer, Item> entry : this.regearInventory.entrySet()) {
               if (entry.getValue() != Items.AIR) {
                  int slotId = containerScreen.getContainer().getContainerSize() + entry.getKey() - 9;
                  ItemStack playerStack = mc.player.getInventory().getItem(slotId);
                  if (playerStack.getCount() < playerStack.getMaxStackSize()) {
                     Queue<Integer> validSlots = new ArrayDeque<>();

                     for (int i = 0; i < containerScreen.getContainer().getContainerSize(); i++) {
                        Slot slot = containerScreen.getSlot(i);
                        if (slot.hasItem() && slot.getItem().getItem() == entry.getValue()) {
                           validSlots.add(i);
                        }
                     }

                     for (;
                        !validSlots.isEmpty() && playerStack.getCount() < playerStack.getMaxStackSize();
                        playerStack = mc.player.getInventory().getItem(slotId)
                     ) {
                        if (this.clickTimer.passed(this.delayConfig.getValue() * 1000.0F)) {
                           int swapSlot = validSlots.remove();
                           Managers.INVENTORY.pickupSlot(swapSlot);
                           Managers.INVENTORY.pickupSlot(slotId);
                           this.clickTimer.reset();
                        }
                     }
                  }
               }
            }
         }

         if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerBoxScreenHandler) {
            for (Entry<Integer, Item> entryx : this.regearInventory.entrySet()) {
               if (entryx.getValue() != Items.AIR) {
                  int slotId = shulkerBoxScreenHandler.getItems().size() + entryx.getKey() - 9;
                  ItemStack playerStack = mc.player.getInventory().getItem(slotId);
                  if (playerStack.getCount() < playerStack.getMaxStackSize()) {
                     Queue<Integer> validSlots = new ArrayDeque<>();

                     for (int ix = 0; ix < shulkerBoxScreenHandler.slots.size(); ix++) {
                        Slot slot = shulkerBoxScreenHandler.slots.get(ix);
                        if (slot.hasItem() && slot.getItem().getItem() == entryx.getValue()) {
                           validSlots.add(ix);
                        }
                     }

                     for (;
                        !validSlots.isEmpty() && playerStack.getCount() < playerStack.getMaxStackSize();
                        playerStack = mc.player.getInventory().getItem(slotId)
                     ) {
                        if (this.clickTimer.passed(this.delayConfig.getValue() * 1000.0F)) {
                           int swapSlot = validSlots.remove();
                           Managers.INVENTORY.pickupSlot(swapSlot);
                           Managers.INVENTORY.pickupSlot(slotId);
                           this.clickTimer.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void clearPlayerInventory() {
      this.regearInventory.clear();
   }

   public void savePlayerInventory() {
      this.regearInventory.clear();

      for (int i = 0; i < 35; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         this.regearInventory.put(i < 9 ? i + 36 : i, stack.getItem());
      }
   }

   public void saveRegearFile() {
      if (!this.regearInventory.isEmpty()) {
         try {
            Path regearFile = Shoreline.CONFIG.getClientDirectory().resolve("regear.json");
            if (!Files.exists(regearFile)) {
               Files.createFile(regearFile);
            }

            FileWriter writer = new FileWriter(regearFile.toFile());
            JsonArray jsonArray = new JsonArray();

            for (Entry<Integer, Item> entry : this.regearInventory.entrySet()) {
               JsonObject jsonObject1 = new JsonObject();
               jsonObject1.addProperty("slotId", entry.getKey());
               jsonObject1.addProperty("item", BuiltInRegistries.ITEM.getKey(entry.getValue()).toString());
               jsonArray.add(jsonObject1);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonArray, writer);
         } catch (IOException var7) {
         }
      }
   }

   public void loadRegearFile() {
      Path regearFile = Shoreline.CONFIG.getClientDirectory().resolve("regear.json");
      if (Files.exists(regearFile)) {
         try {
            this.regearInventory.clear();
            FileReader reader = new FileReader(regearFile.toFile());
            Gson gson = new Gson();

            for (JsonElement jsonElement : (JsonArray)gson.fromJson(reader, JsonArray.class)) {
               JsonObject jsonObject = jsonElement.getAsJsonObject();
               int slotId = jsonObject.get("slotId").getAsInt();
               Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(jsonObject.get("item").getAsString()));
               this.regearInventory.put(slotId, item);
            }
         } catch (IOException var10) {
            var10.printStackTrace();
         }
      }
   }
}
