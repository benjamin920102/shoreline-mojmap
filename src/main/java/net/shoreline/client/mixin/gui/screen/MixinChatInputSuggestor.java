package net.shoreline.client.mixin.gui.screen;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import net.shoreline.client.impl.event.gui.screen.SuggestChatEvent;
import net.shoreline.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class MixinChatInputSuggestor
{

    @Shadow
    private ParseResults<SharedSuggestionProvider> currentParse;

    @Shadow
    @Final
    private EditBox input;

    @Shadow
    private boolean keepSuggestions;

    @Shadow
    @Nullable
    private CommandSuggestions.@Nullable SuggestionsList window;

    @Shadow
    private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    protected abstract void updateUsageInfo();

    @Inject(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", remap = false), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void hookRefresh(CallbackInfo ci, String string, StringReader stringReader)
    {
        SuggestChatEvent suggestChatEvent = new SuggestChatEvent();
        EventBus.INSTANCE.dispatch(suggestChatEvent);
        if (stringReader.getString().startsWith(suggestChatEvent.getPrefix(), stringReader.getCursor()))
        {
            stringReader.setCursor(stringReader.getCursor() + 1);
            if (currentParse == null)
            {
                currentParse = suggestChatEvent.getDispatcher().parse(stringReader, suggestChatEvent.getSource());
            }
            int cursor = input.getCursorPosition();
            if (cursor >= 1 && (window == null || !keepSuggestions))
            {
                pendingSuggestions = suggestChatEvent.getDispatcher().getCompletionSuggestions(currentParse, cursor);
                pendingSuggestions.thenRun(() ->
                {
                    if (pendingSuggestions.isDone())
                    {
                        updateUsageInfo();
                    }
                });
            }
            ci.cancel();
        }
    }
}
