package net.shoreline.client.impl.event.option;

import net.minecraft.client.CameraType;
import net.shoreline.eventbus.event.Event;

public class PerspectiveUpdateEvent extends Event
{
    private final CameraType perspective;

    public PerspectiveUpdateEvent(CameraType perspective)
    {
        this.perspective = perspective;
    }

    public CameraType getPerspective()
    {
        return perspective;
    }
}
