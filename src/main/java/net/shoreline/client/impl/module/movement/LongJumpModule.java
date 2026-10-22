package net.shoreline.client.impl.module.movement;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.entity.player.TravelEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.impl.module.exploit.PacketFlyModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class LongJumpModule extends ToggleModule
{
    private static LongJumpModule INSTANCE;

    //
    Config<JumpMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for long jump", JumpMode.NORMAL, JumpMode.values()));
    Config<Float> boostConfig = register(new NumberConfig<>("Boost", "The jump boost speed", 0.1f, 4.5f, 10.0f, () -> modeConfig.getValue() == JumpMode.NORMAL));
    Config<Boolean> elytraBoostConfig = register(new BooleanConfig("VanillaBoost", "Uses vanilla elytra boost", false, () -> modeConfig.getValue() == JumpMode.GRIM));
    Config<Boolean> autoDisableConfig = register(new BooleanConfig("AutoDisable", "Automatically disables when rubberband is detected", true));
    //
    private int stage;
    private double distance;
    private double speed;
    //
    private int airTicks;
    private int groundTicks;

    /**
     *
     */
    public LongJumpModule()
    {
        super("LongJump", "Allows the player to jump farther", ModuleCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static LongJumpModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public String getModuleData()
    {
        return EnumFormatter.formatEnum(modeConfig.getValue());
    }

    @Override
    public void onEnable()
    {
        groundTicks = 0;
    }

    @Override
    public void onDisable()
    {
        stage = 0;
        distance = 0.0;
    }

    @EventListener
    public void onTravel(TickEvent event)
    {
        if (mc.player == null || event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }
        double dx = mc.player.getX() - mc.player.xo;
        double dz = mc.player.getZ() - mc.player.zo;
        distance = Math.sqrt(dx * dx + dz * dz);

        if (modeConfig.getValue() == JumpMode.GRIM)
        {
            AABB bb = mc.player.getBoundingBox();
            boolean shouldFall = false;
            for (double i = 0.0; i < 2.0; i += 0.01)
            {
                if (!mc.level.noCollision(mc.player, bb.move(0.0, -i, 0.0)))
                {
                    shouldFall = true;
                    break;
                }
            }
            if (!shouldFall)
            {
                return;
            }
            int elytraSlot = -1;
            for (int i = 0; i < 36; i++)
            {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof ElytraItem)
                {
                    elytraSlot = i;
                    break;
                }
            }
            if (elytraSlot == -1)
            {
                return;
            }
            if (mc.player.onGround())
            {
                mc.player.jumpFromGround();
            }
            else if (mc.player.getDeltaMovement().y < 0.0 && !mc.player.isFallFlying())
            {
                Managers.INVENTORY.click(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, ClickType.PICKUP);
                Managers.INVENTORY.click(6, 0, ClickType.PICKUP);
                Managers.INVENTORY.click(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, ClickType.PICKUP);
                Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                mc.player.startFallFlying();
                Managers.INVENTORY.click(6, 0, ClickType.PICKUP);
                Managers.INVENTORY.click(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, ClickType.PICKUP);
                Managers.INVENTORY.click(6, 0, ClickType.PICKUP);
            }

            if (elytraBoostConfig.getValue())
            {
                if (!mc.player.isFallFlying() || mc.player.isInWater() || mc.player.isInLava() || mc.player.getFoodData().getFoodLevel() <= 6.0f)
                {
                    return;
                }
                // event.cancel();
                if (mc.player.getDeltaMovement().y < 0.0)
                {
                    final double d4 = 0.014f * Math.cos(Math.toRadians(mc.player.getYRot() + 90.0f));
                    final double d5 = 0.014f * Math.sin(Math.toRadians(mc.player.getYRot() + 90.0f));
                    Vec3 glide = new Vec3(d4, 0.0, d5);
                    Vec3 motion = mc.player.getDeltaMovement();
                    Managers.MOVEMENT.setMotionXZ(motion.x + glide.x, motion.z + glide.z);
                }
            }
        }
    }

    @EventListener
    public void onElytraTransform(ElytraTransformEvent event)
    {
        if (modeConfig.getValue() == JumpMode.GRIM && event.getEntity() == mc.player)
        {
            event.cancel();
        }
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (modeConfig.getValue() == JumpMode.NORMAL)
        {
            if (mc.player == null || mc.level == null
                    || FlightModule.getInstance().isEnabled()
                    || DisablerModule.getInstance().grimFireworkCheck()
                    || PacketFlyModule.getInstance().isEnabled()
                    || !MovementUtil.isInputtingMovement())
            {
                return;
            }
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
            if (stage == 0)
            {
                stage = 1;
                speed = boostConfig.getValue() * base - 0.01;
            }
            else if (stage == 1)
            {
                stage = 2;
                Managers.MOVEMENT.setMotionY(0.42);
                event.setY(0.42);
                speed *= 2.149;
            }
            else if (stage == 2)
            {
                stage = 3;
                double moveSpeed = 0.66 * (distance - base);
                speed = distance - moveSpeed;
            }
            else
            {
                if (!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0,
                        mc.player.getDeltaMovement().y(), 0)) || mc.player.verticalCollision)
                {
                    stage = 0;
                }
                speed = distance - distance / 159.0;
            }
            speed = Math.max(speed, base);
            event.cancel();
            Vec2 motion = SpeedModule.getInstance().handleStrafeMotion((float) speed);
            event.setX(motion.x);
            event.setZ(motion.y);
        }
    }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event)
    {
        // Direkt LongJump
        if (event.getStage() == StageEvent.EventStage.PRE
                && modeConfig.getValue() == JumpMode.GLIDE)
        {
            if (FlightModule.getInstance().isEnabled() || mc.player.isFallFlying()
                    || mc.player.isSuppressingSlidingDownLadder()
                    || mc.player.isInWater())
            {
                return;
            }
            if (mc.player.onGround())
            {
                distance = 0.0;
            }
            final float direction = mc.player.getYRot() +
                    ((mc.player.zza < 0.0f) ? 180 : 0) +
                    ((mc.player.xxa > 0.0f) ? (-90.0f *
                            ((mc.player.zza < 0.0f) ? -0.5f :
                                    ((mc.player.zza > 0f) ? 0.5f : 1.0f)))
                            : 0.0f) - ((mc.player.xxa < 0.0f) ? (-90.0f *
                    ((mc.player.zza < 0.0f) ? -0.5f :
                            ((mc.player.zza > 0.0f) ? 0.5f : 1.0f))) : 0.0f);
            final float dx = (float) Math.cos((direction + 90.0f) * Math.PI / 180.0);
            final float dz = (float) Math.sin((direction + 90.0f) * Math.PI / 180.0);
            if (!mc.player.verticalCollision)
            {
                airTicks++;
                if (mc.player.input.shiftKeyDown)
                {
                    mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                            0.0, 2.147483647e9, 0.0, false));
                }
                groundTicks = 0;
                if (!mc.player.verticalCollision)
                {
                    if (mc.player.getDeltaMovement().y == -0.07190068807140403)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.35f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.10306193759436909)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.55f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.13395038817442878)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.67f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.16635183030382)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.69f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.19088711097794803)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.71f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.21121925191528862)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.2f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.11979897632390576)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.93f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.18758479151225355)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.72f);
                    }
                    if (mc.player.getDeltaMovement().y == -0.21075983825251726)
                    {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.76f);
                    }
                    if (getJumpCollisions(mc.player, 70.0) < 0.5)
                    {
                        if (mc.player.getDeltaMovement().y == -0.23537393014173347)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.03f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08531999505205401)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * -0.5);
                        }
                        if (mc.player.getDeltaMovement().y == -0.03659320313669756)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * -0.1f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.07481386749524899)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * -0.07f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.0732677700939672)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * -0.05f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.07480988066790395)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * -0.04f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.0784000015258789)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.1f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08608320193943977)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.1f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08683615560584318)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.05f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08265497329678266)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.05f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08245009535659828)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.05f);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08244005633718426)
                        {
                            Managers.MOVEMENT.setMotionY(-0.08243956442521608);
                        }
                        if (mc.player.getDeltaMovement().y == -0.08243956442521608)
                        {
                            Managers.MOVEMENT.setMotionY(-0.08244005590677261);
                        }
                        if (mc.player.getDeltaMovement().y > -0.1
                                && mc.player.getDeltaMovement().y < -0.08
                                && !mc.player.onGround()
                                && mc.player.input.up)
                        {
                            Managers.MOVEMENT.setMotionY(-1.0e-4f);
                        }
                    }
                    else
                    {
                        if (mc.player.getDeltaMovement().y < -0.2
                                && mc.player.getDeltaMovement().y > -0.24)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.7);
                        }
                        if (mc.player.getDeltaMovement().y < -0.25
                                && mc.player.getDeltaMovement().y > -0.32)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.8);
                        }
                        if (mc.player.getDeltaMovement().y < -0.35
                                && mc.player.getDeltaMovement().y > -0.8)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.98);
                        }
                        if (mc.player.getDeltaMovement().y < -0.8
                                && mc.player.getDeltaMovement().y > -1.6)
                        {
                            Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y * 0.99);
                        }
                    }
                }
                Managers.TICK.setClientTick(0.85f);
                double[] jumpFactor = new double[]
                        {
                                0.420606, 0.417924, 0.415258, 0.412609,
                                0.409977, 0.407361, 0.404761, 0.402178,
                                0.399611, 0.39706, 0.394525, 0.392, 0.3894,
                                0.38644, 0.383655, 0.381105, 0.37867, 0.37625,
                                0.37384, 0.37145, 0.369, 0.3666, 0.3642, 0.3618,
                                0.35945, 0.357, 0.354, 0.351, 0.348, 0.345,
                                0.342, 0.339, 0.336, 0.333, 0.33, 0.327, 0.324,
                                0.321, 0.318, 0.315, 0.312, 0.309, 0.307,
                                0.305, 0.303, 0.3, 0.297, 0.295, 0.293, 0.291,
                                0.289, 0.287, 0.285, 0.283, 0.281, 0.279, 0.277,
                                0.275, 0.273, 0.271, 0.269, 0.267, 0.265, 0.263,
                                0.261, 0.259, 0.257, 0.255, 0.253, 0.251, 0.249,
                                0.247, 0.245, 0.243, 0.241, 0.239, 0.237
                        };
                if (mc.player.input.up)
                {
                    try
                    {
                        Managers.MOVEMENT.setMotionXZ((double) dx * jumpFactor[airTicks - 1] * 3.0,
                                (double) dz * jumpFactor[airTicks - 1] * 3.0);
                    }
                    catch (ArrayIndexOutOfBoundsException ignored)
                    {

                    }
                    return;
                }
                Managers.MOVEMENT.setMotionXZ(0.0, 0.0);
                return;
            }
            Managers.TICK.setClientTick(1.0f);
            airTicks = 0;
            groundTicks++;
            Managers.MOVEMENT.setMotionXZ(mc.player.getDeltaMovement().x / 13.0,
                    mc.player.getDeltaMovement().z / 13.0);
            if (groundTicks == 1)
            {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        mc.player.getX(), mc.player.getY(),
                        mc.player.getZ(), mc.player.onGround()));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        mc.player.getX() + 0.0624, mc.player.getY(),
                        mc.player.getZ(), mc.player.onGround()));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        mc.player.getX(), mc.player.getY() + 0.419,
                        mc.player.getZ(), mc.player.onGround()));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        mc.player.getX() + 0.0624, mc.player.getY(),
                        mc.player.getZ(), mc.player.onGround()));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                        mc.player.getX(), mc.player.getY() + 0.419,
                        mc.player.getZ(), mc.player.onGround()));
            }
            if (groundTicks > 2)
            {
                groundTicks = 0;
                Managers.MOVEMENT.setMotionXZ(dx * 0.3, dz * 0.3);
                Managers.MOVEMENT.setMotionY(0.42399999499320984);
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.level == null
                || mc.screen instanceof ReceivingLevelScreen)
        {
            return;
        }
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket
                && autoDisableConfig.getValue())
        {
            disable();
        }
    }

    /**
     * @param player
     * @param d
     * @return
     */
    private double getJumpCollisions(Player player, double d)
    {
        /*
        List<VoxelShape> collisions = Lists.newArrayList(mc.world.getCollisions(
                player, player.getBoundingBox().expand(0.0, -d, 0.0)));
        if (collisions.isEmpty())
        {
            return 0.0;
        }
        d = 0.0;
        for (VoxelShape coll : collisions)
        {
            Box bb = coll.getBoundingBox();
            if (bb.maxY <= d)
            {
                continue;
            }
            d = bb.maxY;
        }
        return player.getY() - d;
        */
        return 1.0;
    }

    public enum JumpMode
    {
        NORMAL,
        GLIDE,
        GRIM
    }
}
