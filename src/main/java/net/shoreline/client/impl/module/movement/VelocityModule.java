package net.shoreline.client.impl.module.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.impl.event.entity.player.PushFluidsEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.PushOutOfBlocksEvent;
import net.shoreline.client.impl.module.combat.SurroundModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorBundlePacket;
import net.shoreline.client.mixin.accessor.AccessorClientWorld;
import net.shoreline.client.mixin.accessor.AccessorEntityVelocityUpdateS2CPacket;
import net.shoreline.client.mixin.accessor.AccessorExplosionS2CPacket;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin, linus
 * @since 1.0
 */
public class VelocityModule extends ToggleModule
{
    private static VelocityModule INSTANCE;

    Config<Boolean> knockbackConfig = register(new BooleanConfig("Knockback", "Removes player knockback velocity", true));
    Config<Boolean> explosionConfig = register(new BooleanConfig("Explosion", "Removes player explosion velocity", true));
    Config<VelocityMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for velocity", VelocityMode.NORMAL, VelocityMode.values()));
    Config<Float> horizontalConfig = register(new NumberConfig<>("Horizontal", "How much horizontal knock-back to take", 0.0f, 0.0f, 100.0f, NumberDisplay.PERCENT, () -> modeConfig.getValue() == VelocityMode.NORMAL || modeConfig.getValue() == VelocityMode.WALLS));
    Config<Float> verticalConfig = register(new NumberConfig<>("Vertical", "How much vertical knock-back to take", 0.0f, 0.0f, 100.0f, NumberDisplay.PERCENT, () -> modeConfig.getValue() == VelocityMode.NORMAL || modeConfig.getValue() == VelocityMode.WALLS));
    Config<Boolean> concealConfig = register(new BooleanConfig("Conceal", "Fixes velocity on servers with excessive setbacks", false));
    Config<Boolean> wallsAirConfig = register(new BooleanConfig("GroundOnly", "Only applies velocity in walls while on ground", false, () -> modeConfig.getValue() == VelocityMode.WALLS));
    Config<Boolean> wallsTrappedConfig = register(new BooleanConfig("Trapped", "Applies velocity while player head is trapped", false, () -> modeConfig.getValue() == VelocityMode.WALLS));
    Config<Boolean> pushEntitiesConfig = register(new BooleanConfig("NoPush-Entities", "Prevents being pushed away from entities", true));
    Config<Boolean> pushBlocksConfig = register(new BooleanConfig("NoPush-Blocks", "Prevents being pushed out of blocks", true));
    Config<Boolean> pushLiquidsConfig = register(new BooleanConfig("NoPush-Liquids", "Prevents being pushed by flowing liquids", true));
    Config<Boolean> pushFishhookConfig = register(new BooleanConfig("NoPush-Fishhook", "Prevents being pulled by fishing rod hooks", true));
    //
    private boolean cancelVelocity;
    private boolean concealVelocity;

    /**
     *
     */
    public VelocityModule()
    {
        super("Velocity", "Reduces the amount of player knockback velocity", ModuleCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static VelocityModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public String getModuleData()
    {
        if (modeConfig.getValue() == VelocityMode.NORMAL)
        {
            DecimalFormat decimal = new DecimalFormat("0.0");
            return String.format("H:%s%%, V:%s%%",
                    decimal.format(horizontalConfig.getValue()),
                    decimal.format(verticalConfig.getValue()));
        }
        return EnumFormatter.formatEnum(modeConfig.getValue());
    }

    @Override
    public void onEnable()
    {
        cancelVelocity = false;
    }

    @Override
    public void onDisable()
    {
        if (cancelVelocity)
        {
            if (modeConfig.getValue() == VelocityMode.GRIM)
            {
                float yaw = mc.player.getYRot();
                float pitch = mc.player.getXRot();
                if (Managers.ROTATION.isRotating())
                {
                    yaw = Managers.ROTATION.getRotationYaw();
                    pitch = Managers.ROTATION.getRotationPitch();
                }
                Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(),
                        mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN));
//                if (Managers.NETWORK.isCrystalPvpCC())
//                {
//                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
//                            mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
//                }
            }
            cancelVelocity = false;
        }

        concealVelocity = false;
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket && concealConfig.getValue())
        {
            concealVelocity = true;
        }

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && knockbackConfig.getValue())
        {
            if (packet.getId() != mc.player.getId())
            {
                return;
            }

            if (concealVelocity && packet.getXa() == 0 && packet.getZa() == 0 && packet.getZa() == 0)
            {
                concealVelocity = false;
                return;
            }

            if (modeConfig.getValue() == VelocityMode.WALLS)
            {
                if (!isPhased() && (!wallsTrappedConfig.getValue() || !isWallsTrapped()))
                {
                    return;
                }

                if (wallsAirConfig.getValue() && !Managers.POSITION.isOnGround())
                {
                    return;
                }
            }

            switch (modeConfig.getValue())
            {
                case NORMAL, WALLS ->
                {
                    if (horizontalConfig.getValue() == 0.0f && verticalConfig.getValue() == 0.0f)
                    {
                        event.cancel();
                        return;
                    }
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityX((int) (packet.getXa()
                            * (horizontalConfig.getValue() / 100.0f)));
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityY((int) (packet.getYa()
                            * (verticalConfig.getValue() / 100.0f)));
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityZ((int) (packet.getZa()
                            * (horizontalConfig.getValue() / 100.0f)));
                }
                case GRIM ->
                {
                    if (!Managers.ANTICHEAT.hasPassed(100))
                    {
                        return;
                    }
                    event.cancel();
                    cancelVelocity = true;
                }

                case GRIM_V3 -> event.setCanceled(isPhased());
            }
        }
        else if (event.getPacket() instanceof ClientboundExplodePacket packet && explosionConfig.getValue())
        {
            if (modeConfig.getValue() == VelocityMode.WALLS && !isPhased())
            {
                return;
            }

            switch (modeConfig.getValue())
            {
                case NORMAL, WALLS ->
                {
                    if (horizontalConfig.getValue() == 0.0f && verticalConfig.getValue() == 0.0f)
                    {
                        event.cancel();
                    }
                    else
                    {
                        ((AccessorExplosionS2CPacket) packet).setPlayerVelocityX(packet.getKnockbackX()
                                * (horizontalConfig.getValue() / 100.0f));
                        ((AccessorExplosionS2CPacket) packet).setPlayerVelocityY(packet.getKnockbackY()
                                * (verticalConfig.getValue() / 100.0f));
                        ((AccessorExplosionS2CPacket) packet).setPlayerVelocityZ(packet.getKnockbackZ()
                                * (horizontalConfig.getValue() / 100.0f));
                    }
                }
                case GRIM ->
                {
                    if (!Managers.ANTICHEAT.hasPassed(100))
                    {
                        return;
                    }
                    event.cancel();
                    cancelVelocity = true;
                }

                case GRIM_V3 -> event.setCanceled(isPhased());
            }

            if (event.isCanceled())
            {
                // Dumb fix bc canceling explosion velocity removes explosion handling in 1.19
                mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(packet.getX(), packet.getY(), packet.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                        4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));
            }
        }

        else if (event.getPacket() instanceof ClientboundBundlePacket packet)
        {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.subPackets())
            {
                if (packet1 instanceof ClientboundExplodePacket packet2 && explosionConfig.getValue())
                {
                    mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(packet2.getX(), packet2.getY(), packet2.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                            4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));

                    if (modeConfig.getValue() == VelocityMode.WALLS && !isPhased())
                    {
                        allowedBundle.add(packet1);
                        continue;
                    }

                    switch (modeConfig.getValue())
                    {
                        case NORMAL, WALLS ->
                        {
                            if (horizontalConfig.getValue() == 0.0f && verticalConfig.getValue() == 0.0f)
                            {
                                continue;
                            }
                            else
                            {
                                ((AccessorExplosionS2CPacket) packet2).setPlayerVelocityX(packet2.getKnockbackX()
                                        * (horizontalConfig.getValue() / 100.0f));
                                ((AccessorExplosionS2CPacket) packet2).setPlayerVelocityY(packet2.getKnockbackY()
                                        * (verticalConfig.getValue() / 100.0f));
                                ((AccessorExplosionS2CPacket) packet2).setPlayerVelocityZ(packet2.getKnockbackZ()
                                        * (horizontalConfig.getValue() / 100.0f));
                            }
                        }
                        case GRIM ->
                        {
                            if (Managers.ANTICHEAT.hasPassed(100))
                            {
                                allowedBundle.add(packet1);
                                continue;
                            }

                            cancelVelocity = true;
                            continue;
                        }
                        case GRIM_V3 ->
                        {
                            if (isPhased())
                            {
                                continue;
                            }
                        }
                    }
                }
                else if (packet1 instanceof ClientboundSetEntityMotionPacket packet2 && knockbackConfig.getValue())
                {
                    if (packet2.getId() != mc.player.getId())
                    {
                        allowedBundle.add(packet1);
                        continue;
                    }

                    if (modeConfig.getValue() == VelocityMode.WALLS)
                    {
                        if (!isPhased() && (!wallsTrappedConfig.getValue() || !isWallsTrapped()))
                        {
                            allowedBundle.add(packet1);
                            return;
                        }

                        if (wallsAirConfig.getValue() && !Managers.POSITION.isOnGround())
                        {
                            allowedBundle.add(packet1);
                            continue;
                        }
                    }

                    switch (modeConfig.getValue())
                    {
                        case NORMAL, WALLS ->
                        {
                            if (horizontalConfig.getValue() == 0.0f && verticalConfig.getValue() == 0.0f)
                            {
                                continue;
                            }
                            else
                            {
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityX((int) (packet2.getXa()
                                        * (horizontalConfig.getValue() / 100.0f)));
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityY((int) (packet2.getYa()
                                        * (verticalConfig.getValue() / 100.0f)));
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityZ((int) (packet2.getZa()
                                        * (horizontalConfig.getValue() / 100.0f)));
                            }
                        }
                        case GRIM ->
                        {
                            if (!Managers.ANTICHEAT.hasPassed(100))
                            {
                                allowedBundle.add(packet1);
                                continue;
                            }

                            cancelVelocity = true;
                            continue;
                        }
                        case GRIM_V3 ->
                        {
                            if (isPhased())
                            {
                                continue;
                            }
                        }
                    }
                }

                allowedBundle.add(packet1);
            }

            ((AccessorBundlePacket) packet).setIterable(allowedBundle);
        }

        else if (event.getPacket() instanceof ClientboundDamageEventPacket packet
                && packet.entityId() == mc.player.getId()
                && modeConfig.getValue() == VelocityMode.GRIM_V3 && isPhased())
        {
            Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(false));
            Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(true));
            // Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }

        else if (event.getPacket() instanceof ClientboundEntityEventPacket packet
                && packet.getEventId() == EntityEvent.FISHING_ROD_REEL_IN && pushFishhookConfig.getValue())
        {
            Entity entity = packet.getEntity(mc.level);
            if (entity instanceof FishingHook hook && hook.getHookedIn() == mc.player)
            {
                event.cancel();
            }
        }
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        concealVelocity = false;

        if (cancelVelocity)
        {
            if (modeConfig.getValue() == VelocityMode.GRIM)
            {
                // Fixes issue with rotations
                float yaw = Managers.ROTATION.getServerYaw();
                float pitch = Managers.ROTATION.getServerPitch();
                if (Managers.ROTATION.isRotating())
                {
                    yaw = Managers.ROTATION.getRotationYaw();
                    pitch = Managers.ROTATION.getRotationPitch();
                }
                Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(),
                        mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN));
//                if (Managers.NETWORK.isCrystalPvpCC())
//                {
//                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
//                            mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
//                }
            }
            cancelVelocity = false;
        }
    }

    @EventListener
    public void onPushEntity(PushEntityEvent event)
    {
        if (pushEntitiesConfig.getValue() && event.getPushed().equals(mc.player))
        {
            event.cancel();
        }
    }

    @EventListener
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event)
    {
        if (pushBlocksConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onPushFluid(PushFluidsEvent event)
    {
        if (pushLiquidsConfig.getValue())
        {
            event.cancel();
        }
    }

    private boolean isWallsTrapped()
    {
        BlockPos headPos = mc.player.blockPosition().above(mc.player.isVisuallyCrawling() ? 1 : 2);
        if (mc.level.getBlockState(headPos).canBeReplaced())
        {
            return false;
        }

        return SurroundModule.getInstance().getSurroundNoDown(mc.player).stream()
                .noneMatch(blockPos -> mc.level.getBlockState(mc.player.isVisuallyCrawling() ? blockPos : blockPos.above()).canBeReplaced());
    }

    private boolean isPhased()
    {
        return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream()
                .anyMatch(blockPos -> !mc.level.getBlockState(blockPos).canBeReplaced());
    }

    private enum VelocityMode
    {
        NORMAL,
        WALLS,
        GRIM,
        GRIM_V3
    }
}