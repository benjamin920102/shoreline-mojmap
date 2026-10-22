package net.shoreline.client.api.module;

import net.shoreline.client.api.Hideable;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.MacroConfig;
import net.shoreline.client.api.config.setting.ToggleConfig;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;

public class ToggleModule extends Module implements Hideable {
   private final Animation animation = new Animation(false, 300.0F, Easing.CUBIC_IN_OUT);
   Config<Boolean> enabledConfig = this.register(new ToggleConfig("Enabled", "The module enabled state", false));
   Config<Macro> keybindingConfig = this.register(
      new MacroConfig("Keybind", "The keybind to toggle the module", new Macro(this.getId(), -1, () -> this.toggle()))
   );
   Config<Boolean> hiddenConfig = this.register(new BooleanConfig("Hidden", "The hidden state of the module in the arraylist", false));
   Config<Boolean> notifyConfig = this.register(new BooleanConfig("Notify", "Notifies you when the module is toggled in chat", false, () -> false));

   public ToggleModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
      this.register(new Config[]{this.keybindingConfig, this.enabledConfig, this.hiddenConfig});
   }

   public ToggleModule(String name, String desc, ModuleCategory category, Integer keycode) {
      this(name, desc, category);
      this.keybind(keycode);
   }

   @Override
   public boolean isHidden() {
      return this.hiddenConfig.getValue();
   }

   @Override
   public void setHidden(boolean hidden) {
      this.hiddenConfig.setValue(hidden);
   }

   public void toggle() {
      if (this.isEnabled()) {
         this.disable();
      } else {
         this.enable();
      }
   }

   public void enable() {
      this.enabledConfig.setValue(true);
      this.onEnable();
      if (this.notifyConfig.getValue() && mc.level != null) {
         ChatUtil.clientSendMessage("§7" + this.getName() + "§f toggled §son", this.hashCode());
      }
   }

   public void disable() {
      this.enabledConfig.setValue(false);
      this.onDisable();
      if (this.notifyConfig.getValue() && mc.level != null) {
         ChatUtil.clientSendMessage("§7" + this.getName() + "§f toggled §coff", this.hashCode());
      }
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   public void keybind(int keycode) {
      this.keybindingConfig.setContainer(this);
      ((MacroConfig)this.keybindingConfig).setValue(keycode);
   }

   public boolean isEnabled() {
      return this.enabledConfig.getValue();
   }

   public Macro getKeybinding() {
      return this.keybindingConfig.getValue();
   }

   public void setNotify(boolean notify) {
      this.notifyConfig.setValue(notify);
   }

   public boolean getNotify() {
      return this.notifyConfig.getValue();
   }

   public Animation getAnimation() {
      return this.animation;
   }
}
