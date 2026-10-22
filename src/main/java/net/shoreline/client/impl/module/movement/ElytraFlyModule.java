package net.shoreline.client.impl.module.movement;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.MouseUpdateEvent;
import net.shoreline.client.impl.event.camera.CameraRotationEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.entity.player.TravelEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class ElytraFlyModule extends RotationModule
{
    private static ElytraFlyModule INSTANCE;

    Config<FlyMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for elytra flight", FlyMode.CONTROL, FlyMode.values()));
    // Speeds
    Config<Float> speedConfig = register(new NumberConfig<>("Speed", "The horizontal flight speed", 0.1f, 2.5f, 10.0f, () -> modeConfig.getValue() != FlyMode.BOOST && modeConfig.getValue() != FlyMode.BOUNCE));
    Config<Float> vspeedConfig = register(new NumberConfig<>("VerticalSpeed", "The vertical flight speed", 0.1f, 1.0f, 5.0f, () -> modeConfig.getValue() != FlyMode.BOOST && modeConfig.getValue() != FlyMode.BOUNCE));
    Config<Boolean> accelerateConfig = register(new BooleanConfig("Accelerate", "Accelerates fly speed", false, () -> modeConfig.getValue() != FlyMode.BOOST && modeConfig.getValue() != FlyMode.BOUNCE));
    Config<Float> accelSpeedConfig = register(new NumberConfig<>("AccelSpeed", "Acceleration speed", 0.01f, 0.1f, 1.00f, () -> accelerateConfig.getValue() && modeConfig.getValue() != FlyMode.BOOST && modeConfig.getValue() != FlyMode.BOUNCE));
    Config<Float> maxSpeedConfig = register(new NumberConfig<>("MaxSpeed", "The maximum flight speed", 0.1f, 3.5f, 10.0f, () -> accelerateConfig.getValue() && modeConfig.getValue() != FlyMode.BOOST && modeConfig.getValue() != FlyMode.BOUNCE));
    Config<Boolean> boostConfig = register(new BooleanConfig("VanillaBoost", "Uses vanilla boost speed", false, () -> modeConfig.getValue() != FlyMode.BOOST));
    // Control
    Config<Boolean> rotateConfig = register(new BooleanConfig("Rotate", "Rotates the player when moving", false, () -> modeConfig.getValue() == FlyMode.CONTROL));
    Config<Boolean> fireworkConfig = register(new BooleanConfig("Fireworks", "Uses fireworks when flying", false, () -> modeConfig.getValue() == FlyMode.CONTROL));
    // Bounce
    Config<Float> pitchConfig = register(new NumberConfig<>("Pitch", "The pitch angle of bounce", 70.0f, 80.0f, 90.0f, () -> modeConfig.getValue() == FlyMode.BOUNCE));
    Config<Boolean> obstaclePasserConfig = new BooleanConfig("ObstaclePasser", "Passes obstacles and resets fly using Baritone", modeConfig.getValue() == FlyMode.BOUNCE);

    private final static double GRIM_AIR_FRICTION = 0.0264444413;

    private boolean resetSpeed;
    private float speed;

    private float cameraPitch;

    public ElytraFlyModule()
    {
        super("ElytraFly", "Allows you to fly freely using an elytra", ModuleCategory.MOVEMENT);
        INSTANCE = this;
        if (ShorelineMod.isBaritonePresent())
        {
            register(obstaclePasserConfig);
        }
    }

    public static ElytraFlyModule getInstance()
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
        resetSpeed = true;
    }

    @Override
    public void onDisable()
    {
        if (ShorelineMod.isBaritonePresent())
        {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
        }
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.POST &&
                event.getConfig() == accelerateConfig && accelerateConfig.getValue())
        {
            resetSpeed = true;
        }
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (!mc.player.isFallFlying())
        {
            return;
        }

        if (ShorelineMod.isBaritonePresent() && obstaclePasserConfig.getValue())
        {
            if (!BaritoneAPI.getSettings().freeLook.value)
            {
                ChatUtil.clientSendMessage(ChatFormatting.RED + "Please enable FreeLook in Baritone to use ObstaclePasser!", 5005);
                return;
            }
            if (mc.player.horizontalCollision)
            {
                BlockPos goalBlockPos = null;
                for (int i = 3; i < 64; i++)
                {
                    Vec3 position = mc.player.position();
                    Vec3 vec3d2 = RotationUtil.getRotationVector(0.0f, mc.player.getYRot());
                    Vec3 vec3d3 = position.add(vec3d2.x * i, 0.0, vec3d2.z * i);
                    BlockPos blockPos = BlockPos.containing(vec3d3);
                    if (mc.level.getBlockState(blockPos).isAir() && mc.level.getBlockState(blockPos.above()).isAir())
                    {
                        goalBlockPos = blockPos;
                        break;
                    }
                }
                if (goalBlockPos != null)
                {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                            .setGoalAndPath(new GoalBlock(goalBlockPos.getX(), goalBlockPos.getY(), goalBlockPos.getZ()));
                }
            }
        }

        if (modeConfig.getValue() == FlyMode.CONTROL && rotateConfig.getValue())
        {
            float yaw = SprintModule.getInstance().getSprintYaw(mc.player.getYRot());
            setRotation(yaw, getControlPitch());

            int fireworkSlot = -1;
            for (int i = 0; i < 36; i++)
            {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof FireworkRocketItem)
                {
                    fireworkSlot = i;
                    break;
                }
            }
            if (fireworkConfig.getValue() && fireworkSlot != -1)
            {
                if (fireworkSlot < 9)
                {
                    Managers.INVENTORY.setSlot(fireworkSlot);
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    Managers.INVENTORY.syncToClient();
                }
                else
                {
                    mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(0, fireworkSlot, 0, ClickType.PICKUP, mc.player);
                }
            }
        }
        else if (modeConfig.getValue() == FlyMode.BOUNCE)
        {
            if (canSprint())
            {
                mc.player.setSprinting(true);
            }
            mc.player.setXRot(pitchConfig.getValue());
            setRotation(mc.player.getYRot(), pitchConfig.getValue());
        }
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (!mc.player.isFallFlying() || mc.player.isInWater()
                || mc.player.isInLava() || mc.player.getFoodData().getFoodLevel() <= 6.0f)
        {
            return;
        }

        if (modeConfig.getValue() == FlyMode.BOOST)
        {
            boolean boost = (mc.options.keyJump.isDown() || AutoWalkModule.getInstance().isEnabled());
            if (boost)
            {
                applyVanillaBoost();
            }
        }
        else if (boostConfig.getValue())
        {
            applyVanillaBoost();
        }
    }

    @EventListener
    public void onTravel(TravelEvent event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (!MovementUtil.isInputtingMovement() || mc.player.horizontalCollision)
        {
            resetSpeed = true;
        }

        if (accelerateConfig.getValue())
        {
            if (resetSpeed)
            {
                speed = 0.1f;
                resetSpeed = false;
            }
            if (speed < maxSpeedConfig.getValue())
            {
                speed += accelSpeedConfig.getValue();
            }
            if (speed - accelSpeedConfig.getValue() > maxSpeedConfig.getValue())
            {
                speed -= accelSpeedConfig.getValue();
            }
        }
        else
        {
            speed = speedConfig.getValue();
        }

        switch (modeConfig.getValue())
        {
            case CONTROL ->
            {
                if (!mc.player.isFallFlying())
                {
                    return;
                }
                event.cancel();
                float forward = mc.player.input.forwardImpulse;
                float side = mc.player.input.leftImpulse;
                float yaw = Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : mc.player.getYRot();
                if (forward != 0.0f)
                {
                    if (side > 0.0f)
                    {
                        yaw += forward > 0.0f ? -45.0f : 45.0f;
                    }
                    else if (side < 0.0f)
                    {
                        yaw += forward > 0.0f ? 45.0f : -45.0f;
                    }
                    side = 0.0f;
                    if (forward > 0.0f)
                    {
                        forward = 1.0f;
                    }
                    else if (forward < 0.0f)
                    {
                        forward = -1.0f;
                    }
                }
                final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
                final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
                final double rx = forward * speed * cos + side * speed * sin;
                final double rz = forward * speed * sin - side * speed * cos;
                if (forward == 0.0f && side == 0.0f)
                {
                    Managers.MOVEMENT.setMotionXZ(0.0, 0.0);
                }
                else
                {
                    Managers.MOVEMENT.setMotionXZ(rx, rz);
                }
                Managers.MOVEMENT.setMotionY(0.0);
                if (mc.options.keyJump.isDown())
                {
                    Managers.MOVEMENT.setMotionY(vspeedConfig.getValue());
                }
                else if (mc.options.keyShift.isDown())
                {
                    Managers.MOVEMENT.setMotionY(-vspeedConfig.getValue());
                }
            }
            case BOUNCE ->
            {
                if (!mc.player.getInventory().getArmor(2).getItem().equals(Items.ELYTRA))
                {
                    return;
                }

                if (!mc.player.isFallFlying())
                {
                    Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    mc.player.startFallFlying();
                }
                if (mc.player.onGround())
                {
                    mc.player.jumpFromGround();
                }
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket)
        {
            resetSpeed = true;
        }
    }

    @EventListener
    public void onCameraRotation(CameraRotationEvent event)
    {
        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (modeConfig.getValue() == FlyMode.BOUNCE)
        {
            event.setYaw(event.getYaw());
            event.setPitch(cameraPitch);
        }
    }

    @EventListener
    public void onMouseUpdate(MouseUpdateEvent event)
    {
        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }

        if (modeConfig.getValue() == FlyMode.BOUNCE)
        {
            event.cancel();
            float f = (float) event.getCursorDeltaY() * 0.15F;
            float g = (float) event.getCursorDeltaX() * 0.15F;
            this.cameraPitch += f;
            mc.player.setYRot(mc.player.getYRot() + g);
            this.cameraPitch = Mth.clamp(cameraPitch, -90.0F, 90.0F);
        }
    }

    public float getControlPitch()
    {
        if (isBoostedByRocket())
        {
            if (mc.options.keyJump.isDown() && !mc.options.keyShift.isDown())
            {
                return MovementUtil.isMoving() ? -50.0f : -90.0f;
            }
            else if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown())
            {
                return MovementUtil.isMoving() ? 50.0f : 90.0f;
            }
            return 0.0f;
        }
        else
        {
            if (mc.options.keyJump.isDown() && !mc.options.keyShift.isDown())
            {
                return -50.0f;
            }
            else if (mc.options.keyShift.isDown() && !mc.options.keyJump.isDown())
            {
                return 50.0f;
            }
            return 0.1f;
        }
    }

    public void applyVanillaBoost()
    {
        float yaw = Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : mc.player.getYRot();
        final double x = GRIM_AIR_FRICTION * Math.cos(Math.toRadians(yaw + 90.0f));
        final double z = GRIM_AIR_FRICTION * Math.sin(Math.toRadians(yaw + 90.0f));
        double motionX = mc.player.getDeltaMovement().x + x;
        double motionZ = mc.player.getDeltaMovement().z + z;
        mc.player.setDeltaMovement(motionX, mc.player.getDeltaMovement().y, motionZ);
    }

    private boolean isBoostedByRocket()
    {
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (entity instanceof FireworkRocketEntity rocket
                    && ((AccessorFireworkRocketEntity) rocket).hookWasShotByEntity()
                    && ((AccessorFireworkRocketEntity) rocket).hookGetShooter() == mc.player)
            {
                return true;
            }
        }
        return false;
    }

    private boolean canSprint()
    {
        return !mc.player.isShiftKeyDown() && !mc.player.isHandsBusy()
                && !mc.player.isFallFlying()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.isSuppressingSlidingDownLadder()
                && !mc.player.hasEffect(MobEffects.BLINDNESS)
                && mc.player.getFoodData().getFoodLevel() > 6.0F;
    }

    public boolean isBounce()
    {
        return modeConfig.getValue() == FlyMode.BOUNCE;
    }

    public enum FlyMode
    {
        CONTROL,
        BOOST,
        BOUNCE,
        // PACKET
    }
}
