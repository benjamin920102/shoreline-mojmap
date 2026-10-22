package net.shoreline.client.impl.module.movement;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.impl.module.misc.TimerModule;
import net.shoreline.client.impl.module.render.FreecamModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.client.util.math.MathUtil;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class SpeedModule extends ToggleModule
{
    private static SpeedModule INSTANCE;

    //
    Config<Speed> speedModeConfig = register(new EnumConfig<>("Mode", "Speed mode", Speed.STRAFE, Speed.values()));
    Config<Float> collisionDistanceConfig = register(new NumberConfig<>("CollisionDistance", "The distance to apply collision speed", 0.5f, 1.5f, 2.0f, () -> speedModeConfig.getValue() == Speed.GRIM_COLLIDE));
    Config<Float> speedConfig = register(new NumberConfig<>("Speed", "The speed for alternative modes", 0.1f, 4.0f, 50.0f, () -> speedModeConfig.getValue() == Speed.VANILLA));
    Config<Boolean> timerConfig = register(new BooleanConfig("UseTimer", "Uses timer to increase acceleration", false, () -> isStrafe()));
    Config<Boolean> fastConfig = register(new BooleanConfig("Fast", "Fast timer speed", false, () -> speedModeConfig.getValue() == Speed.STRAFE_STRICT && timerConfig.getValue()));
    Config<Boolean> strafeBoostConfig = register(new BooleanConfig("StrafeBoost", "Uses explosion velocity to boost Strafe", false, () -> isStrafe()));
    Config<Integer> boostTicksConfig = register(new NumberConfig<>("BoostTicks", "The number of ticks to boost strafe", 10, 20, 40, () -> isStrafe() && strafeBoostConfig.getValue()));
    Config<Boolean> speedWaterConfig = register(new BooleanConfig("SpeedInWater", "Applies speed even in water and lava", false));
    //
    private int strafe = 4;
    private boolean accel;
    private int strictTicks;
    private int strictFastTicks;
    private int boostTicks;
    //
    private double speed;
    private double boostSpeed;
    private double distance;
    //
    private boolean prevTimer;

    private static final float FRICTION = 159.077f;

    /**
     *
     */
    public SpeedModule()
    {
        super("Speed", "Move faster", ModuleCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static SpeedModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public String getModuleData()
    {
        if (speedModeConfig.getValue() == Speed.GRIM_COLLIDE)
        {
            return "Grim";
        }
        return EnumFormatter.formatEnum(speedModeConfig.getValue());
    }

    @Override
    public void onEnable()
    {
        prevTimer = TimerModule.getInstance().isEnabled();
        if (timerConfig.getValue() && !prevTimer && isStrafe())
        {
            TimerModule.getInstance().enable();
        }
    }

    @Override
    public void onDisable()
    {
        resetStrafe();
        if (TimerModule.getInstance().isEnabled())
        {
            TimerModule.getInstance().resetTimer();
            if (!prevTimer)
            {
                TimerModule.getInstance().disable();
            }
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE)
        {
            boostTicks++;
            if (boostTicks > boostTicksConfig.getValue())
            {
                boostSpeed = 0.0;
            }
            double dx = mc.player.getX() - mc.player.xo;
            double dz = mc.player.getZ() - mc.player.zo;
            distance = Math.sqrt(dx * dx + dz * dz);
            if (speedModeConfig.getValue() == Speed.GRIM_COLLIDE && MovementUtil.isInputtingMovement())
            {
                int collisions = 0;
                for (Entity entity : mc.level.entitiesForRendering())
                {
                    if (checkIsCollidingEntity(entity) && Mth.sqrt((float) mc.player.distanceToSqr(entity)) <= collisionDistanceConfig.getValue())
                    {
                        collisions++;
                    }
                }
                if (collisions > 0)
                {
                    Vec3 velocity = mc.player.getDeltaMovement();
                    // double COLLISION_DISTANCE = 1.5;
                    double factor = 0.08 * collisions;
                    Vec2 strafe = handleStrafeMotion((float) factor);
                    mc.player.setDeltaMovement(velocity.x + strafe.x, velocity.y, velocity.z + strafe.y);
                }
            }
        }
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (mc.player != null && mc.level != null && event.getType() == MoverType.SELF)
        {
            if (!MovementUtil.isInputtingMovement()
                    || FlightModule.getInstance().isEnabled()
                    || LongJumpModule.getInstance().isEnabled()
                    || DisablerModule.getInstance().grimFireworkCheck()
                    || (FreecamModule.getInstance().isEnabled() && !FreecamModule.getInstance().control)
                    || mc.player.getAbilities().flying
                    //      || Modules.ELYTRA_FLY.isEnabled()
                    || mc.player.isHandsBusy()
                    || mc.player.isFallFlying()
                    || mc.player.isSuppressingSlidingDownLadder()
                    || mc.player.fallDistance > 2.0f
                    || (mc.player.isInLava() || mc.player.isInWater())
                    && !speedWaterConfig.getValue())
            {
                resetStrafe();
                TimerModule.getInstance().setTimer(1.0f);
                return;
            }
            event.cancel();
            //
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
            float jumpEffect = 0.0f;
            if (mc.player.hasEffect(MobEffects.JUMP))
            {
                jumpEffect += (mc.player.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1f;
            }
            // ~29 kmh
            if (speedModeConfig.getValue() == Speed.STRAFE || speedModeConfig.getValue() == Speed.STRAFE_B_HOP)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    return;
                }
                if (timerConfig.getValue())
                {
                    TimerModule.getInstance().setTimer(1.0888f);
                }
                if (strafe == 1)
                {
                    speed = 1.35f * base - 0.01f;
                }
                else if (strafe == 2)
                {
                    if (mc.player.input.jumping || !mc.player.onGround())
                    {
                        return;
                    }
                    float jump = (speedModeConfig.getValue() == Speed.STRAFE_B_HOP ? 0.4000000059604645f : 0.3999999463558197f) + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= speedModeConfig.getValue() == Speed.STRAFE_B_HOP ? 1.535 : (accel ? 1.6835 : 1.395);
                }
                else if (strafe == 3)
                {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                    accel = !accel;
                }
                else
                {
                    if ((!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0,
                            mc.player.getDeltaMovement().y(), 0)) || mc.player.verticalCollision) && strafe > 0)
                    {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                if (strafeBoostConfig.getValue())
                {
                    speed += boostSpeed;
                }
                final Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            // ~26-27 kmh
            else if (speedModeConfig.getValue() == Speed.STRAFE_STRICT)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    return;
                }
                if (timerConfig.getValue())
                {
                    if (fastConfig.getValue())
                    {
                        ++strictFastTicks;
                        if (strictFastTicks > 10)
                        {
                            strictFastTicks = 0;
                        }
                        float res = 1.0f + strictFastTicks / 100.0f;
                        TimerModule.getInstance().setTimer(Math.max(1.0f, res));
                    }
                    else
                    {
                        TimerModule.getInstance().setTimer(1.0888f);
                    }
                }
                if (strafe == 1)
                {
                    speed = 1.35f * base - 0.01f;
                }
                else if (strafe == 2)
                {
                    if (mc.player.input.jumping || !mc.player.onGround())
                    {
                        return;
                    }
                    float jump = 0.3999999463558197f + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= 2.149;
                }
                else if (strafe == 3)
                {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                }
                else
                {
                    if ((!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0,
                            mc.player.getDeltaMovement().y(), 0)) || mc.player.verticalCollision) && strafe > 0)
                    {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                strictTicks++;
                speed = Math.max(speed, base);
                //
                if (timerConfig.getValue())
                {
                    TimerModule.getInstance().setTimer(1.0888f);
                }
                double baseMax = 0.465 * speedEffect / slowEffect;
                double baseMin = 0.44 * speedEffect / slowEffect;
                speed = Math.min(speed, strictTicks > 25 ? baseMax : baseMin);
                if (strafeBoostConfig.getValue())
                {
                    speed += boostSpeed;
                }
                if (strictTicks > 50)
                {
                    strictTicks = 0;
                }
                final Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            else if (speedModeConfig.getValue() == Speed.LOW_HOP)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    return;
                }
                if (timerConfig.getValue())
                {
                    TimerModule.getInstance().setTimer(1.0888f);
                }
                if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.4, 3))
                {
                    Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                    event.setY(0.31 + jumpEffect);
                }
                else if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.71, 3))
                {
                    Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                    event.setY(0.04 + jumpEffect);
                }
                else if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.75, 3))
                {
                    Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                    event.setY(-0.2 - jumpEffect);
                }
                else if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.55, 3))
                {
                    Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                    event.setY(-0.14 + jumpEffect);
                }
                else
                {
                    if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.41, 3))
                    {
                        Managers.MOVEMENT.setMotionY(-0.2 + jumpEffect);
                        event.setY(-0.2 + jumpEffect);
                    }
                }
                if (strafe == 1)
                {
                    speed = 1.35f * base - 0.01f;
                }
                else if (strafe == 2)
                {
                    double jump = (isBoxColliding() ? 0.2 : 0.3999) + jumpEffect;
                    Managers.MOVEMENT.setMotionY(jump);
                    event.setY(jump);
                    speed *= accel ? 1.5685 : 1.3445;
                }
                else if (strafe == 3)
                {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                    accel = !accel;
                }
                else
                {
                    if (mc.player.onGround() && strafe > 0)
                    {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleVanillaMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            else if (speedModeConfig.getValue() == Speed.GAY_HOP)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    strafe = 1;
                    return;
                }
                if (strafe == 1 && mc.player.verticalCollision
                        && MovementUtil.isInputtingMovement())
                {
                    speed = 1.25f * base - 0.01f;
                }
                else if (strafe == 2 && mc.player.verticalCollision
                        && MovementUtil.isInputtingMovement())
                {
                    float jump = (isBoxColliding() ? 0.2f : 0.4f) + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= 2.149;
                }
                else if (strafe == 3)
                {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                }
                else
                {
                    if (mc.player.onGround() && strafe > 0)
                    {
                        if (1.35 * base - 0.01 > speed)
                        {
                            strafe = 0;
                        }
                        else
                        {
                            strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                        }
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                if (strafe > 0)
                {
                    Vec2 motion = handleStrafeMotion((float) speed);
                    event.setX(motion.x);
                    event.setZ(motion.y);
                }
                strafe++;
            }
            else if (speedModeConfig.getValue() == Speed.V_HOP)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    strafe = 1;
                    return;
                }
                if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.4, 3))
                {
                    Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                    event.setY(0.31 + jumpEffect);
                }
                else if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.71, 3))
                {
                    Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                    event.setY(0.04 + jumpEffect);
                }
                else if (MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.75, 3))
                {
                    Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                    event.setY(-0.2 - jumpEffect);
                }
                if (!mc.level.noCollision(null, mc.player.getBoundingBox().move(0.0, -0.56, 0.0))
                        && MathUtil.round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == MathUtil.round(0.55, 3))
                {
                    Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                    event.setY(-0.14 + jumpEffect);
                }
                if (strafe != 1 || !mc.player.verticalCollision
                        || mc.player.zza == 0.0f && mc.player.xxa == 0.0f)
                {
                    if (strafe != 2 || !mc.player.verticalCollision
                            || mc.player.zza == 0.0f && mc.player.xxa == 0.0f)
                    {
                        if (strafe == 3)
                        {
                            double moveSpeed = 0.66 * (distance - base);
                            speed = distance - moveSpeed;
                        }
                        else
                        {
                            if (mc.player.onGround() && strafe > 0)
                            {
                                if (1.35 * base - 0.01 > speed)
                                {
                                    strafe = 0;
                                }
                                else
                                {
                                    strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                                }
                            }
                            speed = distance - distance / FRICTION;
                        }
                    }
                    else
                    {
                        double jump = (isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                        Managers.MOVEMENT.setMotionY(jump);
                        event.setY(jump);
                        speed *= 2.149;
                    }
                }
                else
                {
                    speed = 2.0 * base - 0.01;
                }
                if (strafe > 8)
                {
                    speed = base;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            else if (speedModeConfig.getValue() == Speed.B_HOP)
            {
                if (!Managers.ANTICHEAT.hasPassed(100))
                {
                    strafe = 4;
                    return;
                }
                if (MathUtil.round(mc.player.getY() - ((int) mc.player.getY()), 3) == MathUtil.round(0.138, 3))
                {
                    Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y - (0.08 + jumpEffect));
                    event.setY(event.getY() - (0.0931 + jumpEffect));
                    Managers.POSITION.setPositionY(mc.player.getY() - (0.0931 + jumpEffect));
                }
                if (strafe != 2 || mc.player.zza == 0.0f && mc.player.xxa == 0.0f)
                {
                    if (strafe == 3)
                    {
                        double moveSpeed = 0.66 * (distance - base);
                        speed = distance - moveSpeed;
                    }
                    else
                    {
                        if (mc.player.onGround())
                        {
                            strafe = 1;
                        }
                        speed = distance - distance / FRICTION;
                    }
                }
                else
                {
                    double jump = (isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                    Managers.MOVEMENT.setMotionY(jump);
                    event.setY(jump);
                    speed *= 2.149;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            else if (speedModeConfig.getValue() == Speed.VANILLA)
            {
                Vec2 motion = handleStrafeMotion(speedConfig.getValue() / 10.0f);
                event.setX(motion.x);
                event.setZ(motion.y);
            }
        }
    }

    /**
     * @param speed
     * @return
     */
    public Vec2 handleStrafeMotion(final float speed)
    {
        float forward = mc.player.input.forwardImpulse;
        float strafe = mc.player.input.leftImpulse;
        float yaw = mc.player.yRotO + (mc.player.getYRot() - mc.player.yRotO) * mc.getTimer().getGameTimeDeltaPartialTick(true);
        if (forward == 0.0f && strafe == 0.0f)
        {
            return Vec2.ZERO;
        }
        else if (forward != 0.0f)
        {
            if (strafe >= 1.0f)
            {
                yaw += forward > 0.0f ? -45 : 45;
                strafe = 0.0f;
            }
            else if (strafe <= -1.0f)
            {
                yaw += forward > 0.0f ? 45 : -45;
                strafe = 0.0f;
            }
            if (forward > 0.0f)
            {
                forward = 1.0f;
            }
            else if (forward < 0.0f)
            {
                forward = -1.0f;
            }
        }
        float rx = (float) Math.cos(Math.toRadians(yaw));
        float rz = (float) -Math.sin(Math.toRadians(yaw));
        return new Vec2((forward * speed * rz) + (strafe * speed * rx),
                (forward * speed * rx) - (strafe * speed * rz));
    }

    public Vec2 handleVanillaMotion(final float speed)
    {
        float forward = mc.player.input.forwardImpulse;
        float strafe = mc.player.input.leftImpulse;
        if (forward == 0.0f && strafe == 0.0f)
        {
            return Vec2.ZERO;
        }
        else if (forward != 0.0f && strafe != 0.0f)
        {
            forward *= (float) Math.sin(0.7853981633974483);
            strafe *= (float) Math.cos(0.7853981633974483);
        }
        return new Vec2((float) (forward * speed * -Math.sin(Math.toRadians(mc.player.getYRot())) + strafe * speed * Math.cos(Math.toRadians(mc.player.getYRot()))),
                (float) (forward * speed * Math.cos(Math.toRadians(mc.player.getYRot())) - strafe * speed * -Math.sin(Math.toRadians(mc.player.getYRot()))));
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        if (event.getPacket() instanceof ClientboundExplodePacket packet)
        {
            double x = packet.getKnockbackX();
            double z = packet.getKnockbackZ();
            // boostSpeed = Math.sqrt(x * x + z * z);
            // boostTicks = 0;
        }
        else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
                && packet.getId() == mc.player.getId())
        {
            double x = packet.getXa();
            double z = packet.getZa();
            // boostSpeed = Math.sqrt(x * x + z * z);
            // boostTicks = 0;
        }
        else if (event.getPacket() instanceof ClientboundPlayerPositionPacket)
        {
            resetStrafe();
        }
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event)
    {
        if (event.getConfig() == timerConfig && event.getStage() == StageEvent.EventStage.POST && isStrafe())
        {
            if (timerConfig.getValue())
            {
                prevTimer = TimerModule.getInstance().isEnabled();
                if (!prevTimer)
                {
                    TimerModule.getInstance().enable();
                    // Modules.TIMER.setTimer(1.0888f);
                }
            }
            else if (TimerModule.getInstance().isEnabled())
            {
                TimerModule.getInstance().resetTimer();
                if (!prevTimer)
                {
                    TimerModule.getInstance().disable();
                }
            }
        }
    }

    public boolean isBoxColliding()
    {
        return !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, 0.21, 0.0));
    }

    public boolean checkIsCollidingEntity(Entity entity)
    {
        return entity != null && entity != mc.player && entity instanceof LivingEntity
                && !(entity instanceof FakePlayerEntity) && !(entity instanceof ArmorStand);
    }

    public void setPrevTimer()
    {
        prevTimer = !prevTimer;
    }

    public boolean isUsingTimer()
    {
        return isEnabled() && timerConfig.getValue();
    }

    public void resetStrafe()
    {
        strafe = 4;
        strictTicks = 0;
        strictFastTicks = 0;
        speed = 0.0f;
        distance = 0.0;
        accel = false;
    }

    public boolean isStrafe()
    {
        return speedModeConfig.getValue() != Speed.GRIM_COLLIDE && speedModeConfig.getValue() != Speed.VANILLA;
    }

    private enum Speed
    {
        STRAFE,
        STRAFE_STRICT,
        STRAFE_B_HOP,
        LOW_HOP,
        GAY_HOP,
        V_HOP,
        B_HOP,
        VANILLA,
        GRIM_COLLIDE
    }
}
