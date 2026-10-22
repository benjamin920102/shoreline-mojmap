package net.shoreline.client.mixin.accessor;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ChatScreen.class})
public interface AccessorChatScreen {
   @Accessor("input")
   EditBox getChatField();
}
