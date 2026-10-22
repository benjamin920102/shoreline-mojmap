package net.shoreline.client.impl.gui.click.impl.config;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.component.Button;
import net.shoreline.client.impl.gui.click.impl.SearchButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.BindButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.CheckboxButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.ColorButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.ConfigButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.DropdownButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.SliderButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.TextButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;

public class ModuleButton extends Button {
   private final Module module;
   private final List<ConfigButton<?>> configComponents = new CopyOnWriteArrayList<>();
   private float off;
   private boolean open;
   private final Animation settingsAnimation = new Animation(false, 200.0F, Easing.CUBIC_IN_OUT);

   public ModuleButton(Module module, CategoryFrame frame, float x, float y) {
      super(frame, x, y, 103.0F, 13.0F);
      this.module = module;

      for (Config<?> config : module.getConfigs()) {
         if (!config.getName().equalsIgnoreCase("Enabled")) {
            if (config.getValue() instanceof Boolean) {
               this.configComponents.add(new CheckboxButton(frame, this, (Config<Boolean>)config, x, y));
            } else if (config.getValue() instanceof Double) {
               this.configComponents.add(new SliderButton<>(frame, this, (Config<Number>)(Config<?>)config, x, y));
            } else if (config.getValue() instanceof Float) {
               this.configComponents.add(new SliderButton<>(frame, this, (Config<Number>)(Config<?>)config, x, y));
            } else if (config.getValue() instanceof Integer) {
               this.configComponents.add(new SliderButton<>(frame, this, (Config<Number>)(Config<?>)config, x, y));
            } else if (config.getValue() instanceof Enum) {
               this.configComponents.add(new DropdownButton(frame, this, (Config<Enum<?>>)config, x, y));
            } else if (config.getValue() instanceof String) {
               this.configComponents.add(new TextButton(frame, this, (Config<String>)config, x, y));
            } else if (config.getValue() instanceof Macro) {
               this.configComponents.add(new BindButton(frame, this, (Config<Macro>)config, x, y));
            } else if (config.getValue() instanceof Color) {
               this.configComponents.add(new ColorButton(frame, this, (Config<Color>)config, x, y));
            }
         }
      }

      this.open = false;
   }

   @Override
   public void render(GuiGraphics context, float mouseX, float mouseY, float delta) {
      this.render(context, this.x, this.y, mouseX, mouseY, delta);
   }

   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      float scaledTime = 1.0F;
      boolean fill = !(this.module instanceof ToggleModule t && !((scaledTime = (float)t.getAnimation().getFactor()) > 0.01F));
      scaledTime *= 1.7F;
      if (this.module.getName().equalsIgnoreCase("ClickGui")) {
         scaledTime = 1.7F;
      }

      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      boolean b2 = SearchButton.SEARCH_TEXT != null && !SearchButton.SEARCH_TEXT.isEmpty();
      boolean b1 = b2 && !this.getModule().getName().toLowerCase().contains(SearchButton.SEARCH_TEXT.toLowerCase());
      int hoverAlpha = (int)((b1 ? 30 : 60) * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      int hoverAlpha2 = (int)((b1 ? 25 : 50) * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      int unfilledColor = ClickGuiModule.getInstance().fixTransparency(new Color(hoverAlpha, hoverAlpha, hoverAlpha, 51 + hoverAlpha).getRGB());
      this.rect(context, fill ? ClickGuiModule.getInstance().getColor((b1 ? 50 : 100) + hoverAlpha2, scaledTime) : unfilledColor);
      int whiteText = -1;
      int grayText = -5592406;
      int colorText = scaledTime > 0.99F ? whiteText : grayText;
      if (b2) {
         colorText = b1 ? grayText : whiteText;
      }

      this.drawStringScaled(context, this.module.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 3.5F * ClickGuiModule.CLICK_GUI_SCALE, colorText);
      if (this.settingsAnimation.getFactor() > 0.01F) {
         this.off = this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + ClickGuiModule.CLICK_GUI_SCALE;
         float fheight = 0.0F;

         for (ConfigButton<?> configButton : this.configComponents) {
            if (configButton.getConfig().isVisible()) {
               fheight += configButton.getHeight();
               if (configButton instanceof ColorButton colorPicker && colorPicker.getScaledTime() > 0.01F) {
                  fheight += colorPicker.getPickerHeight() * colorPicker.getScaledTime() * this.getScaledTime();
               }
            }
         }

         boolean canScissor = ClickGuiModule.getInstance().getScaleFactor() == 1.0F;
         if (canScissor) {
            this.enableScissor(
               this.x,
               this.off - ClickGuiModule.CLICK_GUI_SCALE,
               this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               this.off + (3.0F * ClickGuiModule.CLICK_GUI_SCALE + fheight * this.settingsAnimation.getFactor())
            );
         }

         for (ConfigButton<?> configButtonx : this.configComponents) {
            if (configButtonx.getConfig().isVisible()) {
               configButtonx.render(context, ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, this.off, mouseX, mouseY, delta);
               ((CategoryFrame)this.frame).offset((float)(configButtonx.getHeight() * this.settingsAnimation.getFactor()));
               this.off = this.off + configButtonx.getHeight();
            }
         }

         if (fill) {
            this.fill(
               context,
               ix,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.CLICK_GUI_SCALE,
               this.off - (this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE) + ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.getInstance().getColor(100 + hoverAlpha2, scaledTime)
            );
            this.fill(
               context,
               ix + this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.CLICK_GUI_SCALE,
               this.off - (this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE) + ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.getInstance().getColor(100 + hoverAlpha2, scaledTime)
            );
            this.fill(
               context,
               ix,
               this.off + ClickGuiModule.CLICK_GUI_SCALE,
               this.width * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.getInstance().getColor(100 + hoverAlpha2, scaledTime)
            );
         }

         if (canScissor) {
            this.disableScissor();
         }

         ((CategoryFrame)this.frame).offset((float)(3.0F * ClickGuiModule.CLICK_GUI_SCALE * this.settingsAnimation.getFactor()));
      } else {
         for (ConfigButton<?> configButtonxx : this.configComponents) {
            if (configButtonxx instanceof BindButton bindButton) {
               bindButton.setListening(false);
            }

            if (configButtonxx instanceof TextButton textButton) {
               textButton.endTyping();
            }

            if (configButtonxx instanceof ColorButton colorButton) {
               colorButton.endTyping();
            }

            if (configButtonxx instanceof SliderButton sliderButton) {
               sliderButton.endTyping();
            }
         }
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         if (button == 0 && this.module instanceof ToggleModule t) {
            t.toggle();
         } else if (button == 1) {
            this.open = !this.open;
            this.settingsAnimation.setState(this.open);
         }

         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }

      if (this.open) {
         for (ConfigButton<?> component : this.configComponents) {
            component.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (this.open) {
         for (ConfigButton<?> component : this.configComponents) {
            component.mouseReleased(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.open) {
         for (ConfigButton<?> component : this.configComponents) {
            component.keyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   @Override
   public void charTyped(char character, int modifiers) {
      if (this.open) {
         for (ConfigButton<?> component : this.configComponents) {
            component.charTyped(character, modifiers);
         }
      }
   }

   public void offset(float in) {
      this.off += in;
   }

   public boolean isOpen() {
      return this.open;
   }

   public float getScaledTime() {
      return (float)this.settingsAnimation.getFactor();
   }

   public Module getModule() {
      return this.module;
   }

   public List<ConfigButton<?>> getConfigButtons() {
      return this.configComponents;
   }
}
