package net.shoreline.client.impl.event.gui.chat;

import net.minecraft.client.gui.GuiGraphics;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @since 1.0
 */
@Cancelable
public class ChatRenderEvent extends Event
{
    //
    private final GuiGraphics context;
    private final float x, y;

    public ChatRenderEvent(GuiGraphics context, float x, float y)
    {
        this.context = context;
        this.x = x;
        this.y = y;
    }

    public GuiGraphics getContext()
    {
        return context;
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }
}
