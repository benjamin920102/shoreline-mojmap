package net.shoreline.client.impl.module.world;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class NukerModule extends CombatModule {
   Config<NukerModule.Selection> selectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of blocks to use for scaffold", NukerModule.Selection.ALL, NukerModule.Selection.values())
   );
   Config<List<Block>> whitelistConfig = this.register(new BlockListConfig<>("Whitelist", "Valid block whitelist", Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN));
   Config<List<Block>> blacklistConfig = this.register(new BlockListConfig<>("Blacklist", "Valid block blacklist"));
   Config<NukerModule.NukeMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The nuker selection mode", NukerModule.NukeMode.SPHERE, NukerModule.NukeMode.values())
   );
   Config<Boolean> flattenConfig = this.register(
      new BooleanConfig("Flatten", "Only clears above the player y-level", false, () -> this.modeConfig.getValue() == NukerModule.NukeMode.SPHERE)
   );
   Config<Boolean> strictDirectionConfig = this.register(new BooleanConfig("StrictDirection", "Only mines on visible faces", false));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to mine blocks", 0.0F, 4.0F, 6.0F));
   Config<Boolean> doubleBreakConfig = this.register(new BooleanConfig("DoubleBreak", "Allows you to mine two blocks at once", false));
   Config<Integer> mineTicksConfig = this.register(
      new NumberConfig<>("MiningTicks", "The max number of ticks to hold a pickaxe for the packet mine", 5, 20, 60, () -> this.doubleBreakConfig.getValue())
   );
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The speed to mine blocks", 0.1F, 1.0F, 1.0F));
   Config<NukerModule.Swap> swapConfig = this.register(
      new EnumConfig<>("AutoSwap", "Swaps to the best tool once the mining is complete", NukerModule.Swap.SILENT, NukerModule.Swap.values())
   );
   Config<Boolean> swapBeforeConfig = this.register(
      new BooleanConfig("SwapBefore", "Swaps before mining", false, () -> this.swapConfig.getValue() != NukerModule.Swap.OFF)
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates when mining the block", true));
   Config<Boolean> switchResetConfig = this.register(new BooleanConfig("SwitchReset", "Resets mining after switching items", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Uses grim block breaking speeds", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Allows mining on new grim servers", false, () -> this.grimConfig.getValue()));
   Config<Color> colorConfig = this.register(new ColorConfig("MineColor", "The mine render color", Color.RED, false, false));
   Config<Color> colorDoneConfig = this.register(new ColorConfig("DoneColor", "The done render color", Color.GREEN, false, false));
   Config<Boolean> debugTicksConfig = this.register(new BooleanConfig("Debug-Ticks", "Shows the mining ticks", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   Config<Boolean> smoothColorConfig = this.register(new BooleanConfig("SmoothColor", "Interpolates from start to done color", false, () -> false));
   private NukerModule.MineData packetMine;
   private NukerModule.MineData instantMine;
   private boolean packetSwapBack;
   private boolean changedInstantMine;
   private boolean waitForPacketMine;
   private final Queue<BlockPos> selectedBlocks = new LinkedList<>();
   private final Queue<NukerModule.MineData> autoMineQueue = new ArrayDeque<>();
   private int autoMineTickDelay;
   private NukerModule.MineAnimation packetMineAnim = new NukerModule.MineAnimation(NukerModule.MineData.empty(), new Animation(true, 200.0F));
   private NukerModule.MineAnimation instantMineAnim = new NukerModule.MineAnimation(NukerModule.MineData.empty(), new Animation(true, 200.0F));

   public NukerModule() {
      super("Nuker", "Clears nearby blocks", ModuleCategory.WORLD);
   }

   @Override
   public void onDisable() {
      this.autoMineQueue.clear();
      this.packetMine = null;
      if (this.instantMine != null) {
         this.abortMining(this.instantMine);
         this.instantMine = null;
      }

      this.packetMineAnim = new NukerModule.MineAnimation(NukerModule.MineData.empty(), new Animation(true, 200.0F));
      this.instantMineAnim = new NukerModule.MineAnimation(NukerModule.MineData.empty(), new Animation(true, 200.0F));
      this.autoMineTickDelay = 0;
      this.waitForPacketMine = false;
      if (this.packetSwapBack) {
         Managers.INVENTORY.syncToClient();
         this.packetSwapBack = false;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.isInstantMineComplete()) {
            if (this.changedInstantMine) {
               this.changedInstantMine = false;
            }

            if (this.waitForPacketMine) {
               this.waitForPacketMine = false;
            }
         }

         this.autoMineTickDelay--;
         if (this.packetMine != null && this.packetMine.getTicksMining() > this.mineTicksConfig.getValue()) {
            this.packetMineAnim.animation.setState(false);
            if (this.packetSwapBack) {
               Managers.INVENTORY.syncToClient();
               this.packetSwapBack = false;
            }

            this.selectedBlocks.remove(this.packetMine.getPos());
            this.packetMine = null;
            if (!this.isInstantMineComplete()) {
               this.waitForPacketMine = true;
            }
         }

         if (this.packetMine != null) {
            float damageDelta = SpeedmineModule.getInstance().calcBlockBreakingDelta(this.packetMine.getState(), mc.level, this.packetMine.getPos());
            this.packetMine.addBlockDamage(damageDelta);
            int slot = this.packetMine.getBestSlot();
            float damageDone = this.packetMine.getBlockDamage() + (this.swapBeforeConfig.getValue() ? damageDelta : 0.0F);
            if (damageDone >= 1.0F && this.canMine(this.packetMine.getState()) && slot != -1 && !this.checkMultitask()) {
               this.packetMine.markAttemptedMine();
               Managers.INVENTORY.setSlot(slot);
               this.packetSwapBack = true;
            }
         }

         if (this.packetSwapBack && (this.packetMine == null || !this.canMine(this.packetMine.getState()))) {
            Managers.INVENTORY.syncToClient();
            this.packetSwapBack = false;
            this.packetMineAnim.animation.setState(false);
            if (this.packetMine != null) {
               this.selectedBlocks.remove(this.packetMine.getPos());
            }

            this.packetMine = null;
            if (!this.isInstantMineComplete()) {
               this.waitForPacketMine = true;
            }
         }

         if (this.instantMine != null) {
            double distance = mc.player.getEyePosition().distanceToSqr(this.instantMine.getPos().getCenter());
            if (distance > ((NumberConfig)this.rangeConfig).getValueSq() || this.instantMine.getTicksMining() > this.mineTicksConfig.getValue()) {
               this.abortMining(this.instantMine);
               this.instantMineAnim.animation.setState(false);
               this.selectedBlocks.remove(this.instantMine.getPos());
               this.instantMine = null;
            }
         }

         if (this.instantMine != null) {
            float damageDelta = SpeedmineModule.getInstance().calcBlockBreakingDelta(this.instantMine.getState(), mc.level, this.instantMine.getPos());
            this.instantMine.addBlockDamage(damageDelta);
            if (this.instantMine.getBlockDamage() >= this.speedConfig.getValue()) {
               if (this.canMine(this.instantMine.getState())) {
                  if (!this.checkMultitask() || this.multitaskConfig.getValue() || this.swapConfig.getValue() == NukerModule.Swap.OFF) {
                     this.stopMining(this.instantMine);
                     this.instantMine.markAttemptedMine();
                  }
               } else {
                  this.abortMining(this.instantMine);
                  this.instantMineAnim.animation.setState(false);
                  this.selectedBlocks.remove(this.instantMine.getPos());
                  this.instantMine = null;
               }
            }
         }

         if (!this.autoMineQueue.isEmpty() && this.autoMineTickDelay <= 0) {
            NukerModule.MineData nextMine = this.autoMineQueue.poll();
            if (nextMine != null) {
               this.startMining(nextMine);
               this.autoMineTickDelay = 5;
            }
         }

         if (this.autoMineQueue.isEmpty()) {
            NukerModule.MineData bestMine = this.getNukerMine();
            if (bestMine != null && (this.packetMine == null && this.doubleBreakConfig.getValue() || this.isInstantMineComplete())) {
               this.startAutoMine(bestMine);
            }
         }
      }
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator()) {
         event.cancel();
         if (event.getState().getBlock().defaultDestroyTime() != -1.0F && this.canMine(event.getState()) && !this.selectedBlocks.contains(event.getPos())) {
            double distance = mc.player.getEyePosition().distanceToSqr(event.getPos().getCenter());
            if (!(distance > ((NumberConfig)this.rangeConfig).getValueSq())) {
               this.selectedBlocks.add(event.getPos());
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket && this.switchResetConfig.getValue()) {
         this.instantMine.setTotalBlockDamage(0.0F, 0.0F);
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet
         && !this.canMine(packet.getBlockState())
         && this.packetMine != null
         && this.packetMine.getPos().equals(packet.getPos())) {
         this.packetMineAnim.animation.setState(false);
         if (this.packetSwapBack) {
            Managers.INVENTORY.syncToClient();
            this.packetSwapBack = false;
         }

         this.packetMine = null;
         this.waitForPacketMine = false;
         if (!this.isInstantMineComplete()) {
            this.waitForPacketMine = true;
         }
      }
   }

   private List<BlockPos> getSphere(Vec3 origin) {
      List<BlockPos> sphere = new ArrayList<>();
      double rad = Math.ceil(this.rangeConfig.getValue().floatValue());

      for (double x = -rad; x <= rad; x++) {
         for (double y = this.flattenConfig.getValue() ? 0.0 : -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.x() + x), (int)(origin.y() + y), (int)(origin.z() + z));
               BlockPos p = new BlockPos(pos);
               sphere.add(p);
            }
         }
      }

      return sphere;
   }

   public void startAutoMine(NukerModule.MineData data) {
      if (this.canMine(data.getState()) && !this.isMining(data.getPos())) {
         if (!this.doubleBreakConfig.getValue()) {
            this.instantMine = data;
            this.autoMineQueue.offer(data);
         } else if ((!this.changedInstantMine || this.isInstantMineComplete()) && !this.waitForPacketMine) {
            boolean updateChanged = false;
            if (!this.isInstantMineComplete() && !this.changedInstantMine) {
               if (this.packetMine == null) {
                  this.packetMine = this.instantMine.copy();
                  this.packetMineAnim = new NukerModule.MineAnimation(this.packetMine, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
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

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator()) {
         RenderBuffers.preRender();

         for (BlockPos pos : this.selectedBlocks) {
            RenderManager.renderBoundingBox(event.getMatrices(), pos, 2.0F, ColorsModule.getInstance().getRGB(160));
         }

         if (this.instantMineAnim != null && this.instantMineAnim.animation().getFactor() > 0.01F) {
            this.renderMiningData(event.getMatrices(), event.getTickDelta(), this.instantMineAnim, true);
         }

         if (this.packetMineAnim != null && this.packetMineAnim.animation().getFactor() > 0.01F) {
            this.renderMiningData(event.getMatrices(), event.getTickDelta(), this.packetMineAnim, false);
         }

         RenderBuffers.postRender();
      }
   }

   public void renderMiningData(PoseStack matrixStack, float tickDelta, NukerModule.MineAnimation mineAnimation, boolean instantMine) {
      NukerModule.MineData data = mineAnimation.data();
      Animation animation = mineAnimation.animation();
      int boxAlpha = (int)(40.0 * animation.getFactor());
      int lineAlpha = (int)(100.0 * animation.getFactor());
      int boxColor;
      int lineColor;
      if (this.smoothColorConfig.getValue()) {
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

   public NukerModule.MineData getNukerMine() {
      if (this.modeConfig.getValue() == NukerModule.NukeMode.SPHERE) {
         List<BlockPos> sphere = this.getSphere(mc.player.position());
         BlockPos minePos = null;
         int yLevel = -128;
         double dist = Double.MAX_VALUE;

         for (BlockPos blockPos : sphere) {
            BlockState state = mc.level.getBlockState(blockPos);
            if (this.canMine(state) && !this.isMining(blockPos)) {
               double distance = mc.player.getEyePosition().distanceToSqr(blockPos.getCenter());
               if (!(distance > ((NumberConfig)this.rangeConfig).getValueSq())) {
                  int y = blockPos.getY();
                  double distToPlayer = mc.player.getEyePosition().distanceToSqr(blockPos.getCenter());
                  if (y > yLevel || y == yLevel && distToPlayer < dist) {
                     minePos = blockPos;
                     yLevel = y;
                     dist = distToPlayer;
                  }
               }
            }
         }

         if (minePos != null) {
            return new NukerModule.MineData(
               minePos, this.strictDirectionConfig.getValue() ? Managers.INTERACT.getInteractDirection(minePos, false) : Direction.UP
            );
         }
      } else {
         if (this.selectedBlocks.isEmpty()) {
            return null;
         }

         if (this.packetMine != null && this.instantMine != null) {
            return null;
         }

         Queue<BlockPos> blocks = new LinkedList<>(this.selectedBlocks);
         BlockPos minePos = blocks.poll();
         if (this.instantMine != null && this.instantMine.getPos().equals(minePos)) {
            minePos = this.selectedBlocks.poll();
         }

         if (minePos != null) {
            return new NukerModule.MineData(
               minePos, this.strictDirectionConfig.getValue() ? Managers.INTERACT.getInteractDirection(minePos, false) : Direction.UP
            );
         }
      }

      return null;
   }

   public void startMining(NukerModule.MineData data) {
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
         if (!this.grimConfig.getValue()) {
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
         }
      }

      this.instantMineAnim = new NukerModule.MineAnimation(data, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
   }

   public void abortMining(NukerModule.MineData data) {
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public void stopMining(NukerModule.MineData data) {
      if (this.rotateConfig.getValue()) {
         float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
         if (this.grimConfig.getValue()) {
            this.setRotationSilent(rotations[0], rotations[1]);
         } else {
            this.setRotation(rotations[0], rotations[1]);
         }
      }

      int slot = data.getBestSlot();
      boolean canSwap = slot != -1 && slot != Managers.INVENTORY.getServerSlot();
      if (canSwap) {
         this.swapTo(slot);
      }

      this.stopMiningInternal(data);
      if (canSwap) {
         this.swapSync(slot);
      }

      if (this.rotateConfig.getValue()) {
         Managers.ROTATION.setRotationSilentSync();
      }
   }

   private void stopMiningInternal(NukerModule.MineData data) {
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public boolean isInstantMineComplete() {
      return this.instantMine == null || this.instantMine.getBlockDamage() >= this.speedConfig.getValue() && !this.canMine(this.instantMine.getState());
   }

   private void swapTo(int slot) {
      switch ((NukerModule.Swap)this.swapConfig.getValue()) {
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
      switch ((NukerModule.Swap)this.swapConfig.getValue()) {
         case SILENT:
            Managers.INVENTORY.syncToClient();
            break;
         case SILENT_ALT:
            Managers.INVENTORY.setSlotAlt(slot);
      }
   }

   private boolean isMining(BlockPos blockPos) {
      return this.instantMine != null && this.instantMine.getPos().equals(blockPos) || this.packetMine != null && this.packetMine.getPos().equals(blockPos);
   }

   private boolean validNukerBlock(Block block) {
      if (BlastResistantBlocks.isUnbreakable(block)) {
         return false;
      } else {
         return switch ((NukerModule.Selection)this.selectionConfig.getValue()) {
            case WHITELIST -> ((BlockListConfig)this.whitelistConfig).contains(block);
            case BLACKLIST -> !((BlockListConfig)this.blacklistConfig).contains(block);
            case ALL -> true;
         };
      }
   }

   public boolean canMine(BlockState state) {
      return !state.isAir() && state.getFluidState().isEmpty();
   }

   public record MineAnimation(NukerModule.MineData data, Animation animation) {
   }

   public static class MineData {
      private final BlockPos pos;
      private final Direction direction;
      private int ticksMining;
      private float blockDamage;
      private float lastDamage;

      public MineData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
      }

      @Override
      public boolean equals(Object obj) {
         return obj instanceof NukerModule.MineData d && d.getPos().equals(this.pos);
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

      public static NukerModule.MineData empty() {
         return new NukerModule.MineData(BlockPos.ZERO, Direction.UP);
      }

      public NukerModule.MineData copy() {
         NukerModule.MineData data = new NukerModule.MineData(this.pos, this.direction);
         data.setTotalBlockDamage(this.blockDamage, this.lastDamage);
         return data;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
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

      public BlockState getState() {
         return Globals.mc.level.getBlockState(this.pos);
      }

      public int getBestSlot() {
         return AutoToolModule.getInstance().getBestToolNoFallback(this.getState());
      }
   }

   public static enum NukeMode {
      SPHERE,
      SELECT;
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
