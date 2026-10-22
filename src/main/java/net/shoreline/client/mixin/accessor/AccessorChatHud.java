package net.shoreline.client.mixin.accessor;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ChatComponent.class})
public interface AccessorChatHud {
   @Invoker("addMessage")
   void hookAddMessage(Component var1, MessageSignature var2, GuiMessageTag var3);
}
