package net.shoreline.client.impl.gui.click.component;

import net.minecraft.client.gui.GuiGraphics;

public class Frame extends Component implements Interactable {
   protected float px;
   protected float py;
   protected float fheight;

   public Frame(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   @Override
   public void render(GuiGraphics context, float mouseX, float mouseY, float delta) {
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   @Override
   public void charTyped(char character, int modifiers) {
   }
}
