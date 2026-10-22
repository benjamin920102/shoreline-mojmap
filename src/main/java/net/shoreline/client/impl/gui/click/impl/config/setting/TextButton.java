package net.shoreline.client.impl.gui.click.impl.config.setting;

import java.awt.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.gui.click.impl.config.ModuleButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import org.apache.commons.lang3.ArrayUtils;

public class TextButton extends ConfigButton<String> {
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private final Timer idleTimer = new CacheTimer();

   public TextButton(CategoryFrame frame, ModuleButton moduleButton, Config<String> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      this.buffer = config.getValue().toCharArray();
   }

   @Override
   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      int whiteText = -1;
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)(80.0 * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      this.rect(context, new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB());
      String renderText = this.typing
         ? new String(this.buffer) + this.getInsertionPoint()
         : this.config.getName() + ChatFormatting.GRAY + " " + new String(this.buffer);
      this.drawStringScaled(context, renderText, ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         if (button == 0) {
            if (this.typing) {
               this.config.setValue(new String(this.buffer));
               this.typing = false;
            } else {
               this.typing = true;
            }

            if (ClickGuiModule.getInstance().getSounds()) {
               Managers.SOUND.playSound(SoundManager.GUI_CLICK);
            }
         }
      } else {
         this.endTyping();
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         switch (keyCode) {
            case 256:
               this.buffer = this.config.getValue().toCharArray();
               this.typing = false;
               break;
            case 257:
               this.config.setValue(new String(this.buffer));
               this.typing = false;
            case 258:
            default:
               break;
            case 259:
               if (this.buffer.length != 0) {
                  this.buffer = ArrayUtils.remove(this.buffer, this.buffer.length - 1);
               }
         }
      }
   }

   @Override
   public void charTyped(char character, int modifiers) {
      if (this.typing) {
         this.buffer = ArrayUtils.add(this.buffer, character);
      }
   }

   public void endTyping() {
      this.typing = false;
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
