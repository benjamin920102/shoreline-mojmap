package net.shoreline.client.impl.manager.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author xgraza & linus
 * @since 1.0
 */
public class InventoryManager implements Globals
{
    private final List<PreSwapData> swapData = new CopyOnWriteArrayList<>();

    // The serverside selected hotbar slot.
    private int slot;

    /**
     *
     */
    public InventoryManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    @EventListener
    public void onPacketOutBound(final PacketEvent.Outbound event)
    {
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet)
        {
            final int packetSlot = packet.getSlot();
            if (!Inventory.isHotbarSlot(packetSlot) || slot == packetSlot)
            {
                event.setCanceled(true);
                return;
            }
            slot = packetSlot;
        }
    }

    @EventListener
    public void onPacketInbound(final PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof ClientboundSetCarriedItemPacket packet)
        {
            slot = packet.getSlot();
        }
        
        if (ReplenishModule.getInstance().isInInventoryScreen() || !AnticheatModule.getInstance().isGrim())
        {
            return;
        }

        // retarded packets from grim we can ignore
        if (event.getPacket() instanceof ClientboundBundlePacket packet)
        {
            List<Packet<?>> allowedBundle = new ArrayList<>();
            for (Packet<?> packet1 : packet.subPackets())
            {
                if (packet1 instanceof ClientboundContainerSetSlotPacket)
                {
                    continue;
                }
                allowedBundle.add(packet1);
            }
            ((AccessorBundlePacket) packet).setIterable(allowedBundle);
        }

        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet)
        {
            int slot = packet.getSlot() - 36;
            if (slot < 0 || slot > 8)
            {
                return;
            }

            if (packet.getItem().isEmpty())
            {
                return;
            }

            for (PreSwapData data : swapData)
            {
                if (data.getSlot() != slot && data.getStarting() != slot)
                {
                    continue;
                }

                ItemStack preStack = data.getPreHolding(slot);
                if (!isEqual(preStack, packet.getItem()))
                {
                    event.cancel();
                    break;
                }
            }
        }
    }

    @EventListener
    public void onItemDesync(ItemDesyncEvent event)
    {
        if (isDesynced())
        {
            event.cancel();
            event.setStack(getServerItem());
        }
    }

    @EventListener
    public void onDeath(EntityDeathEvent event)
    {
        if (event.getEntity() == mc.player)
        {
            syncToClient();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        swapData.removeIf(PreSwapData::isPassedClearTime);
    }

    /**
     * Sets the server slot via a {@link ServerboundSetCarriedItemPacket}
     *
     * @param barSlot the player hotbar slot 0-8
     * @apiNote Method will not do anything if the slot provided is already the server slot
     * @see InventoryManager#setSlotForced(int)
     */
    public void setSlot(final int barSlot)
    {
        if (slot != barSlot && Inventory.isHotbarSlot(barSlot))
        {
            setSlotForced(barSlot);

            final ItemStack[] hotbarCopy = new ItemStack[9];
            for (int i = 0; i < 9; i++)
            {
                hotbarCopy[i] = mc.player.getInventory().getItem(i);
            }
            swapData.add(new PreSwapData(hotbarCopy, slot, barSlot));
        }
    }

    /**
     * Sets the server slot via a click slot
     *
     * @param barSlot the player hotbar slot 0-8
     */
    public void setSlotAlt(final int barSlot)
    {
        if (Inventory.isHotbarSlot(barSlot))
        {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId,
                    barSlot + 36, slot, ClickType.SWAP, mc.player);
        }
    }

    /**
     * Sets the server & client slot
     *
     * @param barSlot the player hotbar slot 0-8
     * @apiNote Method will not do anything if the slot provided is already the server slot
     * @see InventoryManager#setSlotForced(int)
     * @see InventoryManager#setSlot(int)
     */
    public void setClientSlot(final int barSlot)
    {
        if (mc.player.getInventory().selected != barSlot
                && Inventory.isHotbarSlot(barSlot))
        {
            mc.player.getInventory().selected = barSlot;
            setSlotForced(barSlot);
        }
    }

    /**
     * Sends a {@link ServerboundSetCarriedItemPacket} without any slot checks
     *
     * @param barSlot the player hotbar slot 0-8
     */
    public void setSlotForced(final int barSlot)
    {
        Managers.NETWORK.sendPacket(new ServerboundSetCarriedItemPacket(barSlot));
    }

    /**
     * Syncs the server slot to the client slot
     */
    public void syncToClient()
    {
        if (isDesynced())
        {
            setSlotForced(mc.player.getInventory().selected);

            for (PreSwapData swapData : swapData)
            {
                swapData.beginClear();
            }
        }
    }

    public boolean isDesynced()
    {
        return mc.player.getInventory().selected != slot;
    }

    //
    public void closeScreen()
    {
        Managers.NETWORK.sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
    }

    /**
     * @param slot
     */
    public int pickupSlot(final int slot)
    {
        return click(slot, 0, ClickType.PICKUP);
    }

    public void quickMove(final int slot)
    {
        click(slot, 0, ClickType.QUICK_MOVE);
    }

    /**
     * @param slot
     */
    public void throwSlot(final int slot)
    {
        click(slot, 0, ClickType.THROW);
    }

    public int findEmptySlot()
    {
        for (int i = 9; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty())
            {
                return i;
            }
        }
        return -999; // throw
    }

    /**
     * @param slot
     * @param button
     * @param type
     */
    public int click(int slot, int button, ClickType type)
    {
        if (slot < 0)
        {
            return -1;
        }
        AbstractContainerMenu screenHandler = mc.player.containerMenu;
        NonNullList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : defaultedList)
        {
            list.add(slot1.getItem().copy());
        }
        screenHandler.clicked(slot, button, type, mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j)
        {
            ItemStack itemStack2;
            ItemStack itemStack = list.get(j);
            if (ItemStack.matches(itemStack, itemStack2 = defaultedList.get(j).getItem())) continue;
            int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.connection.send(new ServerboundContainerClickPacket(screenHandler.containerId, screenHandler.getStateId(), slot, button, type, screenHandler.getCarried().copy(), int2ObjectMap));
        return screenHandler.getStateId();
    }

    public int click2(int slot, int button, ClickType type)
    {
        if (slot < 0)
        {
            return -1;
        }
        AbstractContainerMenu screenHandler = mc.player.containerMenu;
        NonNullList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : defaultedList)
        {
            list.add(slot1.getItem().copy());
        }
        // screenHandler.onSlotClick(slot, button, type, mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j)
        {
            ItemStack itemStack2;
            ItemStack itemStack = list.get(j);
            if (ItemStack.matches(itemStack, itemStack2 = defaultedList.get(j).getItem())) continue;
            int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.connection.send(new ServerboundContainerClickPacket(screenHandler.containerId, screenHandler.getStateId(), slot, button, type, screenHandler.getCarried().copy(), int2ObjectMap));
        return screenHandler.getStateId();
    }

    /**
     * @return
     */
    public int getServerSlot()
    {
        return slot;
    }

    public int getClientSlot()
    {
        return mc.player.getInventory().selected;
    }

    /**
     * @return
     */
    public ItemStack getServerItem()
    {
        if (mc.player != null && getServerSlot() != -1)
        {
            return mc.player.getInventory().getItem(getServerSlot());
        }
        return null;
    }

    private boolean isEqual(ItemStack stack1, ItemStack stack2)
    {
        return stack1.getItem().equals(stack2.getItem()) && stack1.getHoverName().equals(stack2.getHoverName());
    }

    public static class PreSwapData
    {
        private final ItemStack[] preHotbar;

        private final int starting;
        private final int swapTo;

        private Timer clearTime;

        public PreSwapData(ItemStack[] preHotbar, int start, int swapTo)
        {
            this.preHotbar = preHotbar;
            this.starting = start;
            this.swapTo = swapTo;
        }

        public void beginClear()
        {
            clearTime = new CacheTimer();
            clearTime.reset();
        }

        public boolean isPassedClearTime()
        {
            return clearTime != null && clearTime.passed(300);
        }

        public ItemStack getPreHolding(int i)
        {
            return preHotbar[i];
        }

        public int getStarting()
        {
            return starting;
        }

        public int getSlot()
        {
            return swapTo;
        }
    }
}