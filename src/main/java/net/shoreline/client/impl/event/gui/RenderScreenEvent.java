package net.shoreline.client.impl.event.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.event.Event;

public class RenderScreenEvent extends Event
{
    public final PoseStack matrixStack;

    public RenderScreenEvent(PoseStack matrixStack)
    {
        this.matrixStack = matrixStack;
    }
}
