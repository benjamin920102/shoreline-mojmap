package net.shoreline.client.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.SocialsModule;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class FormattingUtil
{
    private static final Map<Integer, ChatFormatting> COLOR_TO_FORMATTING = Stream.of(ChatFormatting.values()).filter(ChatFormatting::isColor)
            .collect(ImmutableMap.toImmutableMap(ChatFormatting::getColor, Function.identity()));

    // Fuck minecraft
    public static String toString(Component text)
    {
        StringBuilder builder = new StringBuilder();
        text.visit((styleOverride, message) ->
        {
            if (!message.isEmpty())
            {
                if (styleOverride.getColor() != null)
                {
                    int rgb = styleOverride.getColor().getValue();
                    if (rgb == (ColorsModule.getInstance().getRGB() & 0xFFFFFF))
                    {
                        builder.append(ChatFormatting.PREFIX_CODE).append("s");
                    }
                    else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 0xFFFFFF))
                    {
                        builder.append(ChatFormatting.PREFIX_CODE).append("g");
                    }
                    else
                    {
                        ChatFormatting formatting = COLOR_TO_FORMATTING.get(rgb);
                        if (formatting != null)
                        {
                            builder.append(ChatFormatting.PREFIX_CODE).append(formatting.getChar());
                        }
                    }
                }
                else if (styleOverride.isObfuscated())
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("k");
                }
                else if (styleOverride.isBold())
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("l");
                }
                else if (styleOverride.isStrikethrough())
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("m");
                }
                else if (styleOverride.isUnderlined())
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("n");
                }
                else if (styleOverride.isItalic())
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("o");
                }
                else
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("r");
                }
                builder.append(message);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    public static String toString(FormattedCharSequence text)
    {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) ->
        {
            if (style.getColor() != null)
            {
                int rgb = style.getColor().getValue();
                if (rgb == (ColorsModule.getInstance().getRGB() & 0xFFFFFF))
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("s");
                }
                else if (rgb == (SocialsModule.getInstance().getFriendRGB() & 0xFFFFFF))
                {
                    builder.append(ChatFormatting.PREFIX_CODE).append("g");
                }
                else
                {
                    ChatFormatting formatting = COLOR_TO_FORMATTING.get(rgb);
                    if (formatting != null)
                    {
                        builder.append(ChatFormatting.PREFIX_CODE).append(formatting.getChar());
                    }
                }
            }
            else if (style.isObfuscated())
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("k");
            }
            else if (style.isBold())
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("l");
            }
            else if (style.isStrikethrough())
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("m");
            }
            else if (style.isUnderlined())
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("n");
            }
            else if (style.isItalic())
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("o");
            }
            else
            {
                builder.append(ChatFormatting.PREFIX_CODE).append("r");
            }
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }
}
