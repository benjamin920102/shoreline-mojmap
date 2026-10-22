package net.shoreline.client.impl.manager.player.interaction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
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
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.world.SneakBlocks;
import net.shoreline.eventbus.EventBus;

public final class InteractionManager implements Globals {
   private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

   public InteractionManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public boolean canPlace(BlockPos pos, Block block) {
      BlockState state = block.defaultBlockState();
      VoxelShape shape = block.getShape(state, mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
      if (!shape.isEmpty()) {
         for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
            if (!entity.isSpectator()
               && entity.isAlive()
               && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)
               && (
                  !(entity instanceof ItemFrame)
                     || this.placedOnEntities.containsKey(entity.getId())
                        && this.placedOnEntities.get(entity.getId()) > AnticheatModule.getInstance().getEntityPlaceThreshold()
               )) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, rotationCallback, false);
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback, boolean airPlace) {
      boolean isEntityBlockingPlacement;
      BlockState airState = Blocks.AIR.defaultBlockState();
      VoxelShape shape = airState.getShape(mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
      isEntityBlockingPlacement = false;
      label58:
      if (!shape.isEmpty()) {
         Iterator direction = mc.level.getEntities(null, shape.bounds()).iterator();

         while (true) {
            if (!direction.hasNext()) {
               break label58;
            }

            Entity entity = (Entity)direction.next();
            if (!entity.isSpectator() && entity.isAlive() && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
               if (!(entity instanceof ItemFrame)) {
                  break;
               }

               this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
               if (this.placedOnEntities.getOrDefault(entity.getId(), 0) > AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                  break;
               }
            }
         }

         isEntityBlockingPlacement = true;
      }

      if (isEntityBlockingPlacement) {
         return false;
      } else {
         Direction direction = this.getInteractDirectionInternal(pos, strictDirection);
         if (!airPlace && (!AirPlaceModule.getInstance().isEnabled() || direction != null)) {
            if (direction == null) {
               return false;
            } else {
               BlockPos neighbor = pos.relative(direction.getOpposite());
               return this.placeBlock(neighbor, direction, slot, clientSwing, false, rotationCallback);
            }
         } else {
            direction = Direction.DOWN;
            return this.placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
         }
      }
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, boolean packet, RotationCallback rotationCallback) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, packet, false, rotationCallback);
   }

   public boolean placeBlock(
      BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, boolean packet, boolean airPlace, RotationCallback rotationCallback
   ) {
      boolean isEntityBlockingPlacement;
      BlockState airState = Blocks.AIR.defaultBlockState();
      VoxelShape shape = airState.getShape(mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
      isEntityBlockingPlacement = false;
      label58:
      if (!shape.isEmpty()) {
         Iterator direction = mc.level.getEntities(null, shape.bounds()).iterator();

         while (true) {
            if (!direction.hasNext()) {
               break label58;
            }

            Entity entity = (Entity)direction.next();
            if (!entity.isSpectator() && entity.isAlive() && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
               if (!(entity instanceof ItemFrame)) {
                  break;
               }

               this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
               if (this.placedOnEntities.getOrDefault(entity.getId(), 0) > AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                  break;
               }
            }
         }

         isEntityBlockingPlacement = true;
      }

      if (isEntityBlockingPlacement) {
         return false;
      } else {
         Direction direction = this.getInteractDirectionInternal(pos, strictDirection);
         if (!airPlace && (!AirPlaceModule.getInstance().isEnabled() || direction != null)) {
            if (direction == null) {
               return false;
            } else {
               BlockPos neighbor = pos.relative(direction.getOpposite());
               return this.placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
            }
         } else {
            direction = Direction.DOWN;
            return this.placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
         }
      }
   }

   public boolean placeBlock(
      BlockPos pos, Direction direction, int slot, boolean clientSwing, boolean grimAirPlace, boolean packet, RotationCallback rotationCallback
   ) {
      Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
      return this.placeBlock(new BlockHitResult(hitVec, direction, pos, false), slot, clientSwing, grimAirPlace, packet, rotationCallback);
   }

   public boolean placeBlock(BlockPos pos, Direction direction, int slot, boolean clientSwing, boolean grimAirPlace, RotationCallback rotationCallback) {
      Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
      return this.placeBlock(new BlockHitResult(hitVec, direction, pos, false), slot, clientSwing, grimAirPlace, rotationCallback);
   }

   public boolean placeBlock(BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, boolean packet, RotationCallback rotationCallback) {
      boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
      if (isSpoofing) {
         Managers.INVENTORY.setSlot(slot);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      }

      boolean isRotating = rotationCallback != null;
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
         rotationCallback.handleRotation(true, angles);
      }

      boolean result = this.placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, packet);
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
         rotationCallback.handleRotation(false, angles);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      }

      if (isSpoofing) {
         Managers.INVENTORY.syncToClient();
      }

      return result;
   }

   public boolean placeBlock(BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, RotationCallback rotationCallback) {
      boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
      if (isSpoofing) {
         Managers.INVENTORY.setSlot(slot);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      }

      boolean isRotating = rotationCallback != null;
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
         rotationCallback.handleRotation(true, angles);
      }

      boolean result = this.placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, true);
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
         rotationCallback.handleRotation(false, angles);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      }

      if (isSpoofing) {
         Managers.INVENTORY.syncToClient();
      }

      return result;
   }

   public boolean placeBlockImmediately(BlockHitResult result, InteractionHand hand, boolean clientSwing, boolean packet) {
      BlockState state = mc.level.getBlockState(result.getBlockPos());
      boolean shouldSneak = SneakBlocks.isSneakBlock(state) && !mc.player.isShiftKeyDown();
      if (shouldSneak) {
         Managers.MOVEMENT.setPacketSneaking(true);
         MovementUtil.applySneak();
      }

      InteractionResult actionResult = packet ? this.placeBlockPacket(result, hand) : this.placeBlockInternally(result, hand);
      if (actionResult.consumesAction() && actionResult.shouldSwing()) {
         if (clientSwing) {
            mc.player.swing(InteractionHand.MAIN_HAND);
         } else {
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }
      }

      if (shouldSneak) {
         Managers.MOVEMENT.setPacketSneaking(false);
      }

      return actionResult.consumesAction();
   }

   private InteractionResult placeBlockInternally(BlockHitResult hitResult, InteractionHand hand) {
      return mc.gameMode.useItemOn(mc.player, hand, hitResult);
   }

   public InteractionResult placeBlockPacket(BlockHitResult hitResult, InteractionHand hand) {
      Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, hitResult, id));
      return InteractionResult.SUCCESS;
   }

   public Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
      Direction direction = this.getInteractDirectionInternal(blockPos, strictDirection);
      return direction == null ? Direction.UP : direction;
   }

   public Direction getInteractDirectionInternal(BlockPos blockPos, boolean strictDirection) {
      Set<Direction> validDirections = this.getPlaceDirectionsNCP(mc.player.getEyePosition(), blockPos.getCenter());
      Direction interactDirection = null;

      for (Direction direction : Direction.values()) {
         BlockState state = mc.level.getBlockState(blockPos.relative(direction));
         if (!state.isAir()
            && state.getFluidState().isEmpty()
            && state.getBlock() != Blocks.ANVIL
            && state.getBlock() != Blocks.CHIPPED_ANVIL
            && state.getBlock() != Blocks.DAMAGED_ANVIL
            && (!strictDirection || validDirections.contains(direction.getOpposite()))) {
            interactDirection = direction;
            break;
         }
      }

      return interactDirection == null ? null : interactDirection.getOpposite();
   }

   public Direction getPlaceDirectionNCP(BlockPos blockPos, boolean visible) {
      Vec3 eyePos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ());
      if (blockPos.getX() == eyePos.x() && blockPos.getY() == eyePos.y() && blockPos.getZ() == eyePos.z()) {
         return Direction.DOWN;
      } else {
         for (Direction dir : this.getPlaceDirectionsNCP(eyePos, blockPos.getCenter())) {
            if (!visible || mc.level.isEmptyBlock(blockPos.relative(dir))) {
               return dir;
            }
         }

         return Direction.UP;
      }
   }

   public Set<Direction> getPlaceDirectionsNCP(Vec3 eyePos, Vec3 blockPos) {
      return this.getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
   }

   public Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
      double xdiff = x - dx;
      double ydiff = y - dy;
      double zdiff = z - dz;
      Set<Direction> dirs = new HashSet<>(6);
      if (ydiff > 0.5) {
         dirs.add(Direction.UP);
      } else if (ydiff < -0.5) {
         dirs.add(Direction.DOWN);
      } else {
         dirs.add(Direction.UP);
         dirs.add(Direction.DOWN);
      }

      if (xdiff > 0.5) {
         dirs.add(Direction.EAST);
      } else if (xdiff < -0.5) {
         dirs.add(Direction.WEST);
      } else {
         dirs.add(Direction.EAST);
         dirs.add(Direction.WEST);
      }

      if (zdiff > 0.5) {
         dirs.add(Direction.SOUTH);
      } else if (zdiff < -0.5) {
         dirs.add(Direction.NORTH);
      } else {
         dirs.add(Direction.SOUTH);
         dirs.add(Direction.NORTH);
      }

      return dirs;
   }

   public boolean isInEyeRange(BlockPos pos) {
      return pos.getY() > mc.player.getY() + mc.player.getEyeHeight();
   }
}
