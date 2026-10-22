package net.shoreline.client.impl.gui.click.component;

import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;

public abstract class Button extends Component implements Interactable {
   protected final Frame frame;
   protected final Animation hoverAnimation = new Animation(false, 150.0F, Easing.LINEAR);

   public Button(Frame frame, float x, float y, float width, float height) {
      this.frame = frame;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public Button(Frame frame) {
      this.frame = frame;
   }

   @Override
   public abstract void mouseClicked(double var1, double var3, int var5);

   @Override
   public abstract void mouseReleased(double var1, double var3, int var5);

   @Override
   public abstract void keyPressed(int var1, int var2, int var3);

   public Frame getFrame() {
      return this.frame;
   }
}
