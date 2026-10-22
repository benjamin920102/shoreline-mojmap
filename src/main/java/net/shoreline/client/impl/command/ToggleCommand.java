package net.shoreline.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.api.command.ModuleArgumentType;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.util.chat.ChatUtil;

public class ToggleCommand extends Command {
   public ToggleCommand() {
      super("Toggle", "Enables/Disables a module", literal("toggle"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      ((LiteralArgumentBuilder)builder.then(argument("module", ModuleArgumentType.module()).executes(c -> {
         if (ModuleArgumentType.getModule(c, "module") instanceof ToggleModule t) {
            t.toggle();
            ChatUtil.clientSendMessage("%s is now %s", "§7" + t.getName() + "§f", t.isEnabled() ? "§senabled§f" : "§cdisabled§f");
         }

         return 1;
      }))).executes(c -> {
         ChatUtil.error("Must provide module to toggle!");
         return 1;
      });
   }
}
