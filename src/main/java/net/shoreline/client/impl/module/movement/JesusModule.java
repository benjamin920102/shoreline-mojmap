package net.shoreline.client.impl.module.movement;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.player.PlayerJumpEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.world.BlockCollisionEvent;
import net.shoreline.client.impl.module.exploit.PacketFlyModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorKeyBinding;
import net.shoreline.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class JesusModule extends ToggleModule
{
    //
    Config<JesusMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for walking on water", JesusMode.SOLID, JesusMode.values()));
    Config<Boolean> strictConfig = register(new BooleanConfig("Strict", "NCP Updated bypass for floating offsets", false, () -> modeConfig.getValue() == JesusMode.SOLID));
    //
    private int floatTimer = 1000;
    private boolean fluidState;
    //
    private double floatOffset;

    /**
     *
     */
    public JesusModule()
    {
        super("Jesus", "Allow player to walk on water", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getModuleData()
    {
        return EnumFormatter.formatEnum(modeConfig.getValue());
    }

    @Override
    public void onDisable()
    {
        floatOffset = 0.0;
        //
        floatTimer = 1000;
        KeyMapping.set(((AccessorKeyBinding) mc.options.keyJump).getBoundKey(), false);
    }

    @EventListener
    public void onBlockCollision(BlockCollisionEvent event)
    {
        BlockState state = event.getState();
        if (FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled()
                || mc.player.isSpectator() || mc.player.isOnFire() || state.getFluidState().isEmpty())
        {
            return;
        }
        if (modeConfig.getValue() != JesusMode.DOLPHIN && state.getFluidState().isSource() && ((state.getBlock() == Blocks.WATER
                || state.getFluidState().getType() == Fluids.WATER) || state.getBlock() == Blocks.LAVA))
        {
            event.cancel();
            event.setVoxelShape(Shapes.create(new AABB(0.0, 0.0, 0.0, 1.0, 0.99, 1.0)));
            if (mc.player.getVehicle() != null)
            {
                event.setVoxelShape(Shapes.create(new AABB(0.0, 0.0, 0.0, 1.0,
                        0.949999988079071, 1.0)));
            }
            else if (modeConfig.getValue() == JesusMode.TRAMPOLINE)
            {
                event.setVoxelShape(Shapes.create(new AABB(0.0, 0.0, 0.0, 1.0, 0.96, 1.0)));
            }
        }
    }

    @EventListener
    public void onPlayerJump(PlayerJumpEvent event)
    {
        if (!isInFluid() && isOnFluid())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE)
        {
            if (FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled())
            {
                return;
            }
            if (modeConfig.getValue() == JesusMode.SOLID)
            {
                if (isInFluid() || mc.player.fallDistance > 3.0f
                        || mc.player.isShiftKeyDown())
                {
                    // floatOffset = 0.0;
                }
                if (!mc.options.keyShift.isDown() && !mc.options.keyJump.isDown())
                {
                    if (isInFluid())
                    {
                        floatTimer = 0;
                        Managers.MOVEMENT.setMotionY(0.11);
                        return;
                    }
                    if (floatTimer == 0)
                    {
                        Managers.MOVEMENT.setMotionY(0.30);
                    }
                    else if (floatTimer == 1)
                    {
                        Managers.MOVEMENT.setMotionY(0.0);
                    }
                    floatTimer++;
                }
            }
            else if (modeConfig.getValue() == JesusMode.DOLPHIN && isInFluid()
                    && !mc.options.keyShift.isDown() && !mc.options.keyJump.isDown())
            {
                KeyMapping.set(((AccessorKeyBinding) mc.options.keyJump).getBoundKey(), true);
            }
        }
    }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event)
    {
        if (FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled())
        {
            return;
        }
        if (event.getStage() == StageEvent.EventStage.PRE
                && modeConfig.getValue() == JesusMode.TRAMPOLINE)
        {
            boolean inFluid = getFluidBlockInBB(mc.player.getBoundingBox()) != null;
            if (inFluid && !mc.player.isShiftKeyDown())
            {
                mc.player.setOnGround(false);
            }
            Block block = mc.level.getBlockState(new BlockPos((int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()),
                    (int) Math.floor(mc.player.getZ()))).getBlock();
            if (fluidState && !mc.player.getAbilities().flying && !mc.player.isInWater())
            {
                if (mc.player.getDeltaMovement().y < -0.3 || mc.player.onGround()
                        || mc.player.isSuppressingSlidingDownLadder())
                {
                    fluidState = false;
                    return;
                }
                Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y / 0.9800000190734863 + 0.08);
                Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y - 0.03120000000005);
            }
            if (isInFluid())
            {
                Managers.MOVEMENT.setMotionY(0.1);
                fluidState = false;
                return;
            }
            if (!isInFluid() && block instanceof LiquidBlock
                    && mc.player.getDeltaMovement().y < 0.2)
            {
                Managers.MOVEMENT.setMotionY(strictConfig.getValue() ? 0.184 : 0.5);
                fluidState = true;
            }
        }
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (event.isClientPacket() || mc.player == null || mc.getConnection() == null
                || mc.player.tickCount <= 20 || FlightModule.getInstance().isEnabled()
                || PacketFlyModule.getInstance().isEnabled())
        {
            return;
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet
                && packet.hasPosition()
                && modeConfig.getValue() == JesusMode.SOLID && !isInFluid()
                && isOnFluid() && mc.player.fallDistance <= 3.0f)
        {
            double y = packet.getY(mc.player.getY());
            if (!strictConfig.getValue())
            {
                floatOffset = mc.player.tickCount % 2 == 0 ? 0.0 : 0.05;
            }
            ((AccessorPlayerMoveC2SPacket) packet).hookSetY(y - floatOffset);
            if (strictConfig.getValue())
            {
                floatOffset += 0.12;
                if (floatOffset > 0.4)
                {
                    floatOffset = 0.2;
                }
            }
        }
    }

    public boolean isInFluid()
    {
        return mc.player.isInWater() || mc.player.isInLava();
    }

    public BlockState getFluidBlockInBB(AABB box)
    {
        return getFluidBlockInBB(Mth.floor(box.minY - 0.2));
    }

    public BlockState getFluidBlockInBB(int minY)
    {
        for (int i = Mth.floor(mc.player.getBoundingBox().minX); i < Mth.ceil(mc.player.getBoundingBox().maxX); i++)
        {
            for (int j = Mth.floor(mc.player.getBoundingBox().minZ); j < Mth.ceil(mc.player.getBoundingBox().maxZ); j++)
            {
                BlockState state = mc.level.getBlockState(new BlockPos(i, minY, j));
                if (state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource())
                {
                    return state;
                }
            }
        }
        return null;
    }

    public boolean isOnFluid()
    {
        if (mc.player.fallDistance >= 3.0f)
        {
            return false;
        }
        final AABB bb = mc.player.getVehicle() != null ?
                mc.player.getVehicle().getBoundingBox().deflate(0.0, 0.0, 0.0)
                        .move(0.0, -0.05000000074505806, 0.0) :
                mc.player.getBoundingBox().deflate(0.0, 0.0, 0.0).move(0.0,
                        -0.05000000074505806, 0.0);
        boolean onLiquid = false;
        int y = (int) bb.minY;
        for (int x = Mth.floor(bb.minX); x < Mth.floor(bb.maxX + 1.0); x++)
        {
            for (int z = Mth.floor(bb.minZ); z < Mth.floor(bb.maxZ + 1.0); z++)
            {
                final BlockState block = mc.level.getBlockState(new BlockPos(x, y, z));
                if (block.getBlock() != Blocks.AIR)
                {
                    if (block.getBlock() instanceof LiquidBlock && block.getFluidState().isSource())
                    {
                        return false;
                    }
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }

    public enum JesusMode
    {
        SOLID,
        DOLPHIN,
        TRAMPOLINE
    }
}
