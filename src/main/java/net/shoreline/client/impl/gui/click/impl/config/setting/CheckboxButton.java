package net.shoreline.client.impl.gui.click.impl.config.setting;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.gui.click.impl.config.ModuleButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;

public class CheckboxButton extends ConfigButton<Boolean> {
   public CheckboxButton(CategoryFrame frame, ModuleButton moduleButton, Config<Boolean> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      config.getAnimation().setState(config.getValue());
   }

   @Override
   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      Animation checkboxAnimation = this.config.getAnimation();
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)(80.0 * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      this.rect(
         context,
         checkboxAnimation.getFactor() > 0.01F
            ? ClickGuiModule.getInstance().getColor(100 + hoverAlpha, (float)checkboxAnimation.getFactor())
            : new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB()
      );
      int whiteText = -1;
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY) && button == 0) {
         boolean val = this.config.getValue();
         this.config.setValue(!val);
         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }
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
