package net.shoreline.client.impl.module.movement;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec2;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class NoAccelModule extends ToggleModule
{
    Config<Boolean> airConfig = register(new BooleanConfig("Air", "Removes acceleration while in the air", true));
    Config<Boolean> downwardsConfig = register(new BooleanConfig("Downwards", "Removes acceleration while descending", true));

    public NoAccelModule()
    {
        super("NoAccel", "Removes sprint acceleration", ModuleCategory.MOVEMENT);
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (SpeedModule.getInstance().isEnabled() || FlightModule.getInstance().isEnabled())
        {
            return;
        }
        if (!mc.player.onGround() && !airConfig.getValue() || mc.player.getDeltaMovement().y < 0.0 && !downwardsConfig.getValue() || !MovementUtil.isInputtingMovement())
        {
            return;
        }
        double speedEffect = 1.0;
        double slowEffect = 1.0;
        if (mc.player.hasEffect(MobEffects.MOVEMENT_SPEED))
        {
            double amplifier = mc.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier();
            speedEffect = 1 + (0.2 * (amplifier + 1));
        }
        if (mc.player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
        {
            double amplifier = mc.player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier();
            slowEffect = 1 + (0.2 * (amplifier + 1));
        }
        final double base = 0.2873f * speedEffect / slowEffect;
        Vec2 motion = SpeedModule.getInstance().handleVanillaMotion((float) base);
        event.cancel();
        event.setX(motion.x);
        event.setZ(motion.y);
    }
}
