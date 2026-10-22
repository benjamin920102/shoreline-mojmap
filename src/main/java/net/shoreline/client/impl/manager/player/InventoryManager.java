package net.shoreline.client.impl.manager.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.ItemDesyncEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.combat.ReplenishModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorBundlePacket;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class InventoryManager implements Globals {
   private final List<InventoryManager.PreSwapData> swapData = new CopyOnWriteArrayList<>();
   private int slot;

   public InventoryManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketOutBound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
         int packetSlot = packet.getSlot();
         if (!Inventory.isHotbarSlot(packetSlot) || this.slot == packetSlot) {
            event.setCanceled(true);
            return;
         }

         this.slot = packetSlot;
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundSetCarriedItemPacket packet) {
         this.slot = packet.getSlot();
      }

      if (!ReplenishModule.getInstance().isInInventoryScreen() && AnticheatModule.getInstance().isGrim()) {
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.subPackets()) {
               if (!(packet1 instanceof ClientboundContainerSetSlotPacket)) {
                  allowedBundle.add(packet1);
               }
            }

            ((AccessorBundlePacket)packet).setIterable(allowedBundle);
         }

         if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            int slot = packet.getSlot() - 36;
            if (slot < 0 || slot > 8) {
               return;
            }

            if (packet.getItem().isEmpty()) {
               return;
            }

            for (InventoryManager.PreSwapData data : this.swapData) {
               if (data.getSlot() == slot || data.getStarting() == slot) {
                  ItemStack preStack = data.getPreHolding(slot);
                  if (!this.isEqual(preStack, packet.getItem())) {
                     event.cancel();
                     break;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onItemDesync(ItemDesyncEvent event) {
      if (this.isDesynced()) {
         event.cancel();
         event.setStack(this.getServerItem());
      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.syncToClient();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      this.swapData.removeIf(InventoryManager.PreSwapData::isPassedClearTime);
   }

   public void setSlot(int barSlot) {
      if (this.slot != barSlot && Inventory.isHotbarSlot(barSlot)) {
         this.setSlotForced(barSlot);
         ItemStack[] hotbarCopy = new ItemStack[9];

         for (int i = 0; i < 9; i++) {
            hotbarCopy[i] = mc.player.getInventory().getItem(i);
         }

         this.swapData.add(new InventoryManager.PreSwapData(hotbarCopy, this.slot, barSlot));
      }
   }

   public void setSlotAlt(int barSlot) {
      if (Inventory.isHotbarSlot(barSlot)) {
         mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, barSlot + 36, this.slot, ClickType.SWAP, mc.player);
      }
   }

   public void setClientSlot(int barSlot) {
      if (mc.player.getInventory().selected != barSlot && Inventory.isHotbarSlot(barSlot)) {
         mc.player.getInventory().selected = barSlot;
         this.setSlotForced(barSlot);
      }
   }

   public void setSlotForced(int barSlot) {
      Managers.NETWORK.sendPacket(new ServerboundSetCarriedItemPacket(barSlot));
   }

   public void syncToClient() {
      if (this.isDesynced()) {
         this.setSlotForced(mc.player.getInventory().selected);

         for (InventoryManager.PreSwapData swapData : this.swapData) {
            swapData.beginClear();
         }
      }
   }

   public boolean isDesynced() {
      return mc.player.getInventory().selected != this.slot;
   }

   public void closeScreen() {
      Managers.NETWORK.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
   }

   public int pickupSlot(int slot) {
      return this.click(slot, 0, ClickType.PICKUP);
   }

   public void quickMove(int slot) {
      this.click(slot, 0, ClickType.QUICK_MOVE);
   }

   public void throwSlot(int slot) {
      this.click(slot, 0, ClickType.THROW);
   }

   public int findEmptySlot() {
      for (int i = 9; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.isEmpty()) {
            return i;
         }
      }

      return -999;
   }

   public int click(int slot, int button, ClickType type) {
      if (slot < 0) {
         return -1;
      } else {
         AbstractContainerMenu screenHandler = mc.player.containerMenu;
         NonNullList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot1 : defaultedList) {
            list.add(slot1.getItem().copy());
         }

         screenHandler.clicked(slot, button, type, mc.player);
         Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2;
            if (!ItemStack.matches(itemStack, itemStack2 = defaultedList.get(j).getItem())) {
               int2ObjectMap.put(j, itemStack2.copy());
            }
         }

         mc.player
            .connection
            .send(
               new ServerboundContainerClickPacket(
                  screenHandler.containerId, screenHandler.getStateId(), slot, button, type, screenHandler.getCarried().copy(), int2ObjectMap
               )
            );
         return screenHandler.getStateId();
      }
   }

   public int click2(int slot, int button, ClickType type) {
      if (slot < 0) {
         return -1;
      } else {
         AbstractContainerMenu screenHandler = mc.player.containerMenu;
         NonNullList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot1 : defaultedList) {
            list.add(slot1.getItem().copy());
         }

         Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2;
            if (!ItemStack.matches(itemStack, itemStack2 = defaultedList.get(j).getItem())) {
               int2ObjectMap.put(j, itemStack2.copy());
            }
         }

         mc.player
            .connection
            .send(
               new ServerboundContainerClickPacket(
                  screenHandler.containerId, screenHandler.getStateId(), slot, button, type, screenHandler.getCarried().copy(), int2ObjectMap
               )
            );
         return screenHandler.getStateId();
      }
   }

   public int getServerSlot() {
      return this.slot;
   }

   public int getClientSlot() {
      return mc.player.getInventory().selected;
   }

   public ItemStack getServerItem() {
      return mc.player != null && this.getServerSlot() != -1 ? mc.player.getInventory().getItem(this.getServerSlot()) : null;
   }

   private boolean isEqual(ItemStack stack1, ItemStack stack2) {
      return stack1.getItem().equals(stack2.getItem()) && stack1.getHoverName().equals(stack2.getHoverName());
   }

   public static class PreSwapData {
      private final ItemStack[] preHotbar;
      private final int starting;
      private final int swapTo;
      private Timer clearTime;

      public PreSwapData(ItemStack[] preHotbar, int start, int swapTo) {
         this.preHotbar = preHotbar;
         this.starting = start;
         this.swapTo = swapTo;
      }

      public void beginClear() {
         this.clearTime = new CacheTimer();
         this.clearTime.reset();
      }

      public boolean isPassedClearTime() {
         return this.clearTime != null && this.clearTime.passed(300);
      }

      public ItemStack getPreHolding(int i) {
         return this.preHotbar[i];
      }

      public int getStarting() {
         return this.starting;
      }

      public int getSlot() {
         return this.swapTo;
      }
   }
}
