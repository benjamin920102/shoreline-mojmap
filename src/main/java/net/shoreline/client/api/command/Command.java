package net.shoreline.client.api.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;

public abstract class Command implements Globals {
   private final String name;
   private final String desc;
   private final List<LiteralArgumentBuilder<SharedSuggestionProvider>> builders = new ArrayList<>();

   public Command(String name, String desc, LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      this.name = name;
      this.desc = desc;
      this.builders.add(builder);
   }

   public Command(String name, String desc, List<LiteralArgumentBuilder<SharedSuggestionProvider>> builder) {
      this.name = name;
      this.desc = desc;
      this.builders.addAll(builder);
   }

   public abstract void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> var1);

   protected static LiteralArgumentBuilder<SharedSuggestionProvider> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   protected static List<LiteralArgumentBuilder<SharedSuggestionProvider>> literal(String... name) {
      List<LiteralArgumentBuilder<SharedSuggestionProvider>> builders = Lists.newArrayList();

      for (String s : name) {
         builders.add(LiteralArgumentBuilder.literal(s));
      }

      return builders;
   }

   protected static <T> RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   protected static SuggestionProvider<SharedSuggestionProvider> suggest(String... suggestions) {
      return (context, builder) -> SharedSuggestionProvider.suggest(Lists.newArrayList(suggestions), builder);
   }

   public List<LiteralArgumentBuilder<SharedSuggestionProvider>> getCommandBuilders() {
      return this.builders;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.desc;
   }

   public String getUsage() {
      LiteralArgumentBuilder<SharedSuggestionProvider> builder = this.builders.getFirst();
      return Managers.COMMAND.getDispatcher().getAllUsage(builder.build(), Managers.COMMAND.getSource(), false)[0];
   }
}
