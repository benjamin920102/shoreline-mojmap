package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.util.DesktopUtil;
import net.shoreline.client.util.chat.ChatUtil;

public class OpenFolderCommand extends Command {
   public OpenFolderCommand() {
      super("OpenFolder", "Opens the client configurations folder", literal("openfolder"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.executes(c -> {
         try {
            DesktopUtil.open(Shoreline.CONFIG.getClientDirectory().toFile());
         } catch (Exception var2) {
            var2.printStackTrace();
            ChatUtil.error("Failed to open client folder!");
         }

         return 1;
      });
   }
}
