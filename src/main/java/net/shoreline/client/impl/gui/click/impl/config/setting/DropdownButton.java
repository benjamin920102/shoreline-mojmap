package net.shoreline.client.impl.gui.click.impl.config.setting;

import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.gui.click.impl.config.ModuleButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.string.EnumFormatter;

public class DropdownButton extends ConfigButton<Enum<?>> {
   private int index;

   public DropdownButton(CategoryFrame frame, ModuleButton moduleButton, Config<Enum<?>> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
   }

   @Override
   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      String val = EnumFormatter.formatEnum(this.config.getValue());
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)(80.0 * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      this.rect(context, ClickGuiModule.getInstance().getColor(100 + hoverAlpha));
      int whiteText = -1;
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
      float textLeng = RenderManager.textWidth(this.config.getName()) * ClickGuiModule.CLICK_GUI_SCALE;
      int grayText = -5592406;
      this.drawStringScaled(context, " " + val, ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE + textLeng, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, grayText);
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         Enum<?> val = this.config.getValue();
         String[] values = Arrays.stream((Enum[])val.getClass().getEnumConstants()).map(Enum::name).toArray(String[]::new);
         if (button == 0) {
            this.index = this.index + 1 > values.length - 1 ? 0 : this.index + 1;
            this.config.setValue(Enum.valueOf((Class)val.getClass(), values[this.index]));
         } else if (button == 1) {
            this.index = this.index - 1 < 0 ? values.length - 1 : this.index - 1;
            this.config.setValue(Enum.valueOf((Class)val.getClass(), values[this.index]));
         }

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
