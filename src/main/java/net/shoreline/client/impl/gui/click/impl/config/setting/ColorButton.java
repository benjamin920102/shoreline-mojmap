package net.shoreline.client.impl.gui.click.impl.config.setting;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.gui.click.ClickGuiScreen;
import net.shoreline.client.impl.gui.click.impl.config.CategoryFrame;
import net.shoreline.client.impl.gui.click.impl.config.ModuleButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;
import org.apache.commons.lang3.ArrayUtils;

public class ColorButton extends ConfigButton<Color> {
   private boolean open;
   private final Animation pickerAnimation = new Animation(false, 200.0F, Easing.CUBIC_IN_OUT);
   private float[] selectedColor;
   private boolean ignoreSetColor;
   private char[] buffer;
   private boolean typing;
   private boolean idling;
   private final Timer idleTimer = new CacheTimer();

   public ColorButton(CategoryFrame frame, ModuleButton moduleButton, Config<Color> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      ColorConfig colorConfig = (ColorConfig)config;
      float[] hsb = colorConfig.getHsb();
      this.selectedColor = new float[]{hsb[0], hsb[1], 1.0F - hsb[2], hsb[3]};
      this.buffer = Integer.toHexString(colorConfig.getRgb()).toCharArray();
      EventBus.INSTANCE.subscribe(this);
   }

   @Override
   public void render(GuiGraphics context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      int originalColor = ((ColorConfig)this.config).getRgb();
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)(80.0 * Mth.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      this.rect(context, new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha).getRGB());
      int modifiedTransparencyColor = ClickGuiModule.getInstance().fixTransparency(originalColor);
      this.fill(
         context,
         ix + this.width * ClickGuiModule.CLICK_GUI_SCALE - 11.0F * ClickGuiModule.CLICK_GUI_SCALE,
         iy + 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
         10.0F * ClickGuiModule.CLICK_GUI_SCALE,
         10.0F * ClickGuiModule.CLICK_GUI_SCALE,
         modifiedTransparencyColor
      );
      int whiteText = -1;
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
      if (this.pickerAnimation.getFactor() > 0.01F) {
         ColorConfig colorConfig = (ColorConfig)this.config;
         if (colorConfig.isGlobal()) {
            Color global = ColorsModule.getInstance().getColor();
            float[] hsb = Color.RGBtoHSB(global.getRed(), global.getGreen(), global.getBlue(), null);
            this.selectedColor = new float[]{hsb[0], hsb[1], 1.0F - hsb[2], this.selectedColor[3]};
         }

         if (ClickGuiScreen.MOUSE_LEFT_HOLD) {
            if (this.isMouseOver(
                  mouseX,
                  mouseY,
                  this.x + ClickGuiModule.CLICK_GUI_SCALE,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE,
                  this.width * ClickGuiModule.CLICK_GUI_SCALE
               )
               && !colorConfig.isGlobal()) {
               float left = this.x + ClickGuiModule.CLICK_GUI_SCALE;
               float right = this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE;
               float top = this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE;
               float bottom = this.y
                  + this.height * ClickGuiModule.CLICK_GUI_SCALE
                  + 2.0F * ClickGuiModule.CLICK_GUI_SCALE
                  + this.width * ClickGuiModule.CLICK_GUI_SCALE;
               float width1 = right - left;
               this.selectedColor[1] = Mth.clamp((mouseX - left) / width1, 0.001F, 0.999F);
               float height1 = bottom - top;
               this.selectedColor[2] = Mth.clamp((mouseY - top) / height1, 0.001F, 0.999F);
            }

            if (this.isMouseOver(
                  mouseX,
                  mouseY,
                  this.x + ClickGuiModule.CLICK_GUI_SCALE,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 4.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
                  this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  8.0F * ClickGuiModule.CLICK_GUI_SCALE
               )
               && !colorConfig.isGlobal()) {
               this.selectedColor[0] = Mth.clamp(
                  (mouseX - (this.x + ClickGuiModule.CLICK_GUI_SCALE)) / (this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE),
                  0.001F,
                  0.999F
               );
            }

            if (colorConfig.allowAlpha()
               && this.isMouseOver(
                  mouseX,
                  mouseY,
                  this.x + ClickGuiModule.CLICK_GUI_SCALE,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 15.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
                  this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  8.0F * ClickGuiModule.CLICK_GUI_SCALE
               )) {
               this.selectedColor[3] = Mth.clamp(
                  (mouseX - (this.x + ClickGuiModule.CLICK_GUI_SCALE)) / (this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE),
                  0.001F,
                  0.999F
               );
            }

            Color color = Color.getHSBColor(this.selectedColor[0], this.selectedColor[1], 1.0F - this.selectedColor[2]);
            color = new Color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, Mth.clamp(this.selectedColor[3], 0.0F, 1.0F));
            this.ignoreSetColor = true;
            colorConfig.setValue(color);
            this.ignoreSetColor = false;
         }

         float[] hsb = this.selectedColor;
         int color = Color.HSBtoRGB(hsb[0], 1.0F, 1.0F);
         boolean canScissor = ClickGuiModule.getInstance().getScaleFactor() == 1.0F;
         if (canScissor) {
            this.enableScissor(
               this.x,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE,
               this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + this.getPickerHeight() * this.getScaledTime()
            );
         }

         for (float i = 0.0F; i < this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE; i += ClickGuiModule.CLICK_GUI_SCALE) {
            float hue = i / (this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE);
            this.fill(
               context,
               this.x + ClickGuiModule.CLICK_GUI_SCALE + i,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 4.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.CLICK_GUI_SCALE,
               8.0F * ClickGuiModule.CLICK_GUI_SCALE,
               Color.getHSBColor(hue, 1.0F, 1.0F).getRGB()
            );
         }

         this.fill(
            context,
            this.x + ClickGuiModule.CLICK_GUI_SCALE + (this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE) * hsb[0],
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 4.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
            ClickGuiModule.CLICK_GUI_SCALE,
            8.0F * ClickGuiModule.CLICK_GUI_SCALE,
            -1
         );
         this.fillGradientQuad(
            context,
            this.x + ClickGuiModule.CLICK_GUI_SCALE,
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
            this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE,
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
            -1,
            color,
            true
         );
         this.fillGradientQuad(
            context,
            this.x + ClickGuiModule.CLICK_GUI_SCALE,
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE,
            this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE - ClickGuiModule.CLICK_GUI_SCALE,
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 2.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
            0,
            -16777216,
            false
         );
         this.fill(
            context,
            this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE * hsb[1],
            this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE * hsb[2],
            2.0F * ClickGuiModule.CLICK_GUI_SCALE,
            2.0F * ClickGuiModule.CLICK_GUI_SCALE,
            -1
         );
         if (colorConfig.allowAlpha()) {
            boolean flip = false;

            for (float i = 0.0F;
               i < this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0 * ClickGuiModule.CLICK_GUI_SCALE;
               i += 4.0F * ClickGuiModule.CLICK_GUI_SCALE
            ) {
               float x1 = this.x + ClickGuiModule.CLICK_GUI_SCALE + i;
               double width1 = 4.0F * ClickGuiModule.CLICK_GUI_SCALE;
               if (x1 + width1 > this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0 * ClickGuiModule.CLICK_GUI_SCALE) {
                  width1 = this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0 * ClickGuiModule.CLICK_GUI_SCALE - i;
               }

               this.fill(
                  context,
                  x1,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 15.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
                  width1,
                  4.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  flip ? -1 : -7303024
               );
               this.fill(
                  context,
                  x1,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 19.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
                  width1,
                  4.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  flip ? -7303024 : -1
               );
               flip = !flip;
            }

            this.fillGradient(
               context,
               this.x + ClickGuiModule.CLICK_GUI_SCALE,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 15.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0 * ClickGuiModule.CLICK_GUI_SCALE,
               8.0F * ClickGuiModule.CLICK_GUI_SCALE,
               0,
               color
            );
            this.fill(
               context,
               this.x + ClickGuiModule.CLICK_GUI_SCALE + (this.width * ClickGuiModule.CLICK_GUI_SCALE - 2.0F * ClickGuiModule.CLICK_GUI_SCALE) * hsb[3],
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 15.0F * ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.CLICK_GUI_SCALE,
               8.0F * ClickGuiModule.CLICK_GUI_SCALE,
               -1
            );
         }

         String renderText = this.typing ? new String(this.buffer) + this.getInsertionPoint() : new String(this.buffer);
         this.drawStringScaled(
            context,
            "#" + renderText,
            this.x + 3.0F * ClickGuiModule.CLICK_GUI_SCALE,
            this.y
               + this.height * ClickGuiModule.CLICK_GUI_SCALE
               + (colorConfig.allowAlpha() ? 28.0F * ClickGuiModule.CLICK_GUI_SCALE : 18.0F * ClickGuiModule.CLICK_GUI_SCALE)
               + this.width * ClickGuiModule.CLICK_GUI_SCALE,
            whiteText
         );
         this.fill(
            context,
            this.x + ClickGuiModule.CLICK_GUI_SCALE,
            this.y
               + this.height * ClickGuiModule.CLICK_GUI_SCALE
               + (colorConfig.allowAlpha() ? 26.0F * ClickGuiModule.CLICK_GUI_SCALE : 14.0F * ClickGuiModule.CLICK_GUI_SCALE)
               + this.width * ClickGuiModule.CLICK_GUI_SCALE,
            this.width * ClickGuiModule.CLICK_GUI_SCALE - 17.0F * ClickGuiModule.CLICK_GUI_SCALE,
            12.0F * ClickGuiModule.CLICK_GUI_SCALE,
            814780560
         );
         double globalAnimation = this.config.getName().equalsIgnoreCase("Global") ? 1.0 : colorConfig.getAnimation().getFactor();
         if (globalAnimation > 0.01) {
            this.fill(
               context,
               this.x + ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE - 15.0F * ClickGuiModule.CLICK_GUI_SCALE,
               this.y
                  + this.height * ClickGuiModule.CLICK_GUI_SCALE
                  + (colorConfig.allowAlpha() ? 26.0F * ClickGuiModule.CLICK_GUI_SCALE : 14.0F * ClickGuiModule.CLICK_GUI_SCALE)
                  + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               13.0F * ClickGuiModule.CLICK_GUI_SCALE,
               12.0F * ClickGuiModule.CLICK_GUI_SCALE,
               ClickGuiModule.getInstance().getColor((float)globalAnimation)
            );
         }

         float x2 = this.x + ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE - 14.0F * ClickGuiModule.CLICK_GUI_SCALE;
         float y2 = this.y
            + this.height * ClickGuiModule.CLICK_GUI_SCALE
            + (colorConfig.allowAlpha() ? 27.0F * ClickGuiModule.CLICK_GUI_SCALE : 15.0F * ClickGuiModule.CLICK_GUI_SCALE)
            + this.width * ClickGuiModule.CLICK_GUI_SCALE;
         mc.getTextureManager().bindForSetup(ResourceLocation.fromNamespaceAndPath("shoreline", "icon/sync_clickgui.png"));
         RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("shoreline", "icon/sync_clickgui.png"));
         this.drawTexture(
            context,
            x2,
            y2,
            0.0F,
            0.0F,
            11.0F * ClickGuiModule.CLICK_GUI_SCALE,
            10.0F * ClickGuiModule.CLICK_GUI_SCALE,
            11.0F * ClickGuiModule.CLICK_GUI_SCALE,
            10.0F * ClickGuiModule.CLICK_GUI_SCALE
         );
         this.moduleButton.offset((float)(this.getPickerHeight() * this.pickerAnimation.getFactor()));
         ((CategoryFrame)this.frame).offset((float)(this.getPickerHeight() * this.pickerAnimation.getFactor() * this.moduleButton.getScaledTime()));
         if (canScissor) {
            this.disableScissor();
         }
      } else {
         this.endTyping();
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      ColorConfig colorConfig = (ColorConfig)this.config;
      boolean m1 = this.isMouseOver(
         mouseX,
         mouseY,
         this.x + ClickGuiModule.CLICK_GUI_SCALE,
         this.y
            + this.height * ClickGuiModule.CLICK_GUI_SCALE
            + (colorConfig.allowAlpha() ? 26.0F * ClickGuiModule.CLICK_GUI_SCALE : 14.0F * ClickGuiModule.CLICK_GUI_SCALE)
            + this.width * ClickGuiModule.CLICK_GUI_SCALE,
         this.width * ClickGuiModule.CLICK_GUI_SCALE - 17.0F * ClickGuiModule.CLICK_GUI_SCALE,
         12.0F * ClickGuiModule.CLICK_GUI_SCALE
      );
      if (!m1) {
         this.endTyping();
      }

      if (this.isWithin(mouseX, mouseY) && button == 1) {
         this.open = !this.open;
         this.pickerAnimation.setState(this.open);
         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }

      if (button == 0) {
         if (m1) {
            if (this.typing) {
               try {
                  Color color = colorConfig.parseColor(new String(this.buffer).trim());
                  this.config.setValue(color);
               } catch (NumberFormatException var10) {
               }

               this.typing = false;
            } else {
               this.typing = true;
            }

            if (ClickGuiModule.getInstance().getSounds()) {
               Managers.SOUND.playSound(SoundManager.GUI_CLICK);
            }
         }

         if (!this.config.getName().equalsIgnoreCase("Global")
            && this.isMouseOver(
               mouseX,
               mouseY,
               this.x + ClickGuiModule.CLICK_GUI_SCALE + this.width * ClickGuiModule.CLICK_GUI_SCALE - 15.0F * ClickGuiModule.CLICK_GUI_SCALE,
               this.y
                  + this.height * ClickGuiModule.CLICK_GUI_SCALE
                  + (colorConfig.allowAlpha() ? 26.0F * ClickGuiModule.CLICK_GUI_SCALE : 14.0F * ClickGuiModule.CLICK_GUI_SCALE)
                  + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               13.0F * ClickGuiModule.CLICK_GUI_SCALE,
               12.0F * ClickGuiModule.CLICK_GUI_SCALE
            )) {
            boolean val = !colorConfig.isGlobal();
            colorConfig.setGlobal(val);
            float[] hsb = ((ColorConfig)this.config).getHsb();
            this.selectedColor = new float[]{hsb[0], hsb[1], 1.0F - hsb[2], hsb[3]};
            if (ClickGuiModule.getInstance().getSounds()) {
               Managers.SOUND.playSound(SoundManager.GUI_CLICK);
            }
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getConfig() == this.getConfig() && event.getStage() == StageEvent.EventStage.POST) {
         if (!this.ignoreSetColor) {
            float[] hsb = ((ColorConfig)this.config).getHsb();
            this.selectedColor = new float[]{hsb[0], hsb[1], 1.0F - hsb[2], hsb[3]};
         }

         this.buffer = Integer.toHexString(((ColorConfig)this.config).getRgb()).toCharArray();
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
         ColorConfig colorConfig = (ColorConfig)this.config;
         switch (keyCode) {
            case 256:
               this.buffer = Integer.toHexString(colorConfig.getRgb()).toCharArray();
               this.typing = false;
               break;
            case 257:
               try {
                  Color color = colorConfig.parseColor(new String(this.buffer).trim());
                  this.config.setValue(color);
               } catch (NumberFormatException var6) {
               }

               this.buffer = Integer.toHexString(colorConfig.getRgb()).toCharArray();
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
      if (this.typing && this.buffer.length < 8 && this.isHexDigit(character)) {
         this.buffer = ArrayUtils.add(this.buffer, character);
      }
   }

   public boolean isHexDigit(char ch) {
      return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
   }

   public float getPickerHeight() {
      float pickerHeight = 29.0F * ClickGuiModule.CLICK_GUI_SCALE;
      if (((ColorConfig)this.config).allowAlpha()) {
         pickerHeight += 10.0F * ClickGuiModule.CLICK_GUI_SCALE;
      }

      return pickerHeight + this.width * ClickGuiModule.CLICK_GUI_SCALE;
   }

   public void endTyping() {
      this.typing = false;
   }

   public String getInsertionPoint() {
      if (this.idleTimer.passed(250)) {
         this.idling = !this.idling;
         this.idleTimer.reset();
      }

      return this.idling && this.typing ? "_" : "";
   }

   public boolean isTyping() {
      return this.typing;
   }

   public float getScaledTime() {
      return (float)this.pickerAnimation.getFactor();
   }
}
