package net.shoreline.client.impl.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.LimbAnimationEvent;
import net.shoreline.client.impl.event.entity.SwingSpeedEvent;
import net.shoreline.client.impl.event.entity.UpdateServerPositionEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.item.EatTransformationEvent;
import net.shoreline.client.impl.event.render.item.RenderSwingAnimationEvent;
import net.shoreline.client.impl.event.render.item.UpdateHeldItemsEvent;
import net.shoreline.client.mixin.accessor.AccessorBundlePacket;
import net.shoreline.eventbus.annotation.EventListener;

public final class AnimationsModule extends ToggleModule {
   Config<Boolean> noSwitchConfig = this.register(new BooleanConfig("NoSwitchAnimation", "Removes the animation when switching items", false));
   Config<Boolean> oldSwingConfig = this.register(new BooleanConfig("OldSwingAnimation", "Reverts to the 1.8 swinging animations", false));
   Config<Boolean> swingSpeedConfig = this.register(new BooleanConfig("SwingSpeed", "Allows you to modify your swing speed.", false));
   Config<Integer> swingFactorConfig = this.register(
      new NumberConfig<>("SwingFactor", "The speed of your swing.", 1, 6, 20, () -> this.swingSpeedConfig.getValue())
   );
   Config<Boolean> selfOnlyConfig = this.register(new BooleanConfig("SelfOnly", "Make the module only affect yourself", true, () -> false));
   Config<Boolean> eatTransformConfig = this.register(new BooleanConfig("EatTransform", "Transforms the first person eating animation", false));
   Config<Float> eatTransformFactorConfig = this.register(
      new NumberConfig<>("EatTransform-Factor", "Factor for the first person eating animation", 0.0F, 1.0F, 1.0F, () -> this.eatTransformConfig.getValue())
   );
   Config<Boolean> limbSwing = this.register(new BooleanConfig("NoLimbSwing", "Allows you to cancel limb swing animations", false));
   Config<Boolean> interpolationConfig = this.register(
      new BooleanConfig("NoInterpolation", "Entities will be rendered at their server positions", false, () -> this.limbSwing.getValue())
   );

   public AnimationsModule() {
      super("Animations", "Allows you to modify vanilla animations", ModuleCategory.RENDER);
   }

   @EventListener
   public void onUpdateHeldItems(UpdateHeldItemsEvent event) {
      if (this.noSwitchConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onSwingSpeed(SwingSpeedEvent event) {
      if (this.swingSpeedConfig.getValue()) {
         event.cancel();
         event.setSwingSpeed(this.swingFactorConfig.getValue());
         event.setSelfOnly(this.selfOnlyConfig.getValue());
      }
   }

   @EventListener
   public void onEatTransformation(EatTransformationEvent event) {
      if (this.eatTransformConfig.getValue()) {
         event.cancel();
         event.setFactor(this.eatTransformFactorConfig.getValue());
      }
   }

   @EventListener
   public void onLimbAnimation(LimbAnimationEvent event) {
      if (this.limbSwing.getValue()) {
         event.cancel();
         event.setSpeed(0.0F);
      }
   }

   @EventListener
   public void onUpdateServerPosition(UpdateServerPositionEvent event) {
      if (this.limbSwing.getValue() && this.interpolationConfig.getValue()) {
         event.getLivingEntity().setPosRaw(event.getX(), event.getY(), event.getZ());
         event.getLivingEntity().setYRot(event.getYaw());
         event.getLivingEntity().setXRot(event.getPitch());
      }
   }

   @EventListener
   public void onRenderSwing(RenderSwingAnimationEvent event) {
      if (this.oldSwingConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            List<Packet<?>> packets = new ArrayList<>();

            for (Packet<?> packet1 : packet.subPackets()) {
               if (!(
                  packet1 instanceof ClientboundAnimatePacket packet2
                     && this.oldSwingConfig.getValue()
                     && packet2.getId() == mc.player.getId()
                     && (packet2.getAction() == 0 || packet2.getAction() == 3)
               )) {
                  packets.add(packet1);
               }
            }

            ((AccessorBundlePacket)packet).setIterable(packets);
         } else if (event.getPacket() instanceof ClientboundAnimatePacket packet
            && this.oldSwingConfig.getValue()
            && packet.getId() == mc.player.getId()
            && (packet.getAction() == 0 || packet.getAction() == 3)) {
            event.cancel();
         }
      }
   }
}
