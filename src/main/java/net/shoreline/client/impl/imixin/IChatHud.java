package net.shoreline.client.impl.imixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;

@IMixin
public interface IChatHud
{

    void addMessage(Component message, GuiMessageTag indicator, int id);
}
