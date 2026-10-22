package net.shoreline.client.impl.module.combat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class ReplenishModule extends ToggleModule {
   private static ReplenishModule INSTANCE;
   Config<Integer> percentConfig = this.register(new NumberConfig<>("Percent", "The minimum percent of total stack before replenishing", 1, 25, 80));
   Config<Boolean> resistantConfig = this.register(new BooleanConfig("AllowResistant", "Refills obsidian with other types of resistant blocks", false));
   private final Map<Integer, ItemStack> hotbarCache = new ConcurrentHashMap<>();
   private final Timer lastDroppedTimer = new CacheTimer();

   public ReplenishModule() {
      super("Replenish", "Automatically replaces items in your hotbar", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static ReplenishModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.hotbarCache.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.hotbarCache.clear();
   }

   @EventListener
   public void onEntityDeath(EntityDeathEvent event) {
      if (event.getEntity() instanceof LocalPlayer) {
         this.hotbarCache.clear();
      }
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.options.keyDrop.isDown()) {
         this.lastDroppedTimer.reset();
      }

      boolean pauseReplenish = this.isInInventoryScreen() || !this.lastDroppedTimer.passed(100);
      if (!pauseReplenish) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) {
               ItemStack cachedStack = this.hotbarCache.getOrDefault(i, null);
               if (cachedStack != null && !cachedStack.isEmpty()) {
                  this.replenishStack(i, cachedStack);
                  break;
               }
            } else if (stack.isStackable()) {
               double percentage = (double)stack.getCount() / stack.getMaxStackSize() * 100.0;
               if (percentage <= this.percentConfig.getValue().intValue()) {
                  this.replenishStack(i, stack);
                  break;
               }
            }
         }
      }

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = mc.player.getInventory().getItem(ix);
         if (!stack.isEmpty() || pauseReplenish) {
            if (this.hotbarCache.containsKey(ix)) {
               this.hotbarCache.replace(ix, stack.copy());
            } else {
               this.hotbarCache.put(ix, stack.copy());
            }
         }
      }
   }

   public boolean isInInventoryScreen() {
      return mc.screen instanceof ContainerScreen || mc.screen instanceof ShulkerBoxScreen || mc.screen instanceof InventoryScreen;
   }

   private void replenishStack(int slot, ItemStack stack) {
      int slot1 = -1;
      boolean outOfObsidian = stack.getItem() == Items.OBSIDIAN && InventoryUtil.count(Items.OBSIDIAN) <= 1;

      for (int i = 9; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (!itemStack.isEmpty() && this.isSame(stack, itemStack, outOfObsidian) && itemStack.isStackable()) {
            slot1 = i;
         }
      }

      if (slot1 != -1) {
         mc.gameMode.handleInventoryMouseClick(0, slot1, 0, ClickType.PICKUP, mc.player);
         mc.gameMode.handleInventoryMouseClick(0, slot + 36, 0, ClickType.PICKUP, mc.player);
         if (!mc.player.containerMenu.getCarried().isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(0, slot1, 0, ClickType.PICKUP, mc.player);
         }
      }
   }

   public boolean isSame(ItemStack stack1, ItemStack stack2, boolean outOfObsidian) {
      if (this.resistantConfig.getValue() && stack1.getItem() == Items.OBSIDIAN && outOfObsidian) {
         return stack2.getItem() == Items.ENDER_CHEST || stack2.getItem() == Items.CRYING_OBSIDIAN;
      } else if (!(
         stack1.getItem() instanceof BlockItem blockItem
            && (!(stack2.getItem() instanceof BlockItem blockItem1) || blockItem.getBlock() != blockItem1.getBlock())
      )) {
         return !stack1.getHoverName().getString().equals(stack2.getHoverName().getString()) ? false : stack1.getItem().equals(stack2.getItem());
      } else {
         return false;
      }
   }
}
