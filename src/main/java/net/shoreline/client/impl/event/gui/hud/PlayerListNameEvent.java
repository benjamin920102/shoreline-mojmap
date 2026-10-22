package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.network.chat.Component;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

import java.util.UUID;

@Cancelable
public class PlayerListNameEvent extends Event
{
    private Component playerName;
    private final UUID id;

    public PlayerListNameEvent(Component playerName, UUID id)
    {
        this.playerName = playerName;
        this.id = id;
    }

    public void setPlayerName(Component playerName)
    {
        this.playerName = playerName;
    }

    public Component getPlayerName()
    {
        return playerName;
    }

    public UUID getId()
    {
        return id;
    }
}
