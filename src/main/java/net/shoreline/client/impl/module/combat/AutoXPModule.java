package net.shoreline.client.impl.module.combat;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.TickTimer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author hockeyl8
 * @since 1.0
 */
public class AutoXPModule extends RotationModule
{
    private static AutoXPModule INSTANCE;

    Config<Boolean> multiTaskConfig = register(new BooleanConfig("MultiTask", "Allows you to throw xp while using items", false));
    Config<Float> delayConfig = register(new NumberConfig<>("Delay", "Delay to throw xp in ticks", 1.0f, 1.0f, 10.0f, NumberDisplay.DEFAULT));
    Config<Integer> shiftTicksConfig = register(new NumberConfig<>("ShiftTicks", "The number of xp bottles to throw in one tick", 1, 1, 64));
    Config<Boolean> durabilityCheckConfig = register(new BooleanConfig("DurabilityCheck", "Check if your armor and held item durability is full then disables if it is", true));
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotates the player while throwing xp", false));
    Config<Boolean> swingConfig = register(new BooleanConfig("Swing", "Swings hand while throwing xp", false));

    private final TickTimer delayTimer = new TickTimer();

    public AutoXPModule()
    {
        super("AutoXP", "Automatically throws xp silently.", ModuleCategory.COMBAT, 850);
        INSTANCE = this;
    }

    public static AutoXPModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public String getModuleData()
    {
        return String.valueOf(InventoryUtil.count(Items.EXPERIENCE_BOTTLE));
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {

        if (mc.player == null || !delayTimer.passed(delayConfig.getValue()))
        {
            return;
        }

        if (mc.player.isUsingItem() && !multiTaskConfig.getValue())
        {
            return;
        }

        if (durabilityCheckConfig.getValue() && areItemsFullDura(mc.player))
        {
            disable();
            return;
        }

        int slot = -1;
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof ExperienceBottleItem)
            {
                slot = i;
                break;
            }
        }
        if (slot == -1)
        {
            disable();
            return;
        }

        Managers.INVENTORY.setSlot(slot);
        if (rotateConfig.getValue())
        {
            setRotation(mc.player.getYRot(), 90.0f);
            if (isRotationBlocked())
            {
                return;
            }
        }
        for (int i = 0; i < shiftTicksConfig.getValue(); i++)
        {
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            if (swingConfig.getValue())
            {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
        Managers.INVENTORY.syncToClient();
        delayTimer.reset();
    }

    private boolean areItemsFullDura(Player player)
    {
        if (!isItemFullDura(player.getMainHandItem()) || !isItemFullDura(player.getOffhandItem()))
        {
            return false;
        }

        for (ItemStack stack : player.getArmorSlots())
        {
            if (!isItemFullDura(stack))
            {
                return false;
            }
        }

        return true;
    }

    private boolean isItemFullDura(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return true;
        }
        int maxDura = stack.getMaxDamage();
        int currentDura = stack.getDamageValue();
        return currentDura == 0 || maxDura == 0;
    }
}
