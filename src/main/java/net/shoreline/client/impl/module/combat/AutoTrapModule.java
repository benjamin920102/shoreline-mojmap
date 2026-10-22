package net.shoreline.client.impl.module.combat;

import com.google.common.collect.Lists;
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
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
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
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

public final class AutoTrapModule extends ObsidianPlacerModule {
   private static AutoTrapModule INSTANCE;
   Config<Boolean> multitaskConfig = this.register(new BooleanConfig("Multitask", "Allows placing while eating", true));
   Config<Float> placeRangeConfig = this.register(new NumberConfig<>("PlaceRange", "The placement range for trap", 0.0F, 4.0F, 6.0F));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   Config<Boolean> attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of trap ", true));
   Config<Boolean> extendConfig = this.register(new BooleanConfig("Extend", "Extends trap if the player is not in the center of a block", true));
   Config<Boolean> supportConfig = this.register(new BooleanConfig("Support", "Creates a floor for the trap if there is none", false));
   Config<Boolean> headConfig = this.register(new BooleanConfig("Head", "Place a block at targets head", true));
   Config<Boolean> antiStepConfig = this.register(new BooleanConfig("PreventStep", "Prevents target from stepping out of the trap", false));
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

   public AutoTrapModule() {
      super("AutoTrap", "Fully traps enemies with blocks", ModuleCategory.COMBAT, 900);
      INSTANCE = this;
   }

   public static AutoTrapModule getInstance() {
      return INSTANCE;
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
      if (!this.multitaskConfig.getValue() && mc.player.isUsingItem()) {
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
               Player trapTarget = this.getTrapTarget();
               if (trapTarget == null) {
                  this.surround.clear();
                  this.placements.clear();
               } else {
                  BlockPos targetBlockPos = PositionUtil.getRoundedBlockPos(trapTarget.getX(), trapTarget.getY(), trapTarget.getZ());
                  this.surround = this.getSurround(targetBlockPos, trapTarget);
                  if (!this.surround.isEmpty()) {
                     if (this.attackConfig.getValue()) {
                        this.attackBlockingCrystals(this.surround);
                     }

                     this.placements = this.getPlacementsFromSurround(this.surround);
                     if (this.placements.isEmpty()) {
                        if (this.autoDisableConfig.getValue()) {
                           this.disable();
                        }
                     } else {
                        if (this.supportConfig.getValue()) {
                           for (BlockPos block : new ArrayList<>(this.placements)) {
                              if (block.getY() <= targetBlockPos.getY()) {
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
                           this.blocksPlaced++;
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
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
   }

   private Player getTrapTarget() {
      List<Entity> entities = Lists.newArrayList(mc.level.entitiesForRendering());
      return (Player)entities.stream()
         .filter(e -> e instanceof Player && e.isAlive() && mc.player != e && !Managers.SOCIAL.isFriend(e.getName()))
         .filter(e -> mc.player.distanceToSqr(e) <= ((NumberConfig)this.placeRangeConfig).getValueSq())
         .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e)))
         .orElse(null);
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
            if (this.antiStepConfig.getValue()) {
               BlockPos antiStepPos = pos.relative(Direction.UP, 3);
               headBlocks.add(antiStepPos);
            }
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
}
