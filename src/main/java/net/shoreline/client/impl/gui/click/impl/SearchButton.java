package net.shoreline.client.impl.gui.click.impl;

import net.minecraft.client.gui.GuiGraphics;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.component.Button;
import net.shoreline.client.impl.gui.click.component.Frame;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import org.apache.commons.lang3.ArrayUtils;

public class SearchButton extends Button {
   public static String SEARCH_TEXT = null;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private final Timer idleTimer = new CacheTimer();

   public SearchButton(Frame frame, float x, float y) {
      super(frame, x, y, 103.0F, 13.0F);
      this.buffer = "".toCharArray();
   }

   @Override
   public void render(GuiGraphics context, float mouseX, float mouseY, float delta) {
      this.render(context, this.x, this.y, mouseX, mouseY, delta);
   }

   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      int unfilledColor = ClickGuiModule.getInstance().fixTransparency(855638016);
      this.fill(context, ix + 1.0F, iy, 1.0, this.getHeight(), ClickGuiModule.getInstance().getColor(1.7F));
      this.fill(context, ix + this.getWidth() - 2.0F, iy, 1.0, this.getHeight(), ClickGuiModule.getInstance().getColor(1.7F));
      this.fill(context, ix + 1.0F, iy - 1.0F, this.getWidth() - 2.0F, 1.0, ClickGuiModule.getInstance().getColor(1.7F));
      this.fill(context, ix + 1.0F, iy + this.getHeight(), this.getWidth() - 2.0F, 1.0, ClickGuiModule.getInstance().getColor(1.7F));
      this.rect(context, unfilledColor);
      int whiteText = -1;
      String renderText = this.typing ? new String(this.buffer) + this.getInsertionPoint() : "Search...";
      this.drawStringScaled(context, renderText, ix + 5.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 3.5F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         if (button == 0) {
            this.typing = !this.typing;
            if (!this.typing) {
               SEARCH_TEXT = null;
               this.buffer = "".toCharArray();
            }

            if (ClickGuiModule.getInstance().getSounds()) {
               Managers.SOUND.playSound(SoundManager.GUI_CLICK);
            }
         }
      } else {
         this.typing = false;
         SEARCH_TEXT = null;
         this.buffer = "".toCharArray();
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         switch (keyCode) {
            case 32:
            case 256:
            case 257:
               this.typing = false;
               SEARCH_TEXT = null;
               this.buffer = "".toCharArray();
               break;
            case 259:
               if (this.buffer.length != 0) {
                  this.buffer = ArrayUtils.remove(this.buffer, this.buffer.length - 1);
                  SEARCH_TEXT = new String(this.buffer);
               }
         }
      }
   }

   @Override
   public void charTyped(char character, int modifiers) {
      if (this.typing) {
         this.buffer = ArrayUtils.add(this.buffer, character);
         SEARCH_TEXT = new String(this.buffer);
      }
   }

   public boolean isTyping() {
      return this.typing;
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }
}
