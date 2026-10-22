package net.shoreline.client.impl.module.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.SwingEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class SwingModule extends ToggleModule {
   private static SwingModule INSTANCE;
   Config<SwingModule.SwingHand> swingHandConfig = this.register(
      new EnumConfig<>("Hand", "The player swinging hand", SwingModule.SwingHand.MAINHAND, SwingModule.SwingHand.values())
   );
   private InteractionHand prevPreferredHand;
   private InteractionHand hand;

   public SwingModule() {
      super("Swing", "Modifies the swinging hand", ModuleCategory.MISCELLANEOUS);
      INSTANCE = this;
   }

   public static SwingModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevPreferredHand = mc.player.swingingArm;
         if (this.swingHandConfig.getValue() != SwingModule.SwingHand.PACKET) {
            this.updateSwingHand(this.swingHandConfig.getValue());
         }
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.swingingArm = this.prevPreferredHand;
      }
   }

   @EventListener
   public void onSwing(SwingEvent event) {
      this.updateSwingHand(this.swingHandConfig.getValue());
   }

   private void updateSwingHand(SwingModule.SwingHand swingHand) {
      this.hand = switch (swingHand) {
         case MAINHAND -> InteractionHand.MAIN_HAND;
         case OFFHAND -> InteractionHand.OFF_HAND;
         case SWAP -> mc.player.swinging && mc.player.swingTime < this.getHandSwingDuration() / 2 && mc.player.swingTime >= 0
            ? this.hand
            : (this.hand != InteractionHand.MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
         case PACKET -> {
            mc.player.swingTime = 0;
            mc.player.swinging = false;
            yield null;
         }
      };
      if (this.hand != null) {
         mc.player.swingingArm = this.hand;
      }
   }

   public int getHandSwingDuration() {
      if (MobEffectUtil.hasDigSpeed(mc.player)) {
         return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(mc.player));
      } else {
         return mc.player.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + mc.player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
      }
   }

   public InteractionHand getSwingHand() {
      return this.hand;
   }

   public InteractionHand getPrevPreferredHand() {
      return this.prevPreferredHand;
   }

   private static enum SwingHand {
      MAINHAND,
      OFFHAND,
      SWAP,
      PACKET;
   }
}
