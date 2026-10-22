package net.shoreline.client.impl.event.gui.screen;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.eventbus.event.Event;

public class SuggestChatEvent extends Event
{

    private CommandDispatcher dispatcher;
    private SharedSuggestionProvider source;
    private String prefix;

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public SharedSuggestionProvider getSource()
    {
        return source;
    }

    public void setSource(SharedSuggestionProvider source)
    {
        this.source = source;
    }

    public void setDispatcher(CommandDispatcher dispatcher)
    {
        this.dispatcher = dispatcher;
    }

    public CommandDispatcher getDispatcher()
    {
        return dispatcher;
    }
}
