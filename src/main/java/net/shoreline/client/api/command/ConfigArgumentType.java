package net.shoreline.client.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.module.Module;

public class ConfigArgumentType implements ArgumentType<Config<?>> {
   private final Module module;

   private ConfigArgumentType(Module module) {
      this.module = module;
   }

   public static ConfigArgumentType config(Module module) {
      return new ConfigArgumentType(module);
   }

   public static Config<?> getConfig(CommandContext<?> context, String name) {
      return (Config<?>)context.getArgument(name, Config.class);
   }

   public Config<?> parse(StringReader reader) throws CommandSyntaxException {
      String string = reader.readString();
      Config<?> config = null;

      for (Config<?> config1 : this.module.getConfigs()) {
         if (!config1.getName().equalsIgnoreCase("Enabled")
            && !config1.getName().equalsIgnoreCase("Keybind")
            && !config1.getName().equalsIgnoreCase("Hidden")
            && !config1.getName().equalsIgnoreCase("Notify")
            && config1.getName().equalsIgnoreCase(string)) {
            config = config1;
            break;
         }
      }

      if (config == null) {
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, null);
      } else {
         return config;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      for (Config<?> config : this.module.getConfigs()) {
         if (!config.getName().equalsIgnoreCase("Enabled") && !config.getName().equalsIgnoreCase("Keybind") && !config.getName().equalsIgnoreCase("Hidden")) {
            builder.suggest(config.getName().toLowerCase());
         }
      }

      return builder.buildFuture();
   }
}
