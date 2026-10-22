package net.shoreline.client.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.shoreline.client.init.Managers;

public class CommandArgumentType implements ArgumentType<Command> {
   public static CommandArgumentType command() {
      return new CommandArgumentType();
   }

   public static Command getCommand(CommandContext<?> context, String name) {
      return (Command)context.getArgument(name, Command.class);
   }

   public Command parse(StringReader reader) throws CommandSyntaxException {
      String string = reader.readString();
      Command command = Managers.COMMAND.getCommand(string.toLowerCase());
      if (command == null) {
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, null);
      } else {
         return command;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      for (Command command : Managers.COMMAND.getCommands()) {
         builder.suggest(command.getName().toLowerCase());
      }

      return builder.buildFuture();
   }
}
