package net.shoreline.client.impl.module.misc;

import net.minecraft.item.*;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.init.Managers;

/**
 * @author Shoreline
 * @since 1.0
 */
public class ChestSwapModule extends ToggleModule
{

    Config<Priority> priorityConfig = register(new EnumConfig<>("Priority", "The chestplate material to prioritize", Priority.NETHERITE, Priority.values()));
    Config<Boolean> autoFireworkConfig = register(new BooleanConfig("AutoFirework", "Automatically fireworks when swapping to an elytra", false));

    public ChestSwapModule()
    {
        super("ChestSwap", "Automatically swaps chestplate", ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onEnable()
    {
        ItemStack armorStack = mc.player.getInventory().getArmor(2);
        if (armorStack.getItem() instanceof ArmorItem armorItem
                && armorItem.getEquipmentSlot() == EquipmentSlot.CHEST)
        {
            int elytraSlot = getElytraSlot();
            if (elytraSlot != -1)
            {
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                Managers.INVENTORY.pickupSlot(6);
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                if (autoFireworkConfig.getValue() && !mc.player.onGround())
                {
                    Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    mc.player.startFallFlying();
                    int slot = -1;
                    for (int i = 0; i < 45; i++)
                    {
                        ItemStack stack = mc.player.getInventory().getItem(i);
                        if (stack.getItem() == Items.FIREWORK_ROCKET)
                        {
                            slot = i;
                            break;
                        }
                    }
                    if (slot == -1)
                    {
                        return;
                    }
                    if (slot < 9)
                    {
                        Managers.INVENTORY.setSlot(slot);
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        Managers.INVENTORY.syncToClient();
                    }
                    else
                    {
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                    }
                }
            }
        }
        else
        {
            int chestplateSlot = getChestplateSlot();
            if (chestplateSlot != -1)
            {
                Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
                Managers.INVENTORY.pickupSlot(6);
                Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
            }
        }
        disable();
    }

    private int getChestplateSlot()
    {
        int slot = -1;
        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArmorItem armorItem
                    && armorItem.getEquipmentSlot() == EquipmentSlot.CHEST)
            {
                if (armorItem.getMaterial() == ArmorMaterials.NETHERITE && priorityConfig.getValue() == Priority.NETHERITE)
                {
                    slot = i;
                    break;
                }
                else if (armorItem.getMaterial() == ArmorMaterials.DIAMOND && priorityConfig.getValue() == Priority.DIAMOND)
                {
                    slot = i;
                    break;
                }
                else
                {
                    slot = i;
                }
            }
        }
        return slot;
    }

    private int getElytraSlot()
    {
        int slot = -1;
        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof ElytraItem)
            {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private enum Priority
    {
        NETHERITE,
        DIAMOND
    }
}