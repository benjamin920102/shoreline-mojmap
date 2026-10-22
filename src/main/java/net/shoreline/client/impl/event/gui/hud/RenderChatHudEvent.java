package net.shoreline.client.impl.event.gui.hud;

import net.minecraft.client.GuiMessage.Line;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderChatHudEvent extends Event {
   private final Line chatHudLine;
   private double animation;
   private boolean animationMode;

   public RenderChatHudEvent(Line chatHudLine) {
      this.chatHudLine = chatHudLine;
   }

   public double getAnimation() {
      return this.animation;
   }

   public void setAnimation(double animation) {
      this.animation = animation;
   }

   public void setSlide(boolean animationMode) {
      this.animationMode = animationMode;
   }

   public Line getChatHudLine() {
      return this.chatHudLine;
   }

   public boolean getAnimationMode() {
      return this.animationMode;
   }
}
