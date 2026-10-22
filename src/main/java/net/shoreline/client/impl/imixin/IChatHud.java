package net.shoreline.client.impl.imixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;

@IMixin
public interface IChatHud {
   void addMessage(Component var1, GuiMessageTag var2, int var3);
}
