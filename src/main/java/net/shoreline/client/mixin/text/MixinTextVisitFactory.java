package net.shoreline.client.mixin.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.shoreline.client.impl.event.ClientColorEvent;
import net.shoreline.client.impl.event.text.TextVisitEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(StringDecomposer.class)
public abstract class MixinTextVisitFactory implements Globals
{

    @Shadow
    private static boolean feedChar(Style style, FormattedCharSink visitor, int index, char c)
    {
        return false;
    }

    /**
     * @param text
     * @return
     */
    @ModifyArg(method = "iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value =
            "INVOKE", target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", ordinal = 0), index = 0)
    private static String hookVisitFormatted(String text)
    {
        if (text == null)
        {
            return "";
        }
        if (mc.player == null)
        {
            return text;
        }
        final TextVisitEvent textVisitEvent = new TextVisitEvent(text);
        EventBus.INSTANCE.dispatch(textVisitEvent);
        if (textVisitEvent.isCanceled())
        {
            return textVisitEvent.getText();
        }
        return text;
    }

    @Inject(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At(value = "HEAD"), cancellable = true)
    private static void hookVisitFormatted$1(String text, int startIndex, Style startingStyle, Style resetStyle,
                                             FormattedCharSink visitor, CallbackInfoReturnable<Boolean> cir)
    {
        cir.cancel();
        int i = text.length();
        Style style = startingStyle;
        for (int j = startIndex; j < i; ++j)
        {
            char c = text.charAt(j);
            char d;
            if (c == 167)
            {
                if (j + 1 >= i)
                {
                    break;
                }

                d = text.charAt(j + 1);
                if (d == 's')
                { // Custom client color
                    ClientColorEvent clientColorEvent = new ClientColorEvent();
                    EventBus.INSTANCE.dispatch(clientColorEvent);
                    style = style.withColor(clientColorEvent.getClientRgb());
                }
                else if (d == 'g')
                { // Friend color
                    ClientColorEvent.Friend clientColorEvent = new ClientColorEvent.Friend();
                    EventBus.INSTANCE.dispatch(clientColorEvent);
                    style = style.withColor(clientColorEvent.getClientRgb());
                }
                else
                {
                    ChatFormatting formatting = ChatFormatting.getByCode(d);
                    if (formatting != null)
                    {
                        style = formatting == ChatFormatting.RESET ? resetStyle : style.applyLegacyFormat(formatting);
                    }
                }

                ++j;
            }
            else if (Character.isHighSurrogate(c))
            {
                if (j + 1 >= i)
                {
                    if (!visitor.accept(j, style, 65533))
                    {
                        cir.setReturnValue(false);
                        return;
                    }
                    break;
                }

                d = text.charAt(j + 1);
                if (Character.isLowSurrogate(d))
                {
                    if (!visitor.accept(j, style, Character.toCodePoint(c, d)))
                    {
                        cir.setReturnValue(false);
                        return;
                    }

                    ++j;
                }
                else if (!visitor.accept(j, style, 65533))
                {
                    cir.setReturnValue(false);
                    return;
                }
            }
            else if (!feedChar(style, visitor, j, c))
            {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }

}
