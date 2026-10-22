package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.util.FormattedCharSequence;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class ChatTextEvent extends Event
{

    private FormattedCharSequence text;

    public ChatTextEvent(FormattedCharSequence text)
    {
        this.text = text;
    }

    public void setText(FormattedCharSequence text)
    {
        this.text = text;
    }

    public FormattedCharSequence getText()
    {
        return text;
    }
}
