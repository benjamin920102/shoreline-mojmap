package net.shoreline.client.impl.gui.click.impl.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.shoreline.client.api.config.Serializable;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.gui.click.ClickGuiScreen;
import net.shoreline.client.impl.gui.click.component.Frame;
import net.shoreline.client.impl.gui.click.impl.SearchButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.ColorButton;
import net.shoreline.client.impl.gui.click.impl.config.setting.ConfigButton;
import net.shoreline.client.impl.manager.world.sound.SoundManager;
import net.shoreline.client.impl.module.client.ClickGuiModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.client.util.string.EnumFormatter;

public class CategoryFrame extends Frame implements Serializable<CategoryFrame> {
   private final String name;
   private final ModuleCategory category;
   private final List<ModuleButton> moduleButtons = new CopyOnWriteArrayList<>();
   private float off;
   private float inner;
   private boolean open;
   private boolean drag;
   private final Animation categoryAnimation = new Animation(false, 200.0F, Easing.CUBIC_IN_OUT);
   private SearchButton searchButton;

   public CategoryFrame(ModuleCategory category, float x, float y, float width, float height) {
      super(x, y, width, height);
      this.category = category;
      this.name = EnumFormatter.formatEnum(category);

      for (Module module : Managers.MODULE.getModules()) {
         if (module.getCategory() == category) {
            this.moduleButtons.add(new ModuleButton(module, this, x, y));
         }
      }

      if (category == ModuleCategory.CLIENT) {
         this.searchButton = new SearchButton(this, x, y);
      }

      this.setOpen(true);
   }

   public CategoryFrame(ModuleCategory category, float x, float y) {
      this(category, x, y, 105.0F, 15.0F);
   }

   @Override
   public void render(GuiGraphics context, float mouseX, float mouseY, float delta) {
      this.setHeight(RenderManager.textHeight() + 6.0F);
      if (this.drag) {
         this.x = this.x + (ClickGuiScreen.MOUSE_X - this.px);
         this.y = this.y + (ClickGuiScreen.MOUSE_Y - this.py);
      }

      this.fheight = 2.0F * ClickGuiModule.CLICK_GUI_SCALE;
      if (this.searchButton != null) {
         this.fheight = this.fheight + (this.searchButton.getHeight() + 6.0F * ClickGuiModule.CLICK_GUI_SCALE);
      }

      for (ModuleButton moduleButton : this.moduleButtons) {
         this.fheight = this.fheight + (moduleButton.getHeight() + ClickGuiModule.CLICK_GUI_SCALE);
         if (!(moduleButton.getScaledTime() < 0.01F)) {
            this.fheight = this.fheight + 3.0F * ClickGuiModule.CLICK_GUI_SCALE * moduleButton.getScaledTime();

            for (ConfigButton<?> configButton : moduleButton.getConfigButtons()) {
               if (configButton.getConfig().isVisible()) {
                  this.fheight = this.fheight + configButton.getHeight() * moduleButton.getScaledTime();
                  if (configButton instanceof ColorButton colorPicker && colorPicker.getScaledTime() > 0.01F) {
                     this.fheight = this.fheight + colorPicker.getPickerHeight() * colorPicker.getScaledTime() * moduleButton.getScaledTime();
                  }
               }
            }
         }
      }

      this.rect(context, ClickGuiModule.getInstance().getColor(1.7F));
      int whiteText = -1;
      this.drawStringScaled(context, this.name, this.x + 3.0F * ClickGuiModule.CLICK_GUI_SCALE, this.y + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
      if (this.categoryAnimation.getFactor() > 0.01F) {
         boolean canScissor = ClickGuiModule.getInstance().getScaleFactor() == 1.0F;
         float y1 = this.y;
         if (this.searchButton != null) {
            y1 += this.searchButton.getHeight() + 6.0F * ClickGuiModule.CLICK_GUI_SCALE;
         }

         if (canScissor) {
            this.enableScissor(
               this.x,
               this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE,
               this.x + this.width * ClickGuiModule.CLICK_GUI_SCALE,
               this.y + ClickGuiModule.CLICK_GUI_SCALE * this.height + this.fheight * this.categoryAnimation.getFactor()
            );
         }

         int fillColor = ClickGuiModule.getInstance().fixTransparency(1996488704);
         this.fill(context, this.x, this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE, this.width * ClickGuiModule.CLICK_GUI_SCALE, this.fheight, fillColor);
         if (ClickGuiModule.getInstance().getBlur() && ClickGuiModule.getInstance().getScaleFactor() == 1.0F) {
            mc.options.menuBackgroundBlurriness().set(7);
            mc.gameRenderer.processBlurEffect(delta);
            mc.getMainRenderTarget().bindWrite(false);
            mc.options.menuBackgroundBlurriness().set(5);
         }

         if (this.searchButton != null) {
            this.searchButton
               .render(
                  context,
                  this.x + ClickGuiModule.CLICK_GUI_SCALE,
                  this.y + this.height * ClickGuiModule.CLICK_GUI_SCALE + 4.0F * ClickGuiModule.CLICK_GUI_SCALE,
                  mouseX,
                  mouseY,
                  delta
               );
         }

         this.off = y1 + this.height * ClickGuiModule.CLICK_GUI_SCALE + ClickGuiModule.CLICK_GUI_SCALE;
         this.inner = this.off;

         for (ModuleButton moduleButtonx : this.moduleButtons) {
            moduleButtonx.render(context, this.x + ClickGuiModule.CLICK_GUI_SCALE, this.inner + ClickGuiModule.CLICK_GUI_SCALE, mouseX, mouseY, delta);
            this.off = this.off + (float)((moduleButtonx.getHeight() + ClickGuiModule.CLICK_GUI_SCALE) * this.categoryAnimation.getFactor());
            this.inner = this.inner + (moduleButtonx.getHeight() + ClickGuiModule.CLICK_GUI_SCALE);
         }

         if (canScissor) {
            this.disableScissor();
         }
      }

      this.px = ClickGuiScreen.MOUSE_X;
      this.py = ClickGuiScreen.MOUSE_Y;
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 1 && this.isWithin(mouseX, mouseY)) {
         this.open = !this.open;
         this.categoryAnimation.setState(this.open);
         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }

      if (this.isOpen()) {
         for (ModuleButton button : this.moduleButtons) {
            button.mouseClicked(mouseX, mouseY, mouseButton);
         }
      }

      if (this.searchButton != null) {
         this.searchButton.mouseClicked(mouseX, mouseY, mouseButton);
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
      super.mouseReleased(mouseX, mouseY, mouseButton);
      this.drag = false;
      if (this.isOpen()) {
         for (ModuleButton button : this.moduleButtons) {
            button.mouseReleased(mouseX, mouseY, mouseButton);
         }
      }

      if (this.searchButton != null) {
         this.searchButton.mouseReleased(mouseX, mouseY, mouseButton);
      }
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      super.keyPressed(keyCode, scanCode, modifiers);
      if (this.isOpen()) {
         for (ModuleButton button : this.moduleButtons) {
            button.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      if (this.searchButton != null) {
         this.searchButton.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public void charTyped(char character, int modifiers) {
      super.charTyped(character, modifiers);
      if (this.isOpen()) {
         for (ModuleButton button : this.moduleButtons) {
            button.charTyped(character, modifiers);
         }
      }

      if (this.searchButton != null) {
         this.searchButton.charTyped(character, modifiers);
      }
   }

   @Override
   public JsonObject toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("category", this.category.name());
      obj.addProperty("x", this.getX());
      obj.addProperty("y", this.getY());
      obj.addProperty("open", this.isOpen());
      return obj;
   }

   public CategoryFrame fromJson(JsonObject jsonObj) {
      if (jsonObj.has("x") && jsonObj.has("y") && jsonObj.has("open")) {
         JsonElement xElement = jsonObj.get("x");
         JsonElement yElement = jsonObj.get("y");
         JsonElement openElement = jsonObj.get("open");
         this.setPos(xElement.getAsFloat(), yElement.getAsFloat());
         this.setOpen(openElement.getAsBoolean());
      }

      return null;
   }

   public boolean isWithinTotal(float mx, float my) {
      return this.isMouseOver(mx, my, this.x, this.y, this.width * ClickGuiModule.CLICK_GUI_SCALE, this.getTotalHeight());
   }

   public void offset(float in) {
      this.off += in;
      this.inner += in;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public boolean isOpen() {
      return this.open && this.categoryAnimation.getFactor() > 0.1F;
   }

   public void setOpen(boolean open) {
      this.open = open;
      this.categoryAnimation.setState(open);
   }

   public float getTotalHeight() {
      return this.height * ClickGuiModule.CLICK_GUI_SCALE + this.fheight;
   }

   public List<ModuleButton> getModuleButtons() {
      return this.moduleButtons;
   }

   public void setDragging(boolean drag) {
      this.drag = drag;
   }

   public boolean isDragging() {
      return this.drag;
   }

   public SearchButton getSearchButton() {
      return this.searchButton;
   }
}
