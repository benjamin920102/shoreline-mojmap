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

/**
 * @author linus
 * @since 1.0
 */
public class SwingModule extends ToggleModule
{
    private static SwingModule INSTANCE;

    //
    Config<SwingHand> swingHandConfig = register(new EnumConfig<>("Hand", "The player swinging hand", SwingHand.MAINHAND, SwingHand.values()));
    private InteractionHand prevPreferredHand;
    private InteractionHand hand;

    public SwingModule()
    {
        super("Swing", "Modifies the swinging hand", ModuleCategory.MISCELLANEOUS);
        INSTANCE = this;
    }

    public static SwingModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onEnable()
    {
        if (mc.player != null)
        {
            prevPreferredHand = mc.player.swingingArm;
            if (swingHandConfig.getValue() != SwingHand.PACKET)
            {
                updateSwingHand(swingHandConfig.getValue());
            }
        }
    }

    @Override
    public void onDisable()
    {
        if (mc.player != null)
        {
            mc.player.swingingArm = prevPreferredHand;
        }
    }

    @EventListener
    public void onSwing(SwingEvent event)
    {
        updateSwingHand(swingHandConfig.getValue());
    }

    private void updateSwingHand(SwingHand swingHand)
    {
        hand = switch (swingHand)
        {
            case MAINHAND -> InteractionHand.MAIN_HAND;
            case OFFHAND -> InteractionHand.OFF_HAND;
            case SWAP ->
            {
                if (!mc.player.swinging
                        || mc.player.swingTime >= getHandSwingDuration() / 2
                        || mc.player.swingTime < 0)
                {
                    yield hand != InteractionHand.MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                }
                yield hand;
            }
            case PACKET ->
            {
                mc.player.swingTime = 0;
                mc.player.swinging = false;
                yield null;
            }
        };
        if (hand != null)
        {
            mc.player.swingingArm = hand;
        }
    }

    public int getHandSwingDuration()
    {
        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(mc.player));
        }
        return mc.player.hasEffect(MobEffects.DIG_SLOWDOWN) ?
                6 + (1 + mc.player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
    }

    public InteractionHand getSwingHand()
    {
        return hand;
    }

    public InteractionHand getPrevPreferredHand()
    {
        return prevPreferredHand;
    }

    private enum SwingHand
    {
        MAINHAND,
        OFFHAND,
        SWAP,
        PACKET
    }
}