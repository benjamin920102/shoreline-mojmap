package net.shoreline.client.impl.module.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.shoreline.client.util.math.position.DirectionUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AnticheatModule extends ConcurrentModule
{
    private static AnticheatModule INSTANCE;

    Config<Anticheats> modeConfig = register(new EnumConfig<>("Mode", "Applies anticheat optimizations", Anticheats.VANILLA, Anticheats.values()));
    Config<Float> entityPlaceConfig = register(new NumberConfig<>("PlaceThreshold", "The max ticks to place on entities", 0.0f, 2.0f, 25.0f, () -> modeConfig.getValue() != Anticheats.VANILLA));
    Config<Boolean> miningFixConfig = register(new BooleanConfig("MiningFix", "Fixes vanilla mining on GrimV3", false, () -> modeConfig.getValue() == Anticheats.GRIM));
    Config<Boolean> webJumpFixConfig = register(new BooleanConfig("WebJumpFix", "Fixes sprint jumping in webs on grim", false, () -> modeConfig.getValue() == Anticheats.GRIM));
    Config<Boolean> noScreenCloseConfig = register(new BooleanConfig("NoScreenClose", "Prevents the server from closing your inventory screen", false, () -> modeConfig.getValue() == Anticheats.GRIM));
    Config<Boolean> raytraceSpoofConfig = register(new BooleanConfig("RaytraceFix", "Allows you to spoof your raytrace", false, () -> modeConfig.getValue() == Anticheats.N_C_P));

    private final Timer raytraceTimer = new CacheTimer();
    private float pitch = Float.NaN;

    public AnticheatModule()
    {
        super("Anticheat", "Settings for anticheat configs", ModuleCategory.CLIENT);
        INSTANCE = this;
    }

    public static AnticheatModule getInstance()
    {
        return INSTANCE;
    }

    @EventListener(priority = 10000)
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (isNCP() && raytraceSpoofConfig.getValue() && event.getPacket() instanceof ServerboundUseItemOnPacket packet && raytraceTimer.passed(250))
        {
            BlockHitResult packetResult = packet.getHitResult();
            BlockPos pos = packetResult.getBlockPos();
            BlockHitResult result = mc.level.clip(new ClipContext(
                    mc.player.getEyePosition(), DirectionUtil.getDirectionOffsetPos(pos, packetResult.getDirection()),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player));
            if (mc.level.noCollision(mc.player.getBoundingBox().expandTowards(0.0, 0.15, 0.0)) && result != null
                    && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos))
            {
                pitch = -75;
                raytraceTimer.reset();
            }
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation() && !Float.isNaN(pitch))
        {
            ((AccessorPlayerMoveC2SPacket) packet).hookSetPitch(pitch);
            pitch = Float.NaN;
        }

        if (isGrim() && miningFixConfig.getValue() && event.getPacket() instanceof ServerboundPlayerActionPacket packet
                && (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                || packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                || packet.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK))
        {
            if (BlastResistantBlocks.isUnbreakable(packet.getPos()))
            {
                event.cancel();
                return;
            }

            if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
            {
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                event.cancel();
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (isGrim() && noScreenCloseConfig.getValue() && event.getPacket() instanceof ClientboundContainerClosePacket packet && packet.getContainerId() == 0)
        {
            event.cancel();
        }
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.POST &&
                event.getConfig() == raytraceSpoofConfig && !raytraceSpoofConfig.getValue())
        {
            pitch = Float.NaN;
        }
    }

    public boolean isGrim()
    {
        return modeConfig.getValue() == Anticheats.GRIM;
    }

    public boolean isNCP()
    {
        return modeConfig.getValue() == Anticheats.N_C_P;
    }

    public boolean getMiningFix()
    {
        return miningFixConfig.getValue();
    }

    public boolean getWebJumpFix()
    {
        return webJumpFixConfig.getValue();
    }

    public int getEntityPlaceThreshold()
    {
        return modeConfig.getValue() == Anticheats.VANILLA ? 0 : Math.round(entityPlaceConfig.getValue() * 10.0f);
    }

    private enum Anticheats
    {
        GRIM,
        N_C_P,
        VANILLA
    }
}