package net.shoreline.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;

public class ConnectCommand extends Command
{
    public ConnectCommand()
    {
        super("Connect", "Connects you to a server", literal("connect"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder)
    {
        builder.then(argument("server", StringArgumentType.string())).executes(c ->
        {
            String serverAddress = StringArgumentType.getString(c, "server");
            if (mc.screen instanceof ConnectScreen)
            {
                ChatUtil.error("Attempt to connect while already connecting");
                return 0;
            }
            mc.disconnect();
            mc.prepareForMultiplayer();
            Managers.NETWORK.connect(ServerAddress.parseString(serverAddress),
                    new ServerData(I18n.get("selectServer.defaultName", new Object[0]), serverAddress, ServerData.Type.OTHER));
            return 1;
        });
    }
}
