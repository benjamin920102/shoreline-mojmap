package net.shoreline.client.impl.manager.player.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.math.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.world.SneakBlocks;
import net.shoreline.eventbus.EventBus;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xgraza
 * @since 1.0
 */
public final class InteractionManager implements Globals
{
    private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

    public InteractionManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    public boolean canPlace(BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        VoxelShape shape = block.getShape(state, mc.level, pos, CollisionContext.empty())
                .move(pos.getX(), pos.getY(), pos.getZ());
        if (!shape.isEmpty()) {
            for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
                if (entity.isSpectator() || !entity.isAlive() ||
                        !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND) ||
                        entity instanceof ItemFrame && (!this.placedOnEntities.containsKey(entity.getId()) ||
                                this.placedOnEntities.get(entity.getId()) <= AnticheatModule.getInstance().getEntityPlaceThreshold())) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }


    public boolean placeBlock(final BlockPos pos,
                              final int slot,
                              final boolean strictDirection,
                              final boolean clientSwing,
                              final RotationCallback rotationCallback)
    {
        return placeBlock(pos, slot, strictDirection, clientSwing, rotationCallback, false);
    }

    public boolean placeBlock(final BlockPos pos,
                              final int slot,
                              final boolean strictDirection,
                              final boolean clientSwing,
                              final RotationCallback rotationCallback,
                              final boolean airPlace) {
        BlockState airState = Blocks.AIR.defaultBlockState();
        VoxelShape shape = airState.getShape(mc.level, pos, CollisionContext.empty())
                .move(pos.getX(), pos.getY(), pos.getZ());

        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
                if (entity.isSpectator() || !entity.isAlive() ||
                        !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
                    continue;
                }
                if (entity instanceof ItemFrame) {
                    this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                    if (this.placedOnEntities.getOrDefault(entity.getId(), 0) <= AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                        continue;
                    }
                }
                isEntityBlockingPlacement = true;
                break;
            }
        }
        if (isEntityBlockingPlacement) {
            return false;
        }
        Direction direction = getInteractDirectionInternal(pos, strictDirection);
        if (airPlace || AirPlaceModule.getInstance().isEnabled() && direction == null)
        {
            direction = Direction.DOWN;
            return placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
        }
        if (direction == null)
        {
            return false;
        }
        final BlockPos neighbor = pos.relative(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos,
                              final int slot,
                              final boolean strictDirection,
                              final boolean clientSwing,
                              final boolean packet,
                              final RotationCallback rotationCallback)
    {
        return placeBlock(pos, slot, strictDirection, clientSwing, packet, false, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos,
                              final int slot,
                              final boolean strictDirection,
                              final boolean clientSwing,
                              final boolean packet,
                              final boolean airPlace,
                              final RotationCallback rotationCallback) {
        BlockState airState = Blocks.AIR.defaultBlockState();
        VoxelShape shape = airState.getShape(mc.level, pos, CollisionContext.empty())
                .move(pos.getX(), pos.getY(), pos.getZ());

        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
                if (entity.isSpectator() || !entity.isAlive() ||
                        !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
                    continue;
                }
                if (entity instanceof ItemFrame) {
                    this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                    if (this.placedOnEntities.getOrDefault(entity.getId(), 0) <= AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                        continue;
                    }
                }
                isEntityBlockingPlacement = true;
                break;
            }
        }
        if (isEntityBlockingPlacement) {
            return false;
        }
        Direction direction = getInteractDirectionInternal(pos, strictDirection);
        if (airPlace || AirPlaceModule.getInstance().isEnabled() && direction == null)
        {
            direction = Direction.DOWN;
            return placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
        }
        if (direction == null)
        {
            return false;
        }
        final BlockPos neighbor = pos.relative(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos,
                              final Direction direction,
                              final int slot,
                              final boolean clientSwing,
                              final boolean grimAirPlace,
                              final boolean packet,
                              final RotationCallback rotationCallback)
    {
        Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
                slot, clientSwing, grimAirPlace, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos,
                              final Direction direction,
                              final int slot,
                              final boolean clientSwing,
                              final boolean grimAirPlace,
                              final RotationCallback rotationCallback)
    {
        Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
                slot, clientSwing, grimAirPlace, rotationCallback);
    }

    public boolean placeBlock(final BlockHitResult hitResult,
                              final int slot,
                              final boolean clientSwing,
                              final boolean grimAirPlace,
                              final boolean packet,
                              final RotationCallback rotationCallback)
    {
        final boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
        if (isSpoofing)
        {
            Managers.INVENTORY.setSlot(slot);
        }

        if (grimAirPlace)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating)
        {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, packet);
        if (isRotating)
        {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        if (isSpoofing)
        {
            Managers.INVENTORY.syncToClient();
        }

        return result;
    }

    public boolean placeBlock(final BlockHitResult hitResult,
                              final int slot,
                              final boolean clientSwing,
                              final boolean grimAirPlace,
                              final RotationCallback rotationCallback)
    {
        final boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
        if (isSpoofing)
        {
            Managers.INVENTORY.setSlot(slot);
        }

        if (grimAirPlace)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating)
        {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, true);
        if (isRotating)
        {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        if (isSpoofing)
        {
            Managers.INVENTORY.syncToClient();
        }

        return result;
    }

    public boolean placeBlockImmediately(final BlockHitResult result,
                                         final InteractionHand hand,
                                         final boolean clientSwing,
                                         final boolean packet)
    {
        final BlockState state = mc.level.getBlockState(result.getBlockPos());
        final boolean shouldSneak = SneakBlocks.isSneakBlock(state) && !mc.player.isShiftKeyDown();
        if (shouldSneak)
        {
            Managers.MOVEMENT.setPacketSneaking(true);
            MovementUtil.applySneak();
        }
        final InteractionResult actionResult = packet ? placeBlockPacket(result, hand) : placeBlockInternally(result, hand);
        if (actionResult.consumesAction() && actionResult.shouldSwing())
        {
            if (clientSwing)
            {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            else
            {
                Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
        if (shouldSneak)
        {
            Managers.MOVEMENT.setPacketSneaking(false);
        }
        return actionResult.consumesAction();
    }

    private InteractionResult placeBlockInternally(final BlockHitResult hitResult,
                                              final InteractionHand hand)
    {
        return mc.gameMode.useItemOn(mc.player, hand, hitResult);
    }

    public InteractionResult placeBlockPacket(final BlockHitResult hitResult,
                                         final InteractionHand hand)
    {
        Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, hitResult, id));
        return InteractionResult.SUCCESS;
    }

    public Direction getInteractDirection(final BlockPos blockPos, final boolean strictDirection)
    {
        Direction direction = getInteractDirectionInternal(blockPos, strictDirection);
        return direction == null ? Direction.UP : direction;
    }

    /**
     * @param blockPos
     * @param strictDirection
     * @return
     */
    public Direction getInteractDirectionInternal(final BlockPos blockPos, final boolean strictDirection)
    {
        Set<Direction> validDirections = getPlaceDirectionsNCP(mc.player.getEyePosition(), blockPos.getCenter());
        Direction interactDirection = null;
        for (final Direction direction : Direction.values())
        {
            final BlockState state = mc.level.getBlockState(blockPos.relative(direction));
            if (state.isAir() || !state.getFluidState().isEmpty())
            {
                continue;
            }

            if (state.getBlock() == Blocks.ANVIL || state.getBlock() == Blocks.CHIPPED_ANVIL
                    || state.getBlock() == Blocks.DAMAGED_ANVIL)
            {
                continue;
            }

            if (strictDirection && !validDirections.contains(direction.getOpposite()))
            {
                continue;
            }
            interactDirection = direction;
            break;
        }
        if (interactDirection == null)
        {
            return null;
        }
        return interactDirection.getOpposite();
    }

    public Direction getPlaceDirectionNCP(BlockPos blockPos, boolean visible)
    {
        Vec3 eyePos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());
        if (blockPos.getX() == eyePos.x() && blockPos.getY() == eyePos.y() && blockPos.getZ() == eyePos.z())
        {
            return Direction.DOWN;
        }
        else
        {
            Set<Direction> ncpDirections = getPlaceDirectionsNCP(eyePos, blockPos.getCenter());
            for (Direction dir : ncpDirections)
            {
                if (visible && !mc.level.isEmptyBlock(blockPos.relative(dir)))
                {
                    continue;
                }
                return dir;
            }
        }
        return Direction.UP;
    }

    public Set<Direction> getPlaceDirectionsNCP(Vec3 eyePos, Vec3 blockPos)
    {
        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
    }

    public Set<Direction> getPlaceDirectionsNCP(final double x, final double y, final double z,
                                                final double dx, final double dy, final double dz)
    {
        // directly from NCP src
        final double xdiff = x - dx;
        final double ydiff = y - dy;
        final double zdiff = z - dz;
        final Set<Direction> dirs = new HashSet<>(6);
        if (ydiff > 0.5)
        {
            dirs.add(Direction.UP);
        }
        else if (ydiff < -0.5)
        {
            dirs.add(Direction.DOWN);
        }
        else
        {
            dirs.add(Direction.UP);
            dirs.add(Direction.DOWN);
        }
        if (xdiff > 0.5)
        {
            dirs.add(Direction.EAST);
        }
        else if (xdiff < -0.5)
        {
            dirs.add(Direction.WEST);
        }
        else
        {
            dirs.add(Direction.EAST);
            dirs.add(Direction.WEST);
        }
        if (zdiff > 0.5)
        {
            dirs.add(Direction.SOUTH);
        }
        else if (zdiff < -0.5)
        {
            dirs.add(Direction.NORTH);
        }
        else
        {
            dirs.add(Direction.SOUTH);
            dirs.add(Direction.NORTH);
        }
        return dirs;
    }

    /**
     * Checks if the block is within our "eye range"
     * You can't place blocks above your head for any direction other than DOWN
     *
     * @param pos the block position
     * @return if the block pos is in range of our eye y coordinate
     */
    public boolean isInEyeRange(final BlockPos pos)
    {
        return pos.getY() > mc.player.getY() + mc.player.getEyeHeight();
    }
}