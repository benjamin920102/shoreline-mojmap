package net.shoreline.client.impl.module.misc;

import net.minecraft.client.gui.screens.DeathScreen;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.ScreenOpenEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class AutoRespawnModule extends ToggleModule
{
    //
    private boolean respawn;

    /**
     *
     */
    public AutoRespawnModule()
    {
        super("AutoRespawn", "Respawns automatically after a death",
                ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE && respawn && mc.player.isDeadOrDying())
        {
            mc.player.respawn();
            respawn = false;
        }
    }

    @EventListener
    public void onScreenOpen(ScreenOpenEvent event)
    {
        if (event.getScreen() instanceof DeathScreen)
        {
            respawn = true;
        }
    }
}
