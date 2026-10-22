package net.shoreline.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

public final class SelfTrapModule extends ObsidianPlacerModule {
   private static SelfTrapModule INSTANCE;
   Config<SelfTrapModule.Timing> timingConfig = this.register(
      new EnumConfig<>("Timing", "Timing for replacing blocks", SelfTrapModule.Timing.VANILLA, SelfTrapModule.Timing.values())
   );
   Config<Boolean> prePlaceExplosionConfig = this.register(
      new BooleanConfig("PrePlace-Explosions", "Pre places before explosions", false, () -> this.timingConfig.getValue() == SelfTrapModule.Timing.SEQUENTIAL)
   );
   Config<Boolean> prePlaceTickConfig = this.register(
      new BooleanConfig("PrePlaceTick", "Pre places before ticks", false, () -> this.timingConfig.getValue() == SelfTrapModule.Timing.SEQUENTIAL)
   );
   Config<Float> placeRangeConfig = this.register(new NumberConfig<>("PlaceRange", "The placement range for trap ", 0.0F, 4.0F, 6.0F));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   Config<Boolean> attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of trap ", true));
   Config<Boolean> extendConfig = this.register(new BooleanConfig("Extend", "Extends trap if the player is not in the center of a block", true));
   Config<Boolean> mineExtendConfig = this.register(new BooleanConfig("MineExtend", "Extends surround if the feet block is being mined", false));
   Config<Boolean> headExtendConfig = this.register(new BooleanConfig("HeadExtend", "Extends surround if the head block is being mined", false));
   Config<Boolean> supportConfig = this.register(new BooleanConfig("Support", "Creates a floor for the trap if there is none", false));
   Config<Boolean> headConfig = this.register(new BooleanConfig("CoverHead", "Place a block at your head", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disables after placing the blocks", true));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where trap is placing blocks", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private List<BlockPos> surround = new ArrayList<>();
   private List<BlockPos> placements = new ArrayList<>();
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private int blocksPlaced;
   private double prevY;

   public SelfTrapModule() {
      super("SelfTrap", "Fully surrounds the player with blocks", ModuleCategory.COMBAT, 900);
      INSTANCE = this;
   }

   public static SelfTrapModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }
   }

   @Override
   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.packets.clear();
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.blocksPlaced = 0;
      if (!this.autoDisableConfig.getValue() || !(mc.player.getY() - this.prevY > 0.5) && !(mc.player.fallDistance > 1.5F)) {
         if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
            this.surround.clear();
            this.placements.clear();
         } else {
            ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
            if (blockSlot != null) {
               int slot = blockSlot.slot();
               if (slot == -1) {
                  this.surround.clear();
                  this.placements.clear();
               } else {
                  BlockPos playerPos = PositionUtil.getRoundedBlockPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                  this.surround = this.getSurround(playerPos, mc.player);
                  if (!this.surround.isEmpty()) {
                     if (this.attackConfig.getValue()) {
                        this.attackBlockingCrystals(this.surround);
                     }

                     this.placements = this.getPlacementsFromSurround(this.surround);
                     if (!this.placements.isEmpty()) {
                        if (this.supportConfig.getValue()) {
                           for (BlockPos block : new ArrayList<>(this.placements)) {
                              if (block.getY() <= playerPos.getY()) {
                                 Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, this.strictDirectionConfig.getValue());
                                 if (direction == null) {
                                    this.placements.add(block.below());
                                 }
                              }
                           }
                        }

                        this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                        while (this.blocksPlaced < this.shiftTicksConfig.getValue() && this.blocksPlaced < this.placements.size()) {
                           BlockPos targetPos = this.placements.get(this.blocksPlaced);
                           this.placeBlock(targetPos, slot);
                        }

                        if (this.rotateConfig.getValue()) {
                           Managers.ROTATION.setRotationSilentSync();
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.disable();
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
               this.handlePackets(packet1);
            }
         } else {
            this.handlePackets(event.getPacket());
         }
      }
   }

   private void handlePackets(Packet<?> serverPacket) {
      if (this.timingConfig.getValue() == SelfTrapModule.Timing.SEQUENTIAL) {
         if (serverPacket instanceof ClientboundBlockUpdatePacket packet) {
            BlockState blockState = packet.getBlockState();
            BlockPos targetPos = packet.getPos();
            if (this.surround.contains(targetPos)) {
               if (blockState.canBeReplaced() && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, targetPos, CollisionContext.empty())) {
                  ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot == null) {
                     return;
                  }

                  int slot = blockSlot.slot();
                  if (slot == -1) {
                     return;
                  }

                  this.placeBlock(targetPos, slot);
               } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                  this.packets.remove(targetPos);
               }
            }
         }

         if (this.blocksPlaced <= this.shiftTicksConfig.getValue() * 2) {
            if (serverPacket instanceof ClientboundExplodePacket packetx && this.prePlaceExplosionConfig.getValue()) {
               BlockPos pos = BlockPos.containing(packetx.getX(), packetx.getY(), packetx.getZ());
               if (this.surround.contains(pos)) {
                  ObsidianPlacerModule.BlockSlot blockSlotx = this.getResistantBlockItem();
                  if (blockSlotx == null) {
                     return;
                  }

                  int slot = blockSlotx.slot();
                  if (slot == -1) {
                     return;
                  }

                  this.placeBlock(pos, slot);
               }
            }

            if (serverPacket instanceof ClientboundAddEntityPacket packetxx
               && packetxx.getType().equals(EntityType.END_CRYSTAL)
               && this.prePlaceTickConfig.getValue()) {
               for (BlockPos pos : this.surround) {
                  if (pos.equals(BlockPos.containing(packetxx.getX(), packetxx.getY(), packetxx.getZ()))) {
                     ObsidianPlacerModule.BlockSlot blockSlotxx = this.getResistantBlockItem();
                     if (blockSlotxx == null) {
                        return;
                     }

                     int slot = blockSlotxx.slot();
                     if (slot == -1) {
                        return;
                     }

                     this.placeBlock(pos, slot);
                     break;
                  }
               }
            }
         }
      }
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
      this.blocksPlaced++;
   }

   public void attackBlockingCrystals(List<BlockPos> posList) {
      for (BlockPos pos : posList) {
         Entity crystalEntity = mc.level.getEntities(null, new AABB(pos)).stream().filter(e -> e instanceof EndCrystal).findFirst().orElse(null);
         if (crystalEntity != null) {
            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(crystalEntity, mc.player.isShiftKeyDown()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            return;
         }
      }
   }

   public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();

      for (BlockPos surroundPos : surround) {
         Long placed = this.packets.get(surroundPos);
         if ((
               !(this.shiftDelayConfig.getValue() > 0.0F)
                  || placed == null
                  || !((float)(System.currentTimeMillis() - placed) < this.shiftDelayConfig.getValue() * 50.0F)
            )
            && mc.level.getBlockState(surroundPos).canBeReplaced()) {
            double dist = mc.player.distanceToSqr(surroundPos.getCenter());
            if (!(dist > ((NumberConfig)this.placeRangeConfig).getValueSq())
               && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, surroundPos, CollisionContext.empty())) {
               placements.add(surroundPos);
            }
         }
      }

      return placements;
   }

   public List<BlockPos> getSurround(BlockPos playerPos, Player player) {
      List<BlockPos> surroundBlocks = new ArrayList<>();
      List<BlockPos> playerBlocks = this.getPlayerBlocks(playerPos, player);

      for (BlockPos pos : playerBlocks) {
         for (Direction dir : Direction.values()) {
            if (dir.getAxis().isHorizontal()) {
               BlockPos pos1 = pos.relative(dir);
               if (!surroundBlocks.contains(pos1) && !playerBlocks.contains(pos1)) {
                  surroundBlocks.add(pos1);
                  surroundBlocks.add(pos1.above());
               }
            }
         }
      }

      if (this.headConfig.getValue()) {
         boolean support = false;
         List<BlockPos> headBlocks = new ArrayList<>();

         for (BlockPos pos : playerBlocks) {
            BlockPos headPos = pos.relative(Direction.UP, 2);
            if (!mc.level.getBlockState(headPos).canBeReplaced()) {
               support = true;
            }

            headBlocks.add(headPos);
         }

         if (!AirPlaceModule.getInstance().isEnabled()) {
            BlockPos supportingPos = null;
            double min = Double.MAX_VALUE;

            for (BlockPos pos : surroundBlocks) {
               BlockPos pos1 = pos.relative(Direction.UP, 2);
               if (!mc.level.getBlockState(pos1).canBeReplaced()) {
                  support = true;
                  break;
               }

               double dist = mc.player.distanceToSqr(pos1.getCenter());
               if (dist < min) {
                  supportingPos = pos1;
                  min = dist;
               }
            }

            if (supportingPos != null && !support) {
               surroundBlocks.add(supportingPos);
            }
         }

         surroundBlocks.addAll(headBlocks);
      }

      for (BlockPos pos2 : playerBlocks) {
         if (!pos2.equals(playerPos)) {
            surroundBlocks.add(pos2.below());
         }
      }

      if (this.mineExtendConfig.getValue()) {
         for (BlockPos surroundPos : new ArrayList<>(surroundBlocks)) {
            boolean secondLayer = surroundPos.getY() != playerPos.getY();
            if ((this.headExtendConfig.getValue() || !secondLayer)
               && Managers.BLOCK.isPassed(surroundPos, 0.7F)
               && (!secondLayer || Managers.INTERACT.getInteractDirectionInternal(surroundPos, this.strictDirectionConfig.getValue()) != null)) {
               for (Direction direction : Direction.values()) {
                  if (direction != Direction.DOWN && (direction != Direction.UP || !secondLayer)) {
                     BlockPos blockerPos = surroundPos.relative(direction);
                     if (!playerBlocks.contains(blockerPos) && AutoMineModule.getInstance().getMiningBlock() != blockerPos) {
                        surroundBlocks.add(blockerPos);
                     }
                  }
               }
            }
         }
      }

      return surroundBlocks;
   }

   public List<BlockPos> getPlayerBlocks(BlockPos playerPos, Player entity) {
      List<BlockPos> playerBlocks = new ArrayList<>();
      if (this.extendConfig.getValue()) {
         playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
      } else {
         playerBlocks.add(playerPos);
      }

      return playerBlocks;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            set.getValue().setState(false);
            int boxAlpha = (int)(40.0 * set.getValue().getFactor());
            int lineAlpha = (int)(100.0 * set.getValue().getFactor());
            Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
            RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
         }

         RenderBuffers.postRender();
         if (this.placements.isEmpty()) {
            return;
         }

         for (BlockPos pos : this.placements) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(pos, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }

   public static enum Timing {
      VANILLA,
      SEQUENTIAL;
   }
}
