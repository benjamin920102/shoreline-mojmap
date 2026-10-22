package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.GuiMessage;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderChatHudEvent extends Event
{
    private final GuiMessage.Line chatHudLine;
    private double animation;
    private boolean animationMode;

    public RenderChatHudEvent(GuiMessage.Line chatHudLine)
    {
        this.chatHudLine = chatHudLine;
    }

    public double getAnimation()
    {
        return animation;
    }

    public void setAnimation(double animation)
    {
        this.animation = animation;
    }

    public void setSlide(boolean animationMode)
    {
        this.animationMode = animationMode;
    }

    public GuiMessage.Line getChatHudLine()
    {
        return chatHudLine;
    }

    public boolean getAnimationMode()
    {
        return animationMode;
    }
}
