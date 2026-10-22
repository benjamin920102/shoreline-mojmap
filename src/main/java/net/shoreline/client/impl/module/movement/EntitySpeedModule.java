package net.shoreline.client.impl.module.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.text.DecimalFormat;

/**
 * @author linus
 * @since 1.0
 */
public class EntitySpeedModule extends ToggleModule
{

    //
    Config<Float> speedConfig = register(new NumberConfig<>("Speed", "The speed of the entity while moving", 0.1f, 0.5f, 4.0f));
    Config<Boolean> antiStuckConfig = register(new BooleanConfig("AntiStuck", "Prevents entities from getting stuck when moving up", false));
    Config<Boolean> strictConfig = register(new BooleanConfig("Strict", "The NCP-Updated bypass for speeding up entity movement", false));
    //
    private final Timer entityJumpTimer = new CacheTimer();

    /**
     *
     */
    public EntitySpeedModule()
    {
        super("EntitySpeed", "Increases riding entity speeds", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getModuleData()
    {
        DecimalFormat decimal = new DecimalFormat("0.0");
        return decimal.format(speedConfig.getValue());
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }
        if (mc.player.isHandsBusy() && mc.player.getControlledVehicle() != null)
        {
            double d = Math.cos(Math.toRadians(mc.player.getYRot() + 90.0f));
            double d2 = Math.sin(Math.toRadians(mc.player.getYRot() + 90.0f));
            BlockPos pos1 = BlockPos.containing(mc.player.getX() + (2.0 * d),
                    mc.player.getY() - 1.0, mc.player.getZ() + (2.0 * d2));
            BlockPos pos2 = BlockPos.containing(mc.player.getX() + (2.0 * d),
                    mc.player.getY() - 2.0, mc.player.getZ() + (2.0 * d2));
            if (antiStuckConfig.getValue() && !mc.player.getControlledVehicle().onGround()
                    && !mc.level.getBlockState(pos1).blocksMotion()
                    && !mc.level.getBlockState(pos2).blocksMotion())
            {
                entityJumpTimer.reset();
                return;
            }
            BlockPos pos3 = BlockPos.containing(mc.player.getX() + (2.0 * d),
                    mc.player.getY(), mc.player.getZ() + (2.0 * d2));
            if (antiStuckConfig.getValue() && mc.level.getBlockState(pos3).blocksMotion())
            {
                entityJumpTimer.reset();
                return;
            }
            BlockPos pos4 = BlockPos.containing(mc.player.getX() + d,
                    mc.player.getY() + 1.0, mc.player.getZ() + d2);
            if (antiStuckConfig.getValue() && mc.level.getBlockState(pos4).blocksMotion())
            {
                entityJumpTimer.reset();
                return;
            }
            if (mc.player.input.jumping)
            {
                entityJumpTimer.reset();
            }
            if (entityJumpTimer.passed(10000) || !antiStuckConfig.getValue())
            {
                if (!mc.player.getControlledVehicle().isInWater() || mc.player.input.jumping
                        || !entityJumpTimer.passed(1000))
                {
                    if (mc.player.getControlledVehicle().onGround())
                    {
                        mc.player.getControlledVehicle().setDeltaMovement(mc.player.getDeltaMovement().x,
                                0.4, mc.player.getDeltaMovement().z);
                    }
                    mc.player.getControlledVehicle().setDeltaMovement(mc.player.getDeltaMovement().x,
                            -0.4, mc.player.getDeltaMovement().z);
                }
                if (strictConfig.getValue())
                {
                    Managers.NETWORK.sendPacket(ServerboundInteractPacket.createInteractionPacket(
                            mc.player.getControlledVehicle(), false, InteractionHand.MAIN_HAND));
                }
                handleEntityMotion(speedConfig.getValue(), d, d2);
                entityJumpTimer.reset();
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || !mc.player.isHandsBusy() || mc.options.keyShift.isDown()
                || mc.player.getControlledVehicle() == null)
        {
            return;
        }
        if (strictConfig.getValue())
        {
            if (event.getPacket() instanceof ClientboundSetPassengersPacket)
            {
                event.cancel();
            }
            else if (event.getPacket() instanceof ClientboundPlayerPositionPacket)
            {
                event.cancel();
            }
        }
    }

    private void handleEntityMotion(float entitySpeed, double d, double d2)
    {
        Vec3 motion = mc.player.getControlledVehicle().getDeltaMovement();
        //
        float forward = mc.player.input.forwardImpulse;
        float strafe = mc.player.input.leftImpulse;
        if (forward == 0 && strafe == 0)
        {
            mc.player.getControlledVehicle().setDeltaMovement(0.0, motion.y, 0.0);
            return;
        }
        mc.player.getControlledVehicle().setDeltaMovement((forward * entitySpeed * d) + (strafe * entitySpeed * d2),
                motion.y, (forward * entitySpeed * d2) - (strafe * entitySpeed * d));
    }
}
