package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.api.command.Command;

public class PeekCommand extends Command {

    public PeekCommand()
    {
        super("Peek", "Shows the contents of held shulkers", literal("peek"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder)
    {

    }

    public void displayShulkerInventory(ItemStack stack)
    {

    }
}
