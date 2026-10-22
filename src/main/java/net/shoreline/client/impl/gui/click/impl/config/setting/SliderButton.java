package net.shoreline.client.impl.gui.click.impl.config.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.ClickGuiScreen;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.gui.click.impl.config.ModuleButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import org.apache.commons.lang3.ArrayUtils;

public class SliderButton<T extends Number> extends ConfigButton<T> {
   private final int scale;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private final Timer idleTimer = new CacheTimer();

   public SliderButton(CategoryFrame frame, ModuleButton moduleButton, Config<T> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      String sval = String.valueOf(config.getValue());
      this.scale = sval.substring(sval.indexOf(".") + 1).length();
      if (config.getValue() instanceof Integer) {
         this.buffer = String.valueOf(config.getValue().intValue()).toCharArray();
      } else if (config.getValue() instanceof Float) {
         this.buffer = String.valueOf(config.getValue().floatValue()).toCharArray();
      } else if (config.getValue() instanceof Double) {
         this.buffer = String.valueOf(config.getValue().doubleValue()).toCharArray();
      }
   }

   @Override
   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)(80.0 * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      Number min = ((NumberConfig)this.config).getMin();
      Number max = ((NumberConfig)this.config).getMax();
      if (this.isWithin(mouseX, mouseY) && ClickGuiScreen.MOUSE_LEFT_HOLD) {
         float fillv = (mouseX - ix) / (this.width * ClickGuiModule.CLICK_GUI_SCALE);
         if (this.config.getValue() instanceof Integer) {
            float val = min.floatValue() + fillv * (max.intValue() - min.intValue());
            int bval = (int)Mth.clamp(val, (float)min.intValue(), (float)max.intValue());
            ((NumberConfig)this.config).setValue(bval);
         } else if (this.config.getValue() instanceof Float) {
            float val = min.floatValue() + fillv * (max.floatValue() - min.floatValue());
            float bval = Mth.clamp(val, min.floatValue(), max.floatValue());
            BigDecimal bigDecimal = new BigDecimal((double)bval);
            bval = bigDecimal.setScale(this.scale, RoundingMode.HALF_UP).floatValue();
            ((NumberConfig)this.config).setValue(bval);
         } else if (this.config.getValue() instanceof Double) {
            double val = min.doubleValue() + fillv * (max.doubleValue() - min.doubleValue());
            double bval = Mth.clamp(val, min.doubleValue(), max.doubleValue());
            BigDecimal bigDecimal = new BigDecimal(bval);
            bval = bigDecimal.setScale(this.scale, RoundingMode.HALF_UP).doubleValue();
            ((NumberConfig)this.config).setValue(bval);
         }

         float lower = Mth.ceil(this.x + ClickGuiModule.CLICK_GUI_SCALE);
         float upper = Mth.floor(this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE);
         if (mouseX < lower) {
            this.config.setValue((T)min);
         } else if (mouseX >= upper) {
            this.config.setValue((T)max);
         }
      }

      if (!this.typing) {
         if (this.config.getValue() instanceof Integer) {
            this.buffer = String.valueOf(this.config.getValue().intValue()).toCharArray();
         } else if (this.config.getValue() instanceof Float) {
            this.buffer = String.valueOf(this.config.getValue().floatValue()).toCharArray();
         } else if (this.config.getValue() instanceof Double) {
            this.buffer = String.valueOf(this.config.getValue().doubleValue()).toCharArray();
         }

         float fill = (this.config.getValue().floatValue() - min.floatValue()) / (max.floatValue() - min.floatValue());
         this.fill(
            context,
            ix,
            iy,
            fill * this.width * ClickGuiModule.CLICK_GUI_SCALE,
            this.height * ClickGuiModule.CLICK_GUI_SCALE,
            ClickGuiModule.getInstance().getColor(100 + hoverAlpha)
         );
      }

      int whiteText = -1;
      this.drawStringScaled(
         context,
         this.typing ? new String(this.buffer) + this.getInsertionPoint() : this.config.getName(),
         ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
         iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE,
         whiteText
      );
      float textLeng = RenderManager.textWidth(this.config.getName()) * ClickGuiModule.CLICK_GUI_SCALE;
      if (!this.typing) {
         int grayText = -5592406;
         this.drawStringScaled(
            context, " " + this.config.getValue(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE + textLeng, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, grayText
         );
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         if (button == 1) {
            if (this.typing) {
               String number = new String(this.buffer);

               try {
                  if (this.config.getValue() instanceof Integer) {
                     ((NumberConfig)this.config).setValue(Integer.parseInt(number));
                  } else if (this.config.getValue() instanceof Float) {
                     ((NumberConfig)this.config).setValue(Float.parseFloat(number));
                  } else if (this.config.getValue() instanceof Double) {
                     ((NumberConfig)this.config).setValue(Double.parseDouble(number));
                  }
               } catch (NumberFormatException var8) {
               }

               this.typing = false;
            } else {
               this.typing = true;
            }
         }

         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      } else {
         this.typing = false;
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
               if (this.config.getValue() instanceof Integer) {
                  this.buffer = String.valueOf(this.config.getValue().intValue()).toCharArray();
               } else if (this.config.getValue() instanceof Float) {
                  this.buffer = String.valueOf(this.config.getValue().floatValue()).toCharArray();
               } else if (this.config.getValue() instanceof Double) {
                  this.buffer = String.valueOf(this.config.getValue().doubleValue()).toCharArray();
               }

               this.typing = false;
               break;
            case 257:
               String number = new String(this.buffer);

               try {
                  if (this.config.getValue() instanceof Integer) {
                     ((NumberConfig)this.config).setValue(Integer.parseInt(number));
                  } else if (this.config.getValue() instanceof Float) {
                     ((NumberConfig)this.config).setValue(Float.parseFloat(number));
                  } else if (this.config.getValue() instanceof Double) {
                     ((NumberConfig)this.config).setValue(Double.parseDouble(number));
                  }
               } catch (NumberFormatException var6) {
               }

               if (this.config.getValue() instanceof Integer) {
                  this.buffer = String.valueOf(this.config.getValue().intValue()).toCharArray();
               } else if (this.config.getValue() instanceof Float) {
                  this.buffer = String.valueOf(this.config.getValue().floatValue()).toCharArray();
               } else if (this.config.getValue() instanceof Double) {
                  this.buffer = String.valueOf(this.config.getValue().doubleValue()).toCharArray();
               }

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
      if (this.typing && (Character.isDigit(character) || character == '.')) {
         this.buffer = ArrayUtils.add(this.buffer, character);
      }
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }

   public void endTyping() {
      this.typing = false;
   }

   public boolean isTyping() {
      return this.typing;
   }
}
