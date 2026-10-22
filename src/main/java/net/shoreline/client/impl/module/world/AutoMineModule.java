package net.shoreline.client.impl.module.world;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BlockListConfig;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.AttackBlockEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.CombatModule;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.combat.AutoCrystalModule;
import net.shoreline.client.impl.module.combat.SurroundModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;
import org.jetbrains.annotations.NotNull;

public class AutoMineModule extends CombatModule {
   private static AutoMineModule INSTANCE;
   Config<Boolean> autoConfig = this.register(new BooleanConfig("Auto", "Automatically mines nearby players feet", false));
   Config<AutoMineModule.Selection> selectionConfig = this.register(
      new EnumConfig<>(
         "Selection", "The selection of blocks mine", AutoMineModule.Selection.ALL, AutoMineModule.Selection.values(), () -> this.autoConfig.getValue()
      )
   );
   Config<List<Block>> whitelistConfig = this.register(new BlockListConfig<>("Whitelist", "Valid block whitelist", Blocks.OBSIDIAN, Blocks.ENDER_CHEST));
   Config<List<Block>> blacklistConfig = this.register(new BlockListConfig<>("Blacklist", "Valid block blacklist", Blocks.SHULKER_BOX));
   Config<Boolean> avoidSelfConfig = this.register(
      new BooleanConfig("AvoidSelf", "Avoids mining blocks in your surround", false, () -> this.autoConfig.getValue())
   );
   Config<Boolean> strictDirectionConfig = this.register(
      new BooleanConfig("StrictDirection", "Only mines on visible faces", false, () -> this.autoConfig.getValue())
   );
   Config<Float> enemyRangeConfig = this.register(
      new NumberConfig<>("EnemyRange", "Range to search for targets", 1.0F, 5.0F, 10.0F, () -> this.autoConfig.getValue())
   );
   Config<Boolean> antiCrawlConfig = this.register(new BooleanConfig("AntiCrawl", "Attempts to stop player from crawling", false));
   Config<Boolean> headConfig = this.register(new BooleanConfig("TargetBody", "Attempts to mine players face blocks", false, () -> this.autoConfig.getValue()));
   Config<Boolean> aboveHeadConfig = this.register(
      new BooleanConfig("TargetHead", "Attempts to mine above players head", false, () -> this.autoConfig.getValue())
   );
   Config<Boolean> doubleBreakConfig = this.register(new BooleanConfig("DoubleBreak", "Allows you to mine two blocks at once", false));
   Config<Integer> mineTicksConfig = this.register(
      new NumberConfig<>("MiningTicks", "The max number of ticks to hold a pickaxe for the packet mine", 5, 20, 60, () -> this.doubleBreakConfig.getValue())
   );
   Config<AutoMineModule.RemineMode> remineConfig = this.register(
      new EnumConfig<>("Remine", "Remines already mined blocks", AutoMineModule.RemineMode.NORMAL, AutoMineModule.RemineMode.values())
   );
   Config<Boolean> packetInstantConfig = this.register(
      new BooleanConfig("Fast", "Instant mines on packet", false, () -> this.remineConfig.getValue() == AutoMineModule.RemineMode.INSTANT)
   );
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to mine blocks", 0.1F, 4.0F, 6.0F));
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The speed to mine blocks", 0.1F, 1.0F, 1.0F));
   Config<AutoMineModule.Swap> swapConfig = this.register(
      new EnumConfig<>("AutoSwap", "Swaps to the best tool once the mining is complete", AutoMineModule.Swap.SILENT, AutoMineModule.Swap.values())
   );
   Config<Boolean> swapBeforeConfig = this.register(
      new BooleanConfig("SwapBefore", "Swaps before fully done mining", false, () -> this.swapConfig.getValue() != AutoMineModule.Swap.OFF)
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates when mining the block", true));
   Config<Boolean> switchResetConfig = this.register(new BooleanConfig("SwitchReset", "Resets mining after switching items", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Uses grim block breaking speeds", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Allows mining on new grim servers", false, () -> this.grimConfig.getValue()));
   Config<Color> colorConfig = this.register(new ColorConfig("MineColor", "The mine render color", Color.RED, false, false));
   Config<Color> colorDoneConfig = this.register(new ColorConfig("DoneColor", "The done render color", Color.GREEN, false, false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   Config<Boolean> smoothColorConfig = this.register(new BooleanConfig("SmoothColor", "Interpolates from start to done color", false, () -> false));
   Config<Boolean> debugConfig = new BooleanConfig("Debug", "Renders the debug mode for mines", false);
   Config<Color> packetColorConfig = new ColorConfig(
      "PacketColor", "The packet mine render color", new Color(0, 0, 255), false, false, () -> this.debugConfig.getValue()
   );
   Config<Color> instantColorConfig = new ColorConfig(
      "InstantColor", "The instant mine render color", new Color(255, 0, 255), false, false, () -> this.debugConfig.getValue()
   );
   Config<Boolean> debugTicksConfig = new BooleanConfig("Debug-Ticks", "Shows the mining ticks", false, () -> this.debugConfig.getValue());
   private Player playerTarget;
   private AutoMineModule.MineData packetMine;
   private AutoMineModule.MineData instantMine;
   private boolean packetSwapBack;
   private boolean manualOverride;
   private final Timer remineTimer = new CacheTimer();
   private boolean changedInstantMine;
   private boolean waitForPacketMine;
   private boolean packetMineStuck;
   private boolean antiCrawlOverride;
   private int antiCrawlTicks;
   private final Queue<AutoMineModule.MineData> autoMineQueue = new ArrayDeque<>();
   private int autoMineTickDelay;
   private AutoMineModule.MineAnimation packetMineAnim = new AutoMineModule.MineAnimation(AutoMineModule.MineData.empty(), new Animation(true, 200.0F));
   private AutoMineModule.MineAnimation instantMineAnim = new AutoMineModule.MineAnimation(AutoMineModule.MineData.empty(), new Animation(true, 200.0F));

   public AutoMineModule() {
      super("AutoMine", "Automatically mines blocks", ModuleCategory.WORLD, 900);
      INSTANCE = this;
      this.register(this.debugConfig);
      this.register(this.packetColorConfig);
      this.register(this.instantColorConfig);
      this.register(this.debugTicksConfig);
   }

   public static AutoMineModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.instantMine != null ? String.format("%.1f", Math.min(this.instantMine.getBlockDamage(), 1.0F)) : super.getModuleData();
   }

   @Override
   public void onDisable() {
      this.autoMineQueue.clear();
      this.playerTarget = null;
      this.packetMine = null;
      if (this.instantMine != null) {
         this.abortMining(this.instantMine);
         this.instantMine = null;
      }

      this.packetMineAnim = new AutoMineModule.MineAnimation(AutoMineModule.MineData.empty(), new Animation(true, 200.0F));
      this.instantMineAnim = new AutoMineModule.MineAnimation(AutoMineModule.MineData.empty(), new Animation(true, 200.0F));
      this.autoMineTickDelay = 0;
      this.antiCrawlTicks = 0;
      this.manualOverride = false;
      this.antiCrawlOverride = false;
      this.waitForPacketMine = false;
      this.packetMineStuck = false;
      if (this.packetSwapBack) {
         Managers.INVENTORY.syncToClient();
         this.packetSwapBack = false;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && event.getStage() == StageEvent.EventStage.PRE) {
         Player currentTarget = this.getClosestPlayer(this.enemyRangeConfig.getValue().floatValue());
         boolean targetChanged = this.playerTarget != null && this.playerTarget != currentTarget;
         this.playerTarget = currentTarget;
         if (this.isInstantMineComplete()) {
            if (this.changedInstantMine) {
               this.changedInstantMine = false;
            }

            if (this.waitForPacketMine) {
               this.waitForPacketMine = false;
            }
         }

         this.autoMineTickDelay--;
         this.antiCrawlTicks--;
         if (this.packetMine != null && this.packetMine.getTicksMining() > this.mineTicksConfig.getValue()) {
            this.packetMineStuck = true;
            this.packetMineAnim.animation.setState(false);
            if (this.packetSwapBack) {
               Managers.INVENTORY.syncToClient();
               this.packetSwapBack = false;
            }

            this.packetMine = null;
            if (!this.isInstantMineComplete()) {
               this.waitForPacketMine = true;
            }
         }

         if (this.packetMine != null) {
            float damageDelta = SpeedmineModule.getInstance().calcBlockBreakingDelta(this.packetMine.getState(), mc.level, this.packetMine.getPos());
            this.packetMine.addBlockDamage(damageDelta);
            int slot = this.packetMine.getBestSlot();
            float damageDone = this.packetMine.getBlockDamage() + (!this.swapBeforeConfig.getValue() && !this.packetMineStuck ? 0.0F : damageDelta);
            if (damageDone >= 1.0F && slot != -1 && !this.checkMultitask()) {
               Managers.INVENTORY.setSlot(slot);
               this.packetSwapBack = true;
               if (this.packetMineStuck) {
                  this.packetMineStuck = false;
               }
            }
         }

         if (this.packetSwapBack) {
            if (this.packetMine != null && this.canMine(this.packetMine.getState())) {
               this.packetMine.markAttemptedMine();
            } else {
               Managers.INVENTORY.syncToClient();
               this.packetSwapBack = false;
               this.packetMineAnim.animation.setState(false);
               this.packetMine = null;
               if (!this.isInstantMineComplete()) {
                  this.waitForPacketMine = true;
               }
            }
         }

         if (this.instantMine != null) {
            double distance = mc.player.getEyePosition().distanceToSqr(this.instantMine.getPos().getCenter());
            if (distance > ((NumberConfig)this.rangeConfig).getValueSq() || this.instantMine.getTicksMining() > this.mineTicksConfig.getValue()) {
               this.abortMining(this.instantMine);
               this.instantMineAnim.animation.setState(false);
               this.instantMine = null;
            }
         }

         if (this.instantMine != null) {
            float damageDelta = SpeedmineModule.getInstance().calcBlockBreakingDelta(this.instantMine.getState(), mc.level, this.instantMine.getPos());
            this.instantMine.addBlockDamage(damageDelta);
            if (this.instantMine.getBlockDamage() >= this.speedConfig.getValue()) {
               boolean canMine = this.canMine(this.instantMine.getState());
               boolean canPlace = mc.level.isUnobstructed(this.instantMine.getState(), this.instantMine.getPos(), CollisionContext.empty());
               if (canMine) {
                  this.instantMine.markAttemptedMine();
               } else {
                  this.instantMine.resetMiningTicks();
                  if (this.remineConfig.getValue() == AutoMineModule.RemineMode.NORMAL || this.remineConfig.getValue() == AutoMineModule.RemineMode.FAST) {
                     this.instantMine.setTotalBlockDamage(0.0F, 0.0F);
                  }

                  if (this.manualOverride) {
                     this.manualOverride = false;
                     this.abortMining(this.instantMine);
                     this.instantMineAnim.animation.setState(false);
                     this.instantMine = null;
                  }
               }

               boolean passedRemine = this.remineConfig.getValue() == AutoMineModule.RemineMode.INSTANT || this.remineTimer.passed(500);
               if (this.instantMine != null
                  && (
                     this.remineConfig.getValue() == AutoMineModule.RemineMode.INSTANT
                           && this.packetInstantConfig.getValue()
                           && this.packetMine == null
                           && canPlace
                        || canMine && passedRemine
                  )
                  && (!this.checkMultitask() || this.multitaskConfig.getValue() || this.swapConfig.getValue() == AutoMineModule.Swap.OFF)) {
                  this.stopMining(this.instantMine);
                  this.remineTimer.reset();
                  if (AutoCrystalModule.getInstance().isEnabled() && AutoCrystalModule.getInstance().shouldPreForcePlace()) {
                     AutoCrystalModule.getInstance().placeCrystalForTarget(this.playerTarget, this.instantMine.getPos().below());
                  }

                  if (this.remineConfig.getValue() == AutoMineModule.RemineMode.FAST) {
                     this.startMining(this.instantMine);
                  }
               }
            }
         }

         if (this.manualOverride && (this.instantMine == null || this.instantMine.getGoal() != AutoMineModule.MiningGoal.MANUAL)) {
            this.manualOverride = false;
         }

         if (this.antiCrawlOverride && (this.instantMine == null || this.instantMine.getGoal() != AutoMineModule.MiningGoal.PREVENT_CRAWL)) {
            this.antiCrawlOverride = false;
         }

         if (this.autoConfig.getValue()) {
            if (!this.autoMineQueue.isEmpty() && this.autoMineTickDelay <= 0) {
               AutoMineModule.MineData nextMine = this.autoMineQueue.poll();
               if (nextMine != null) {
                  this.startMining(nextMine);
                  this.autoMineTickDelay = 5;
               }
            }

            BlockPos antiCrawlPos = this.getAntiCrawlPos(this.playerTarget);
            if (this.antiCrawlOverride) {
               if (mc.player.getPose().equals(Pose.SWIMMING)) {
                  this.antiCrawlTicks = 10;
               }

               if (this.antiCrawlTicks <= 0 || !this.isInstantMineComplete() && antiCrawlPos != null && !this.instantMine.getPos().equals(antiCrawlPos)) {
                  this.antiCrawlOverride = false;
               }
            }

            if (this.autoMineQueue.isEmpty() && !this.manualOverride && !this.antiCrawlOverride) {
               if (this.antiCrawlConfig.getValue() && mc.player.getPose().equals(Pose.SWIMMING) && antiCrawlPos != null) {
                  AutoMineModule.MineData data = new AutoMineModule.MineData(
                     antiCrawlPos,
                     this.strictDirectionConfig.getValue() ? Managers.INTERACT.getInteractDirection(antiCrawlPos, false) : Direction.UP,
                     AutoMineModule.MiningGoal.PREVENT_CRAWL
                  );
                  if (this.isInstantMineComplete() || !this.instantMine.equals(data)) {
                     this.startAutoMine(data);
                     this.antiCrawlOverride = true;
                  }
               } else if (this.playerTarget != null && !targetChanged) {
                  BlockPos targetPos = EntityUtil.getRoundedBlockPos(this.playerTarget);
                  boolean bedrockPhased = PositionUtil.isBedrock(this.playerTarget.getBoundingBox(), targetPos) && !this.playerTarget.isVisuallyCrawling();
                  if (!this.isInstantMineComplete() && this.checkDataY(this.instantMine, targetPos, bedrockPhased)) {
                     this.abortMining(this.instantMine);
                     this.instantMineAnim.animation.setState(false);
                     this.instantMine = null;
                  } else if (this.packetMine != null && this.checkDataY(this.packetMine, targetPos, bedrockPhased)) {
                     this.packetMineAnim.animation.setState(false);
                     if (this.packetSwapBack) {
                        Managers.INVENTORY.syncToClient();
                        this.packetSwapBack = false;
                     }

                     this.packetMine = null;
                     this.waitForPacketMine = false;
                  } else {
                     List<BlockPos> phasedBlocks = this.getPhaseBlocks(this.playerTarget, targetPos, bedrockPhased);
                     if (!phasedBlocks.isEmpty()) {
                        BlockPos pos1 = phasedBlocks.removeFirst();
                        AutoMineModule.MineData bestMine = new AutoMineModule.MineData(
                           pos1, this.strictDirectionConfig.getValue() ? Managers.INTERACT.getInteractDirection(pos1, false) : Direction.UP
                        );
                        if (this.packetMine == null && this.doubleBreakConfig.getValue() || this.isInstantMineComplete()) {
                           this.startAutoMine(bestMine);
                        }
                     } else {
                        List<BlockPos> miningBlocks = this.getMiningBlocks(this.playerTarget, targetPos, bedrockPhased);
                        AutoMineModule.MineData bestMine = this.getInstantMine(miningBlocks, bedrockPhased);
                        if (bestMine != null
                           && (this.packetMine == null && !this.changedInstantMine && this.doubleBreakConfig.getValue() || this.isInstantMineComplete())) {
                           this.startAutoMine(bestMine);
                        }
                     }
                  }
               } else {
                  if (!this.isInstantMineComplete() && this.instantMine.getGoal() == AutoMineModule.MiningGoal.MINING_ENEMY) {
                     this.abortMining(this.instantMine);
                     this.instantMineAnim.animation.setState(false);
                     this.instantMine = null;
                  }

                  if (this.packetMine != null && this.packetMine.getGoal() == AutoMineModule.MiningGoal.MINING_ENEMY) {
                     this.packetMineAnim.animation.setState(false);
                     if (this.packetSwapBack) {
                        Managers.INVENTORY.syncToClient();
                        this.packetSwapBack = false;
                     }

                     this.packetMine = null;
                     this.waitForPacketMine = false;
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator()) {
         event.cancel();
         if (event.getState().getBlock().defaultDestroyTime() != -1.0F && this.canMine(event.getState()) && !this.isMining(event.getPos())) {
            AutoMineModule.MineData data = new AutoMineModule.MineData(event.getPos(), event.getDirection(), AutoMineModule.MiningGoal.MANUAL);
            if (this.instantMine != null && this.instantMine.getGoal() == AutoMineModule.MiningGoal.MINING_ENEMY
               || this.packetMine != null && this.packetMine.getGoal() == AutoMineModule.MiningGoal.MINING_ENEMY) {
               this.manualOverride = true;
            }

            if (!this.doubleBreakConfig.getValue()) {
               this.instantMine = data;
               this.startMining(this.instantMine);
               mc.player.swing(InteractionHand.MAIN_HAND, false);
            } else {
               boolean updateChanged = false;
               if (!this.isInstantMineComplete() && !this.changedInstantMine) {
                  if (this.packetMine == null) {
                     this.packetMine = this.instantMine.copy();
                     this.packetMineAnim = new AutoMineModule.MineAnimation(this.packetMine, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
                  } else {
                     updateChanged = true;
                  }
               }

               this.instantMine = data;
               this.startMining(this.instantMine);
               mc.player.swing(InteractionHand.MAIN_HAND, false);
               if (updateChanged) {
                  this.changedInstantMine = true;
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket && this.switchResetConfig.getValue() && this.instantMine != null) {
         this.instantMine.setTotalBlockDamage(0.0F, 0.0F);
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet
         && this.canMine(packet.getBlockState())
         && this.antiCrawlOverride
         && packet.getPos().equals(this.getAntiCrawlPos(this.playerTarget))) {
         this.antiCrawlTicks = 10;
      }
   }

   public void startAutoMine(AutoMineModule.MineData data) {
      if (this.canMine(data.getState()) && !this.isMining(data.getPos())) {
         if (!this.doubleBreakConfig.getValue()) {
            this.instantMine = data;
            this.autoMineQueue.offer(data);
         } else if ((!this.changedInstantMine || this.isInstantMineComplete()) && !this.waitForPacketMine) {
            boolean updateChanged = false;
            if (!this.isInstantMineComplete() && !this.changedInstantMine) {
               if (this.packetMine == null) {
                  this.packetMine = this.instantMine.copy();
                  this.packetMineAnim = new AutoMineModule.MineAnimation(this.packetMine, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
               } else {
                  updateChanged = true;
               }
            }

            this.instantMine = data;
            this.autoMineQueue.offer(data);
            if (updateChanged) {
               this.changedInstantMine = true;
            }
         }
      }
   }

   public AutoMineModule.MineData getInstantMine(List<BlockPos> miningBlocks, boolean bedrockPhased) {
      PriorityQueue<AutoMineModule.MineData> validInstantMines = new PriorityQueue<>();

      for (BlockPos blockPos : miningBlocks) {
         BlockState state1 = mc.level.getBlockState(blockPos);
         if (this.isAutoMineBlock(state1.getBlock())) {
            double dist = mc.player.getEyePosition().distanceToSqr(blockPos.getCenter());
            if (!(dist > ((NumberConfig)this.rangeConfig).getValueSq())) {
               BlockState state2 = mc.level.getBlockState(blockPos.below());
               if (bedrockPhased || state2.is(Blocks.OBSIDIAN) || state2.is(Blocks.BEDROCK)) {
                  Direction direction = this.strictDirectionConfig.getValue() ? Managers.INTERACT.getInteractDirection(blockPos, false) : Direction.UP;
                  validInstantMines.add(new AutoMineModule.MineData(blockPos, direction));
               }
            }
         }
      }

      return validInstantMines.isEmpty() ? null : validInstantMines.peek();
   }

   public List<BlockPos> getPhaseBlocks(Player player, BlockPos playerPos, boolean targetBedrockPhased) {
      List<BlockPos> phaseBlocks = PositionUtil.getAllInBox(
         player.getBoundingBox(), targetBedrockPhased && this.headConfig.getValue() ? playerPos.above() : playerPos
      );
      phaseBlocks.removeIf(p -> {
         BlockState state = mc.level.getBlockState(p);
         if (this.isAutoMineBlock(state.getBlock()) && this.canMine(state) && !this.isMining(p)) {
            double dist = mc.player.getEyePosition().distanceToSqr(p.getCenter());
            return dist > ((NumberConfig)this.rangeConfig).getValueSq() ? true : this.avoidSelfConfig.getValue() && this.intersectsPlayer(p);
         } else {
            return true;
         }
      });
      if (targetBedrockPhased && this.aboveHeadConfig.getValue()) {
         phaseBlocks.add(playerPos.above(2));
      }

      return phaseBlocks;
   }

   public List<BlockPos> getMiningBlocks(Player player, BlockPos playerPos, boolean bedrockPhased) {
      List<BlockPos> surroundingBlocks = SurroundModule.getInstance().getSurroundNoDown(player, this.rangeConfig.getValue());
      List<BlockPos> miningBlocks;
      if (bedrockPhased) {
         List<BlockPos> facePlaceBlocks = new ArrayList<>();
         if (this.headConfig.getValue()) {
            facePlaceBlocks.addAll(surroundingBlocks.stream().map(BlockPos::above).toList());
         }

         BlockState belowFeet = mc.level.getBlockState(playerPos.below());
         if (this.canMine(belowFeet)) {
            facePlaceBlocks.add(playerPos.below());
         }

         miningBlocks = facePlaceBlocks;
      } else {
         miningBlocks = surroundingBlocks;
      }

      miningBlocks.removeIf(p -> this.avoidSelfConfig.getValue() && this.intersectsPlayer(p));
      return miningBlocks;
   }

   private BlockPos getAntiCrawlPos(Player playerTarget) {
      if (!mc.player.onGround()) {
         return null;
      } else {
         BlockPos crawlingPos = EntityUtil.getRoundedBlockPos(mc.player);
         boolean playerBelow = playerTarget != null && EntityUtil.getRoundedBlockPos(playerTarget).getY() < crawlingPos.getY();
         if (playerBelow) {
            BlockState state = mc.level.getBlockState(crawlingPos.below());
            if (this.isAutoMineBlock(state.getBlock()) && this.canMine(state)) {
               return crawlingPos.below();
            }
         } else {
            BlockState state = mc.level.getBlockState(crawlingPos.above());
            if (this.isAutoMineBlock(state.getBlock()) && this.canMine(state)) {
               return crawlingPos.above();
            }
         }

         return null;
      }
   }

   private boolean checkDataY(AutoMineModule.MineData data, BlockPos targetPos, boolean bedrockPhased) {
      return data.getGoal() == AutoMineModule.MiningGoal.MINING_ENEMY && !bedrockPhased && data.getPos().getY() != targetPos.getY();
   }

   private boolean intersectsPlayer(BlockPos pos) {
      List<BlockPos> playerBlocks = SurroundModule.getInstance().getPlayerBlocks(mc.player);
      List<BlockPos> surroundingBlocks = SurroundModule.getInstance().getSurroundNoDown(mc.player);
      return playerBlocks.contains(pos) || surroundingBlocks.contains(pos);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator()) {
         RenderBuffers.preRender();
         if (this.instantMineAnim != null && this.instantMineAnim.animation().getFactor() > 0.01F) {
            this.renderMiningData(event.getMatrices(), event.getTickDelta(), this.instantMineAnim, true);
         }

         if (this.doubleBreakConfig.getValue() && this.packetMineAnim != null && this.packetMineAnim.animation().getFactor() > 0.01F) {
            this.renderMiningData(event.getMatrices(), event.getTickDelta(), this.packetMineAnim, false);
         }

         RenderBuffers.postRender();
      }
   }

   public void renderMiningData(PoseStack matrixStack, float tickDelta, AutoMineModule.MineAnimation mineAnimation, boolean instantMine) {
      AutoMineModule.MineData data = mineAnimation.data();
      Animation animation = mineAnimation.animation();
      int boxAlpha = (int)(40.0 * animation.getFactor());
      int lineAlpha = (int)(100.0 * animation.getFactor());
      int boxColor;
      int lineColor;
      if (this.debugConfig.getValue()) {
         boxColor = instantMine ? ((ColorConfig)this.instantColorConfig).getRgb(boxAlpha) : ((ColorConfig)this.packetColorConfig).getRgb(boxAlpha);
         lineColor = instantMine ? ((ColorConfig)this.instantColorConfig).getRgb(lineAlpha) : ((ColorConfig)this.packetColorConfig).getRgb(lineAlpha);
      } else if (this.smoothColorConfig.getValue()) {
         boxColor = !this.canMine(data.getState())
            ? ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha)
            : ColorUtil.interpolateColor(
                  Math.min(data.getBlockDamage(), 1.0F),
                  ((ColorConfig)this.colorDoneConfig).getValue(boxAlpha),
                  ((ColorConfig)this.colorConfig).getValue(boxAlpha)
               )
               .getRGB();
         lineColor = !this.canMine(data.getState())
            ? ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha)
            : ColorUtil.interpolateColor(
                  Math.min(data.getBlockDamage(), 1.0F),
                  ((ColorConfig)this.colorDoneConfig).getValue(lineAlpha),
                  ((ColorConfig)this.colorConfig).getValue(lineAlpha)
               )
               .getRGB();
      } else {
         boxColor = !(data.getBlockDamage() >= 0.95F) && this.canMine(data.getState())
            ? ((ColorConfig)this.colorConfig).getRgb(boxAlpha)
            : ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha);
         lineColor = !(data.getBlockDamage() >= 0.95F) && this.canMine(data.getState())
            ? ((ColorConfig)this.colorConfig).getRgb(lineAlpha)
            : ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha);
      }

      BlockPos mining = data.getPos();
      VoxelShape outlineShape = Shapes.block();
      if (!instantMine || data.getBlockDamage() < this.speedConfig.getValue()) {
         outlineShape = data.getState().getShape(mc.level, mining);
         outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
      }

      AABB render1 = outlineShape.bounds();
      Vec3 center = render1.move(mining).getCenter();
      float total = instantMine ? this.speedConfig.getValue() : 1.0F;
      float scale = (!instantMine || !(data.getBlockDamage() >= this.speedConfig.getValue())) && this.canMine(data.getState())
         ? Mth.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * tickDelta) / total, 0.0F, 1.0F)
         : 1.0F;
      double dx = (render1.maxX - render1.minX) / 2.0;
      double dy = (render1.maxY - render1.minY) / 2.0;
      double dz = (render1.maxZ - render1.minZ) / 2.0;
      AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
      RenderManager.renderBox(matrixStack, scaled, boxColor);
      RenderManager.renderBoundingBox(matrixStack, scaled, 1.5F, lineColor);
      if (this.debugTicksConfig.getValue()) {
         RenderManager.renderSign(String.valueOf(data.getTicksMining()), center, -1);
      }
   }

   public void startMining(AutoMineModule.MineData data) {
      if (this.rotateConfig.getValue()) {
         float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
         if (this.grimConfig.getValue()) {
            this.setRotationSilent(rotations[0], rotations[1]);
         } else {
            this.setRotation(rotations[0], rotations[1]);
         }
      }

      if (this.doubleBreakConfig.getValue()) {
         if (this.grimNewConfig.getValue()) {
            if (!AnticheatModule.getInstance().getMiningFix()) {
               Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
               Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
               Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
               Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }

            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         } else {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }
      } else {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
      }

      if (this.rotateConfig.getValue() && this.grimConfig.getValue()) {
         Managers.ROTATION.setRotationSilentSync();
      }

      this.instantMineAnim = new AutoMineModule.MineAnimation(data, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
   }

   public void abortMining(AutoMineModule.MineData data) {
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public void stopMining(AutoMineModule.MineData data) {
      if (this.rotateConfig.getValue()) {
         float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
         if (this.grimConfig.getValue()) {
            this.setRotationSilent(rotations[0], rotations[1]);
         } else {
            this.setRotation(rotations[0], rotations[1]);
         }
      }

      int slot = data.getBestSlot();
      if (slot != -1) {
         this.swapTo(slot);
      }

      this.stopMiningInternal(data);
      if (slot != -1) {
         this.swapSync(slot);
      }

      if (this.rotateConfig.getValue() && this.grimConfig.getValue()) {
         Managers.ROTATION.setRotationSilentSync();
      }
   }

   private void stopMiningInternal(AutoMineModule.MineData data) {
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public boolean isInstantMineComplete() {
      return this.instantMine == null || this.instantMine.getBlockDamage() >= this.speedConfig.getValue() && !this.canMine(this.instantMine.getState());
   }

   public BlockPos getMiningBlock() {
      if (this.instantMine != null) {
         double damage = this.instantMine.getBlockDamage() / this.speedConfig.getValue();
         if (damage > 0.75) {
            return this.instantMine.getPos();
         }
      }

      return null;
   }

   private void swapTo(int slot) {
      switch ((AutoMineModule.Swap)this.swapConfig.getValue()) {
         case NORMAL:
            Managers.INVENTORY.setClientSlot(slot);
            break;
         case SILENT:
            Managers.INVENTORY.setSlot(slot);
            break;
         case SILENT_ALT:
            Managers.INVENTORY.setSlotAlt(slot);
      }
   }

   private void swapSync(int slot) {
      switch ((AutoMineModule.Swap)this.swapConfig.getValue()) {
         case SILENT:
            Managers.INVENTORY.syncToClient();
            break;
         case SILENT_ALT:
            Managers.INVENTORY.setSlotAlt(slot);
      }
   }

   public boolean isSilentSwapping() {
      return this.packetSwapBack;
   }

   private boolean isMining(BlockPos blockPos) {
      return this.instantMine != null && this.instantMine.getPos().equals(blockPos) || this.packetMine != null && this.packetMine.getPos().equals(blockPos);
   }

   private boolean isAutoMineBlock(Block block) {
      if (BlastResistantBlocks.isUnbreakable(block)) {
         return false;
      } else {
         return switch ((AutoMineModule.Selection)this.selectionConfig.getValue()) {
            case WHITELIST -> ((BlockListConfig)this.whitelistConfig).contains(block);
            case BLACKLIST -> !((BlockListConfig)this.blacklistConfig).contains(block);
            case ALL -> true;
         };
      }
   }

   public boolean canMine(BlockState state) {
      return !state.isAir() && state.getFluidState().isEmpty();
   }

   public record MineAnimation(AutoMineModule.MineData data, Animation animation) {
   }

   public static class MineData implements Comparable<AutoMineModule.MineData> {
      private final BlockPos pos;
      private final Direction direction;
      private final AutoMineModule.MiningGoal goal;
      private int ticksMining;
      private float blockDamage;
      private float lastDamage;

      public MineData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
         this.goal = AutoMineModule.MiningGoal.MINING_ENEMY;
      }

      public MineData(BlockPos pos, Direction direction, AutoMineModule.MiningGoal goal) {
         this.pos = pos;
         this.direction = direction;
         this.goal = goal;
      }

      private double getPriority() {
         double dist = Globals.mc.player.getEyePosition().distanceToSqr(this.pos.below().getCenter());
         return dist <= AutoCrystalModule.getInstance().getPlaceRange() ? 10.0 : 0.0;
      }

      public int compareTo(@NotNull AutoMineModule.MineData o) {
         return Double.compare(this.getPriority(), o.getPriority());
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof AutoMineModule.MineData d && d.getPos().equals(this.pos);
      }

      public void resetMiningTicks() {
         this.ticksMining = 0;
      }

      public void markAttemptedMine() {
         this.ticksMining++;
      }

      public void addBlockDamage(float blockDamage) {
         this.lastDamage = this.blockDamage;
         this.blockDamage += blockDamage;
      }

      public void setTotalBlockDamage(float blockDamage, float lastDamage) {
         this.blockDamage = blockDamage;
         this.lastDamage = lastDamage;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public AutoMineModule.MiningGoal getGoal() {
         return this.goal;
      }

      public int getTicksMining() {
         return this.ticksMining;
      }

      public float getBlockDamage() {
         return this.blockDamage;
      }

      public float getLastDamage() {
         return this.lastDamage;
      }

      public static AutoMineModule.MineData empty() {
         return new AutoMineModule.MineData(BlockPos.ZERO, Direction.UP);
      }

      public AutoMineModule.MineData copy() {
         AutoMineModule.MineData data = new AutoMineModule.MineData(this.pos, this.direction, this.goal);
         data.setTotalBlockDamage(this.blockDamage, this.lastDamage);
         return data;
      }

      public BlockState getState() {
         return Globals.mc.level.getBlockState(this.pos);
      }

      public int getBestSlot() {
         return AutoToolModule.getInstance().getBestToolNoFallback(this.getState());
      }
   }

   public static enum MiningGoal {
      MANUAL,
      MINING_ENEMY,
      PREVENT_CRAWL;
   }

   public static enum RemineMode {
      INSTANT,
      NORMAL,
      FAST;
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }
}
