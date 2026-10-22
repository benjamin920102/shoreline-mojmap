package net.shoreline.client.impl.module.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.item.TridentPullbackEvent;
import net.shoreline.client.impl.event.item.TridentWaterEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author Shoreline
 * @since 1.0
 */
public class TridentFlyModule extends ToggleModule
{

    Config<Boolean> allowNoWaterConfig = register(new BooleanConfig("AllowNoWater", "Allows you to fly using tridents even without water", true));
    Config<Boolean> instantConfig = register(new BooleanConfig("Instant", "Removes the pullback of the trident", true));
    Config<Boolean> flyConfig = register(new BooleanConfig("Spam", "Automatically uses riptide", false));
    Config<Integer> ticksConfig = register(new NumberConfig<>("Ticks", "The ticks between riptide boost", 0, 3, 20, () -> flyConfig.getValue()));

    public TridentFlyModule()
    {
        super("TridentFly", "Allows you to fly using tridents", ModuleCategory.MOVEMENT);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE || !flyConfig.getValue())
        {
            return;
        }
        if (mc.player.getMainHandItem().getItem() == Items.TRIDENT && mc.player.getTicksUsingItem() >= ticksConfig.getValue())
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
            mc.player.releaseUsingItem();
        }
    }

    @EventListener
    public void onTridentPullback(TridentPullbackEvent event)
    {
        if (instantConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onTridentWaterCheck(TridentWaterEvent event)
    {
        if (allowNoWaterConfig.getValue())
        {
            event.cancel();
        }
    }
}
