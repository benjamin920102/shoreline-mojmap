package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.command.ModuleArgumentType;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.util.chat.ChatUtil;

public class NotifyCommand extends Command {
   public NotifyCommand() {
      super("Notify", "Notifies in chat when a module is toggled", literal("notify"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(argument("module", ModuleArgumentType.module()).executes(c -> {
         if (ModuleArgumentType.getModule(c, "module") instanceof ToggleModule t) {
            t.setNotify(!t.getNotify());
            if (t.getNotify()) {
               ChatUtil.clientSendMessage("Added §7" + t.getName() + "§f notifications to chat");
            } else {
               ChatUtil.clientSendMessage("Removed §7" + t.getName() + "§f notifications from chat");
            }

            return 1;
         } else {
            ChatUtil.error("Invalid module!");
            return 0;
         }
      }));
   }
}
