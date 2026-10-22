package net.shoreline.client.util;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.SocialsModule;

public class FormattingUtil {
   private static final Map<Integer, ChatFormatting> COLOR_TO_FORMATTING = Stream.of(ChatFormatting.values())
      .filter(ChatFormatting::isColor)
      .collect(ImmutableMap.toImmutableMap(ChatFormatting::getColor, Function.identity()));

   public static String toString(Component text) {
      StringBuilder builder = new StringBuilder();
      text.visit((styleOverride, message) -> {
         if (!message.isEmpty()) {
            if (styleOverride.getColor() != null) {
               int rgb = styleOverride.getColor().getValue();
               if (rgb == (ColorsModule.getInstance().getRGB() & 16777215)) {
                  builder.append('§').append("s");
               } else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 16777215)) {
                  builder.append('§').append("g");
               } else {
                  ChatFormatting formatting = COLOR_TO_FORMATTING.get(rgb);
                  if (formatting != null) {
                     builder.append('§').append(formatting.getChar());
                  }
               }
            } else if (styleOverride.isObfuscated()) {
               builder.append('§').append("k");
            } else if (styleOverride.isBold()) {
               builder.append('§').append("l");
            } else if (styleOverride.isStrikethrough()) {
               builder.append('§').append("m");
            } else if (styleOverride.isUnderlined()) {
               builder.append('§').append("n");
            } else if (styleOverride.isItalic()) {
               builder.append('§').append("o");
            } else {
               builder.append('§').append("r");
            }

            builder.append(message);
         }

         return Optional.empty();
      }, Style.EMPTY);
      return builder.toString();
   }

   public static String toString(FormattedCharSequence text) {
      StringBuilder builder = new StringBuilder();
      text.accept((index, style, codePoint) -> {
         if (style.getColor() != null) {
            int rgb = style.getColor().getValue();
            if (rgb == (ColorsModule.getInstance().getRGB() & 16777215)) {
               builder.append('§').append("s");
            } else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 16777215)) {
               builder.append('§').append("g");
            } else {
               ChatFormatting formatting = COLOR_TO_FORMATTING.get(rgb);
               if (formatting != null) {
                  builder.append('§').append(formatting.getChar());
               }
            }
         } else if (style.isObfuscated()) {
            builder.append('§').append("k");
         } else if (style.isBold()) {
            builder.append('§').append("l");
         } else if (style.isStrikethrough()) {
            builder.append('§').append("m");
         } else if (style.isUnderlined()) {
            builder.append('§').append("n");
         } else if (style.isItalic()) {
            builder.append('§').append("o");
         } else {
            builder.append('§').append("r");
         }

         builder.appendCodePoint(codePoint);
         return true;
      });
      return builder.toString();
   }
}
