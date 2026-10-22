package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.command.PlayerArgumentType;
import net.shoreline.client.util.chat.ChatUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinGrabCommand extends Command
{
    public SkinGrabCommand()
    {
        super("SkinGrab", "Downloads player skins", literal("skingrab"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder)
    {
        builder.then(argument("player", PlayerArgumentType.player()).executes(c ->
        {
            String player = PlayerArgumentType.getPlayer(c, "player");
            String skinTexture = null;
            for (PlayerInfo playerListEntry : mc.player.connection.getOnlinePlayers())
            {
                String playerName = playerListEntry.getProfile().getName();
                if (player.equals(playerName))
                {
                    skinTexture = playerListEntry.getSkin().textureUrl();
                    break;
                }
            }
            if (skinTexture == null)
            {
                ChatUtil.error("Failed to find skin texture for " + player);
                return 0;
            }
            try
            {
                URL url = new URL(skinTexture);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(Shoreline.CONFIG.getClientDirectory().toString() + "/" + player + ".png");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                {
                    return 0;
                }
                outputStream.write(inputStream.readAllBytes());
                ChatUtil.clientSendMessage(player + " skin downloaded to client folder");
            }
            catch (IOException e)
            {
                ChatUtil.error("Failed to download skin texture for " + player);
            }

            return 1;
        }));
    }
}
