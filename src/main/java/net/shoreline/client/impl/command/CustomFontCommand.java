package net.shoreline.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.command.Command;
import net.shoreline.client.init.Fonts;
import net.shoreline.client.util.chat.ChatUtil;

public class CustomFontCommand extends Command {
   public CustomFontCommand() {
      super("CustomFont", "Sets the client font", literal("customfont"));
   }

   @Override
   public void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
      builder.then(argument("font name", StringArgumentType.string()).executes(c -> {
         String font = StringArgumentType.getString(c, "font name");
         if (!font.equalsIgnoreCase("verdana") && !font.equalsIgnoreCase("verdana.ttf")) {
            String fileName = font.endsWith(".ttf") ? font : font + ".ttf";
            String filePath = String.format("%s/%s", Shoreline.CONFIG.getClientDirectory().toAbsolutePath(), fileName);
            if (!Files.exists(Path.of(filePath))) {
               ChatUtil.error("Could not find font file: " + fileName);
               return 0;
            } else {
               Fonts.FONT_FILE_PATH = filePath;
               Fonts.loadFonts();
               Shoreline.CONFIG.saveFonts();
               ChatUtil.clientSendMessage("Set client font to " + font + "!");
               return 0;
            }
         } else {
            Fonts.FONT_FILE_PATH = "assets/shoreline/font/verdana.ttf";
            Fonts.loadFonts();
            Shoreline.CONFIG.saveFonts();
            ChatUtil.clientSendMessage("Set client font to " + font + "!");
            return 0;
         }
      }));
   }
}
