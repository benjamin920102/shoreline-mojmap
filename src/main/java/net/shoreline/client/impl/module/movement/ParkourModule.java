package net.shoreline.client.impl.module.movement;

import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author linus
 * @since 1.0
 */
public class ParkourModule extends ToggleModule
{

    private boolean override;

    /**
     *
     */
    public ParkourModule()
    {
        super("Parkour", "Automatically jumps at the edge of blocks", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onDisable()
    {
        super.onDisable();

        if (override)
        {
            override = false;
            mc.options.keyJump.setDown(false);
        }
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        if (mc.player.onGround() && !mc.player.isShiftKeyDown()
                && mc.level.noCollision(mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate(-0.001, 0.0, -0.001)))
        {
            mc.options.keyJump.setDown(true);
            override = true;
        }
        else
        {
            if (override)
            {
                override = false;
                mc.options.keyJump.setDown(false);
            }
        }
    }
}
