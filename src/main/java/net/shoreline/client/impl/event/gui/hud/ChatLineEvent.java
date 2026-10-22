package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.GuiMessage;
import net.shoreline.eventbus.event.Event;

public class ChatLineEvent extends Event
{

    private final GuiMessage.Line chatHudLine;
    private final double width;

    public ChatLineEvent(GuiMessage.Line chatHudLine, double width)
    {
        this.chatHudLine = chatHudLine;
        this.width = width;
    }

    public GuiMessage.Line getChatHudLine()
    {
        return chatHudLine;
    }

    public double getWidth()
    {
        return width;
    }
}
