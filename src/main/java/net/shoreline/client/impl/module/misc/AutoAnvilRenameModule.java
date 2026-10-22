package net.shoreline.client.impl.module.misc;

import java.util.List;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.eventbus.annotation.EventListener;

public final class AutoAnvilRenameModule extends ToggleModule {
   Config<AutoAnvilRenameModule.Selection> autoRenameSelectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of items to rename", AutoAnvilRenameModule.Selection.ALL, AutoAnvilRenameModule.Selection.values())
   );
   Config<List<Item>> autoRenameWhitelistConfig = this.register(
      new ItemListConfig<>(
         "Whitelist",
         "The items to rename.",
         Items.SHULKER_BOX,
         Items.WHITE_SHULKER_BOX,
         Items.LIGHT_GRAY_SHULKER_BOX,
         Items.GRAY_SHULKER_BOX,
         Items.BLACK_SHULKER_BOX,
         Items.BROWN_SHULKER_BOX,
         Items.RED_SHULKER_BOX,
         Items.ORANGE_SHULKER_BOX,
         Items.YELLOW_SHULKER_BOX,
         Items.LIME_SHULKER_BOX,
         Items.GREEN_SHULKER_BOX,
         Items.CYAN_SHULKER_BOX,
         Items.LIGHT_BLUE_SHULKER_BOX,
         Items.BLUE_SHULKER_BOX,
         Items.PURPLE_SHULKER_BOX,
         Items.PINK_SHULKER_BOX
      )
   );
   Config<List<Item>> autoRenameBlacklistConfig = this.register(new ItemListConfig<>("Blacklist", "The items to not rename.", Items.EXPERIENCE_BOTTLE));
   Config<String> autoRenameTextConfig = this.register(new StringConfig("Text", "The text to rename the items to.", "ShorelineClient.net"));
   Config<Integer> autoRenameDelayConfig = this.register(new NumberConfig<>("Delay", "The delay between renaming items.", 0, 10, 20));
   Config<Boolean> debugConfig = this.register(new BooleanConfig("Debug", "Prints debug information to chat.", false, () -> false));
   private final CacheTimer delayTimer = new CacheTimer();

   public AutoAnvilRenameModule() {
      super("AutoAnvilRename", "Automatically renames items in anvils.", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null && mc.screen instanceof AnvilScreen anvilScreen) {
         if (this.delayTimer.passed(this.autoRenameDelayConfig.getValue())) {
            if (mc.player.experienceLevel <= 0 && !mc.player.isCreative()) {
               if (this.debugConfig.getValue()) {
                  this.sendModuleMessage("Not enough experience levels!");
               }
            } else {
               AnvilMenu screenHandler = anvilScreen.getMenu();
               if (!screenHandler.getSlot(1).getItem().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 1);
               } else if (!screenHandler.getSlot(0).getItem().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 0);
               } else if (!screenHandler.getSlot(2).getItem().isEmpty()) {
                  this.moveToEmptySlot(screenHandler, 2);
               } else {
                  for (int i = 3; i < 39; i++) {
                     ItemStack itemStack = screenHandler.getSlot(i).getItem();
                     if (!itemStack.isEmpty()
                        && !this.equalsName(itemStack, this.autoRenameTextConfig.getName())
                        && (
                           this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.BLACKLIST
                              || !this.autoRenameBlacklistConfig.getValue().contains(itemStack.getItem())
                        )
                        && (
                           this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.WHITELIST
                              || this.autoRenameWhitelistConfig.getValue().contains(itemStack.getItem())
                        )) {
                        String name = !this.autoRenameTextConfig.getValue().trim().isEmpty() ? this.autoRenameTextConfig.getValue() : "";
                        mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, i, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, screenHandler.getSlot(0).index, 0, ClickType.PICKUP, mc.player);
                        ((AnvilScreen)mc.screen).name.setValue(name);
                        if (this.debugConfig.getValue()) {
                           this.sendModuleMessage("Successfully renamed item in slot: " + i + ".");
                        }

                        mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, screenHandler.getSlot(2).index, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, i, 0, ClickType.PICKUP, mc.player);
                        break;
                     }
                  }

                  this.delayTimer.reset();
               }
            }
         }
      }
   }

   private void moveToEmptySlot(AnvilMenu screenHandler, int slot) {
      if (mc.gameMode != null) {
         for (int i = 3; i < 39; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getItem();
            if (itemStack.isEmpty()) {
               mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, screenHandler.getSlot(slot).index, 0, ClickType.PICKUP, mc.player);
               mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, i, 0, ClickType.PICKUP, mc.player);
               return;
            }
         }

         mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, screenHandler.getSlot(slot).index, 0, ClickType.THROW, mc.player);
      }
   }

   private boolean equalsName(ItemStack itemStack, String itemName) {
      return itemName.trim().isEmpty() ? itemStack.get(DataComponents.CUSTOM_NAME) == null : itemStack.getHoverName().getString().equals(itemName);
   }

   private static enum Selection {
      ALL,
      WHITELIST,
      BLACKLIST;
   }
}
