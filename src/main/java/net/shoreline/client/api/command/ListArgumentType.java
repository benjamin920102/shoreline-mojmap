package net.shoreline.client.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BlockListConfig;
import net.shoreline.client.api.config.setting.EntityListConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;

public class ListArgumentType implements ArgumentType<Object> {
   public static ListArgumentType list() {
      return new ListArgumentType();
   }

   public static Object getListItem(CommandContext<?> context, String name) {
      return context.getArgument(name, Object.class);
   }

   public Object parse(StringReader reader) throws CommandSyntaxException {
      String string = reader.readString();
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", string));
      if (item != Items.AIR) {
         return item;
      } else {
         Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", string));
         if (block != Blocks.AIR) {
            return block;
         } else {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", string));
            if (!string.equalsIgnoreCase("pig") && entityType == EntityType.PIG) {
               throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, null);
            } else {
               return entityType;
            }
         }
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      Config<?> config = ConfigArgumentType.getConfig(context, "setting");
      String value = StringArgumentType.getString(context, "value");
      if (!value.equalsIgnoreCase("add") && !value.equalsIgnoreCase("remove") && !value.equalsIgnoreCase("del")) {
         return builder.buildFuture();
      } else {
         if (config instanceof ItemListConfig) {
            for (Item item : BuiltInRegistries.ITEM) {
               builder.suggest(BuiltInRegistries.ITEM.getKey(item).getPath());
            }
         }

         if (config instanceof BlockListConfig) {
            for (Block block : BuiltInRegistries.BLOCK) {
               builder.suggest(BuiltInRegistries.BLOCK.getKey(block).getPath());
            }
         }

         if (config instanceof EntityListConfig) {
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
               builder.suggest(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath());
            }
         }

         return builder.buildFuture();
      }
   }
}
