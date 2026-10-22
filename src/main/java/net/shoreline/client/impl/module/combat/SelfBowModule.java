package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TippedArrowItem;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author linus
 * @since 1.0
 */
public class SelfBowModule extends RotationModule
{
    private static SelfBowModule INSTANCE;

    private int pullTicks;
    private final Set<MobEffectInstance> arrows = new HashSet<>();

    public SelfBowModule()
    {
        super("SelfBow", "Shoots player with beneficial tipped arrows", ModuleCategory.COMBAT, 755);
        INSTANCE = this;
    }

    public static SelfBowModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        mc.options.keyUse.setDown(false);
        arrows.clear();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        int arrowSlot = -1;
        MobEffectInstance statusEffect = null;
        for (int i = 0; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof TippedArrowItem))
            {
                continue;
            }
            if (stack.getComponents().has(DataComponents.POTION_CONTENTS))
            {
                List<MobEffectInstance> p = (List<MobEffectInstance>) stack.getComponents()
                        .get(DataComponents.POTION_CONTENTS).getAllEffects();
                for (MobEffectInstance effect : p)
                {
                    MobEffect type = effect.getEffect().value();
                    if (type.isBeneficial() && !arrows.contains(effect))
                    {
                        arrowSlot = i;
                        statusEffect = effect;
                        break;
                    }
                }
            }

            if (arrowSlot != -1)
            {
                break;
            }
        }
        int bowSlot = -1;
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.BOW)
            {
                bowSlot = i;
                break;
            }
        }
        float pullTime = BowItem.getPowerForTime(pullTicks);
        if (mc.player.getMainHandItem().getItem() != Items.BOW || bowSlot == -1 || arrowSlot == -1)
        {
            disable();
            return;
        }
        setRotation(mc.player.getYRot(), -90.0f);
        if (arrowSlot != 9)
        {
            if (mc.player.containerMenu.getCarried().getItem() != Items.TIPPED_ARROW)
            {
                mc.gameMode.handleInventoryMouseClick(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, ClickType.PICKUP, mc.player);
            }
            if (mc.player.containerMenu.getCarried().getItem() == Items.TIPPED_ARROW)
            {
                mc.gameMode.handleInventoryMouseClick(0, 9, 0, ClickType.PICKUP, mc.player);
            }
            if (!mc.player.containerMenu.getCarried().isEmpty())
            {
                mc.gameMode.handleInventoryMouseClick(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, ClickType.PICKUP, mc.player);
            }
        }
        if (pullTime >= 0.15f)
        {
            arrows.add(statusEffect);
            mc.options.keyUse.setDown(false);
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
            mc.player.releaseUsingItem();
            pullTicks = 0;
        }
        else
        {
            mc.options.keyUse.setDown(true);
            pullTicks++;
        }
    }
}
