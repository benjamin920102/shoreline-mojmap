package net.shoreline.client.impl.module.render;

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

import java.util.ArrayList;
import java.util.List;

/**
 * @author hockeyl8
 * @since 1.0
 */
public final class AnimationsModule extends ToggleModule
{
    Config<Boolean> noSwitchConfig = register(new BooleanConfig("NoSwitchAnimation", "Removes the animation when switching items", false));
    Config<Boolean> oldSwingConfig = register(new BooleanConfig("OldSwingAnimation", "Reverts to the 1.8 swinging animations", false));
    Config<Boolean> swingSpeedConfig = register(new BooleanConfig("SwingSpeed", "Allows you to modify your swing speed.", false));
    Config<Integer> swingFactorConfig = register(new NumberConfig<>("SwingFactor", "The speed of your swing.", 1, 6, 20, () -> swingSpeedConfig.getValue()));
    Config<Boolean> selfOnlyConfig = register(new BooleanConfig("SelfOnly", "Make the module only affect yourself", true, () -> false));
    Config<Boolean> eatTransformConfig = register(new BooleanConfig("EatTransform", "Transforms the first person eating animation", false));
    Config<Float> eatTransformFactorConfig = register(new NumberConfig<>("EatTransform-Factor", "Factor for the first person eating animation", 0.0f, 1.0f, 1.0f, () -> eatTransformConfig.getValue()));
    Config<Boolean> limbSwing = register(new BooleanConfig("NoLimbSwing", "Allows you to cancel limb swing animations", false));
    Config<Boolean> interpolationConfig = register(new BooleanConfig("NoInterpolation", "Entities will be rendered at their server positions", false, () -> limbSwing.getValue()));

    public AnimationsModule()
    {
        super("Animations", "Allows you to modify vanilla animations", ModuleCategory.RENDER);
    }

    @EventListener
    public void onUpdateHeldItems(UpdateHeldItemsEvent event)
    {
        if (noSwitchConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onSwingSpeed(SwingSpeedEvent event)
    {
        if (swingSpeedConfig.getValue())
        {
            event.cancel();
            event.setSwingSpeed(swingFactorConfig.getValue());
            event.setSelfOnly(selfOnlyConfig.getValue());
        }
    }

    @EventListener
    public void onEatTransformation(EatTransformationEvent event)
    {
        if (eatTransformConfig.getValue())
        {
            event.cancel();
            event.setFactor(eatTransformFactorConfig.getValue());
        }
    }

    @EventListener
    public void onLimbAnimation(LimbAnimationEvent event)
    {
        if (limbSwing.getValue())
        {
            event.cancel();
            event.setSpeed(0.0f);
        }
    }

    @EventListener
    public void onUpdateServerPosition(UpdateServerPositionEvent event)
    {
        if (limbSwing.getValue() && interpolationConfig.getValue())
        {
            event.getLivingEntity().setPosRaw(event.getX(), event.getY(), event.getZ());
            event.getLivingEntity().setYRot(event.getYaw());
            event.getLivingEntity().setXRot(event.getPitch());
        }
    }

    @EventListener
    public void onRenderSwing(RenderSwingAnimationEvent event)
    {
        if (oldSwingConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null)
        {
            return;
        }

        if (event.getPacket() instanceof ClientboundBundlePacket packet)
        {
            List<Packet<?>> packets = new ArrayList<>();
            for (Packet<?> packet1 : packet.subPackets())
            {
                if (packet1 instanceof ClientboundAnimatePacket packet2 && oldSwingConfig.getValue()
                        && packet2.getId() == mc.player.getId()
                        && (packet2.getAction() == ClientboundAnimatePacket.SWING_MAIN_HAND || packet2.getAction() == ClientboundAnimatePacket.SWING_OFF_HAND))
                {
                    continue;
                }
                packets.add(packet1);
            }
            ((AccessorBundlePacket) packet).setIterable(packets);
        }

        else if (event.getPacket() instanceof ClientboundAnimatePacket packet && oldSwingConfig.getValue()
                && packet.getId() == mc.player.getId()
                && (packet.getAction() == ClientboundAnimatePacket.SWING_MAIN_HAND || packet.getAction() == ClientboundAnimatePacket.SWING_OFF_HAND))
        {
            event.cancel();
        }
    }
}