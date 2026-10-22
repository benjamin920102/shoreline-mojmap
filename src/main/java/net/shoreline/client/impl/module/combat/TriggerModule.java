package net.shoreline.client.impl.module.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.imixin.IMinecraftClient;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorMinecraftClient;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class TriggerModule extends ToggleModule
{

    //
    Config<TriggerMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for activating the trigger bot", TriggerMode.MOUSE_BUTTON, TriggerMode.values()));
    Config<Float> attackSpeedConfig = register(new NumberConfig<>("AttackSpeed", "The speed to attack entities", 0.1f, 8.0f, 20.0f));
    Config<Float> randomSpeedConfig = register(new NumberConfig<>("RandomSpeed", "The speed randomizer for attacks", 0.1f, 2.0f, 10.0f));
    //
    private final Timer triggerTimer = new CacheTimer();

    /**
     *
     */
    public TriggerModule()
    {
        super("Trigger", "Automatically attacks entities in the crosshair",
                ModuleCategory.COMBAT);
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }
        boolean buttonDown = switch (modeConfig.getValue())
        {
            case MOUSE_BUTTON -> mc.mouseHandler.isLeftPressed();
            case MOUSE_OVER ->
            {
                if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY)
                {
                    yield false;
                }
                EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
                final Entity crosshairEntity = entityHit.getEntity();
                yield !mc.player.isAlliedTo(crosshairEntity)
                        && !Managers.SOCIAL.isFriend(crosshairEntity.getName());
            }
            case MOUSE_CLICK -> true;
        };
        double d = Math.random() * randomSpeedConfig.getValue() * 2.0 - randomSpeedConfig.getValue();
        if (buttonDown && triggerTimer.passed(1000.0 - Math.max(attackSpeedConfig.getValue() + d, 0.5) * 50.0))
        {
            ((IMinecraftClient) mc).leftClick();
            ((AccessorMinecraftClient) mc).hookSetAttackCooldown(0);
            triggerTimer.reset();
        }
    }

    public enum TriggerMode
    {
        MOUSE_BUTTON,
        MOUSE_OVER,
        MOUSE_CLICK
    }
}
