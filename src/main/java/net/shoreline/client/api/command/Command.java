package net.shoreline.client.api.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linus
 * @since 1.0
 */
public abstract class Command implements Globals
{
    //
    private final String name;
    private final String desc;
    private final List<LiteralArgumentBuilder<SharedSuggestionProvider>> builders = new ArrayList<>();

    /**
     * @param name
     * @param desc
     * @param builder
     */
    public Command(String name, String desc, LiteralArgumentBuilder<SharedSuggestionProvider> builder)
    {
        this.name = name;
        this.desc = desc;
        builders.add(builder);
    }

    public Command(String name, String desc, List<LiteralArgumentBuilder<SharedSuggestionProvider>> builder)
    {
        this.name = name;
        this.desc = desc;
        builders.addAll(builder);
    }

    public abstract void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder);

    protected static LiteralArgumentBuilder<SharedSuggestionProvider> literal(String name)
    {
        return LiteralArgumentBuilder.literal(name);
    }

    protected static List<LiteralArgumentBuilder<SharedSuggestionProvider>> literal(String... name)
    {
        List<LiteralArgumentBuilder<SharedSuggestionProvider>> builders = Lists.newArrayList();
        for (String s : name)
        {
            builders.add(LiteralArgumentBuilder.literal(s));
        }
        return builders;
    }

    protected static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(String name, ArgumentType<T> type)
    {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static SuggestionProvider<SharedSuggestionProvider> suggest(String... suggestions)
    {
        return (context, builder) -> SharedSuggestionProvider.suggest(Lists.newArrayList(suggestions), builder);
    }

    /**
     * @return
     */
    public List<LiteralArgumentBuilder<SharedSuggestionProvider>> getCommandBuilders()
    {
        return builders;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return desc;
    }

    /**
     * Returns the unique command identifier, used to identify the command in
     * the chat. Ex: help, prefix, openfolder, etc.
     *
     * @return
     */
    public String getUsage()
    {
        LiteralArgumentBuilder<SharedSuggestionProvider> builder = builders.getFirst();
        return Managers.COMMAND.getDispatcher().getAllUsage(builder.build(), Managers.COMMAND.getSource(), false)[0];
    }
}
