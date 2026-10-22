package net.shoreline.client.impl.module.world;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.AttackBlockEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.CombatModule;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorClientPlayerInteractionManager;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.collection.FirstOutQueue;
import net.shoreline.client.util.player.EnchantmentUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class SpeedmineModule extends CombatModule {
   private static SpeedmineModule INSTANCE;
   Config<SpeedmineModule.SpeedmineMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mining mode for speedmine", SpeedmineModule.SpeedmineMode.PACKET, SpeedmineModule.SpeedmineMode.values())
   );
   Config<Boolean> multitaskConfig = this.register(
      new BooleanConfig("Multitask", "Allows mining while using items", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Boolean> doubleBreakConfig = this.register(
      new BooleanConfig("DoubleBreak", "Allows you to mine two blocks at once", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Float> rangeConfig = this.register(
      new NumberConfig<>("Range", "The range to mine blocks", 0.1F, 4.0F, 6.0F, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The speed to mine blocks", 0.1F, 1.0F, 1.0F));
   Config<Boolean> instantConfig = this.register(new BooleanConfig("Instant", "Instantly mines already broken blocks", false));
   Config<SpeedmineModule.Swap> swapConfig = this.register(
      new EnumConfig<>(
         "AutoSwap",
         "Swaps to the best tool once the mining is complete",
         SpeedmineModule.Swap.SILENT,
         SpeedmineModule.Swap.values(),
         () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET
      )
   );
   Config<Boolean> rotateConfig = this.register(
      new BooleanConfig("Rotate", "Rotates when mining the block", true, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Boolean> switchResetConfig = this.register(
      new BooleanConfig("SwitchReset", "Resets mining after switching items", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Uses grim block breaking speeds", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Uses new grim block breaking speeds", false, () -> this.grimConfig.getValue()));
   Config<Color> colorConfig = this.register(
      new ColorConfig("MineColor", "The mine render color", Color.RED, false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Color> colorDoneConfig = this.register(
      new ColorConfig("DoneColor", "The done render color", Color.GREEN, false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET)
   );
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   Config<Boolean> smoothColorConfig = this.register(new BooleanConfig("ColorSmooth", "Interpolates from start to done color", false, () -> false));
   private final Map<SpeedmineModule.MiningData, Animation> fadeList = new HashMap<>();
   private FirstOutQueue<SpeedmineModule.MiningData> miningQueue = new FirstOutQueue<>(2);
   private long lastBreak;

   public SpeedmineModule() {
      super("Speedmine", "Mines blocks faster", ModuleCategory.WORLD, 900);
      INSTANCE = this;
   }

   public static SpeedmineModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      if (this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         SpeedmineModule.MiningData miningData = (SpeedmineModule.MiningData)this.miningQueue.peek();
         if (miningData != null) {
            return String.format("%.1f", Math.min(miningData.getBlockDamage(), 1.0F));
         }
      }

      return super.getModuleData();
   }

   @Override
   protected void onDisable() {
      this.miningQueue.clear();
      this.fadeList.clear();
      Managers.INVENTORY.syncToClient();
   }

   @Override
   public void onEnable() {
      if (this.doubleBreakConfig.getValue()) {
         this.miningQueue = new FirstOutQueue<>(2);
      } else {
         this.miningQueue = new FirstOutQueue<>(1);
      }
   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.DAMAGE) {
            AccessorClientPlayerInteractionManager interactionManager = (AccessorClientPlayerInteractionManager)mc.gameMode;
            if (interactionManager.hookGetCurrentBreakingProgress() >= this.speedConfig.getValue()) {
               interactionManager.hookSetCurrentBreakingProgress(1.0F);
            }
         } else if (!AutoMineModule.getInstance().isEnabled()) {
            if (!this.miningQueue.isEmpty()) {
               for (SpeedmineModule.MiningData data : this.miningQueue) {
                  if (data.getState().isAir()) {
                     data.resetBreakTime();
                  }

                  if (!this.isDataPacketMine(data) || !data.getState().isAir() && (!data.hasAttemptedBreak() || !data.passedAttemptedBreakTime(500L))) {
                     float damageDelta = this.calcBlockBreakingDelta(data.getState(), mc.level, data.getPos());
                     data.damage(damageDelta);
                     if (this.isDataPacketMine(data) && data.getBlockDamage() >= 1.0F && data.getSlot() != -1) {
                        if (mc.player.isUsingItem() && !this.multitaskConfig.getValue()) {
                           return;
                        }

                        if (data.getSlot() != Managers.INVENTORY.getServerSlot()) {
                           Managers.INVENTORY.setSlot(data.getSlot());
                        }

                        if (!data.hasAttemptedBreak()) {
                           data.setAttemptedBreak(true);
                        }
                     }
                  } else {
                     Managers.INVENTORY.syncToClient();
                     this.miningQueue.remove(data);
                  }
               }

               SpeedmineModule.MiningData miningData2 = this.miningQueue.getFirst();
               double distance = mc.player.getEyePosition().distanceToSqr(miningData2.getPos().getCenter());
               if (distance > ((NumberConfig)this.rangeConfig).getValueSq()) {
                  this.miningQueue.remove(miningData2);
               } else if (!miningData2.getState().isAir()) {
                  if (miningData2.getBlockDamage() >= this.speedConfig.getValue()
                     && miningData2.hasAttemptedBreak()
                     && miningData2.passedAttemptedBreakTime(500L)) {
                     this.abortMining(miningData2);
                     this.miningQueue.remove(miningData2);
                  }

                  if (miningData2.getBlockDamage() >= this.speedConfig.getValue()) {
                     if (mc.player.isUsingItem() && !this.multitaskConfig.getValue()) {
                        return;
                     }

                     this.stopMining(miningData2);
                     if (!this.instantConfig.getValue()) {
                        this.miningQueue.remove(miningData2);
                     }

                     if (!miningData2.hasAttemptedBreak()) {
                        miningData2.setAttemptedBreak(true);
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         if (!AutoMineModule.getInstance().isEnabled()) {
            event.cancel();
            if (event.getState().getBlock().defaultDestroyTime() != -1.0F && !event.getState().isAir()) {
               this.startManualMine(event.getPos(), event.getDirection());
               mc.player.swing(InteractionHand.MAIN_HAND);
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundPlayerActionPacket packet
         && packet.getAction() == Action.STOP_DESTROY_BLOCK
         && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.DAMAGE
         && this.grimConfig.getValue()) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, packet.getPos().above(500), packet.getDirection()));
      }

      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket
         && this.switchResetConfig.getValue()
         && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         for (SpeedmineModule.MiningData data : this.miningQueue) {
            data.resetDamage();
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         if (!AutoMineModule.getInstance().isEnabled()) {
            if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
               this.handleBlockUpdatePacket(packet);
            } else if (event.getPacket() instanceof ClientboundBundlePacket packet) {
               for (Packet<?> packet1 : packet.subPackets()) {
                  if (packet1 instanceof ClientboundBlockUpdatePacket packet2) {
                     this.handleBlockUpdatePacket(packet2);
                  }
               }
            }
         }
      }
   }

   private void handleBlockUpdatePacket(ClientboundBlockUpdatePacket packet) {
      if (packet.getBlockState().isAir()) {
         for (SpeedmineModule.MiningData data : this.miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
               data.setAttemptedBreak(false);
            }
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.doubleBreakConfig) {
         if (this.doubleBreakConfig.getValue()) {
            this.miningQueue = new FirstOutQueue<>(2);
         } else {
            this.miningQueue = new FirstOutQueue<>(1);
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!mc.player.isCreative() && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         if (!AutoMineModule.getInstance().isEnabled()) {
            RenderBuffers.preRender();

            for (Entry<SpeedmineModule.MiningData, Animation> set : this.fadeList.entrySet()) {
               SpeedmineModule.MiningData data = set.getKey();
               set.getValue().setState(false);
               int boxAlpha = (int)(40.0 * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               int boxColor;
               int lineColor;
               if (this.smoothColorConfig.getValue()) {
                  boxColor = data.getState().isAir()
                     ? ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha)
                     : ColorUtil.interpolateColor(
                           Math.min(data.getBlockDamage(), 1.0F),
                           ((ColorConfig)this.colorDoneConfig).getValue(boxAlpha),
                           ((ColorConfig)this.colorConfig).getValue(boxAlpha)
                        )
                        .getRGB();
                  lineColor = data.getState().isAir()
                     ? ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha)
                     : ColorUtil.interpolateColor(
                           Math.min(data.getBlockDamage(), 1.0F),
                           ((ColorConfig)this.colorDoneConfig).getValue(lineAlpha),
                           ((ColorConfig)this.colorConfig).getValue(lineAlpha)
                        )
                        .getRGB();
               } else {
                  boxColor = !(data.getBlockDamage() >= 0.95F) && !data.getState().isAir()
                     ? ((ColorConfig)this.colorConfig).getRgb(boxAlpha)
                     : ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha);
                  lineColor = !(data.getBlockDamage() >= 0.95F) && !data.getState().isAir()
                     ? ((ColorConfig)this.colorConfig).getRgb(lineAlpha)
                     : ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha);
               }

               BlockPos mining = data.getPos();
               VoxelShape outlineShape = data.getState().getShape(mc.level, mining);
               outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
               AABB render1 = outlineShape.bounds();
               AABB render = new AABB(
                  mining.getX() + render1.minX,
                  mining.getY() + render1.minY,
                  mining.getZ() + render1.minZ,
                  mining.getX() + render1.maxX,
                  mining.getY() + render1.maxY,
                  mining.getZ() + render1.maxZ
               );
               Vec3 center = render.getCenter();
               float total = this.isDataPacketMine(data) ? 1.0F : this.speedConfig.getValue();
               float scale = data.getState().isAir()
                  ? 1.0F
                  : Mth.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.getTickDelta()) / total, 0.0F, 1.0F);
               double dx = (render1.maxX - render1.minX) / 2.0;
               double dy = (render1.maxY - render1.minY) / 2.0;
               double dz = (render1.maxZ - render1.minZ) / 2.0;
               AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
               RenderManager.renderBox(event.getMatrices(), scaled, boxColor);
               RenderManager.renderBoundingBox(event.getMatrices(), scaled, 1.5F, lineColor);
            }

            for (SpeedmineModule.MiningData data : this.miningQueue) {
               if (!data.getState().isAir()) {
                  Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
                  this.fadeList.put(data, animation);
               }
            }

            this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
            RenderBuffers.postRender();
         }
      }
   }

   private void startManualMine(BlockPos pos, Direction direction) {
      this.clickMine(new SpeedmineModule.MiningData(pos, direction));
   }

   public void clickMine(SpeedmineModule.MiningData miningData) {
      int queueSize = this.miningQueue.size();
      if (queueSize <= 2) {
         this.queueMiningData(miningData);
      }
   }

   private void queueMiningData(SpeedmineModule.MiningData data) {
      if (!data.getState().isAir()) {
         if (this.startMining(data)) {
            if (this.miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
               return;
            }

            this.miningQueue.addFirst(data);
         }
      }
   }

   private boolean startMining(SpeedmineModule.MiningData data) {
      if (data.isStarted()) {
         return false;
      } else {
         data.setStarted();
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
            return true;
         } else {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            return true;
         }
      }
   }

   private void abortMining(SpeedmineModule.MiningData data) {
      if (data.isStarted() && !data.getState().isAir()) {
         Managers.NETWORK.sendSequencedPacket(id -> new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
         Managers.INVENTORY.syncToClient();
      }
   }

   private void stopMining(SpeedmineModule.MiningData data) {
      if (data.isStarted() && !data.getState().isAir()) {
         if (this.rotateConfig.getValue()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
            if (this.grimConfig.getValue()) {
               this.setRotationSilent(rotations[0], rotations[1]);
            } else {
               this.setRotation(rotations[0], rotations[1]);
            }
         }

         int slot = data.getSlot();
         boolean canSwap = slot != -1 && slot != Managers.INVENTORY.getServerSlot();
         if (canSwap) {
            this.swapTo(slot);
         }

         this.stopMiningInternal(data);
         this.lastBreak = System.currentTimeMillis();
         if (canSwap) {
            this.swapSync(slot);
         }

         if (this.rotateConfig.getValue()) {
            Managers.ROTATION.setRotationSilentSync();
         }
      }
   }

   private void swapTo(int slot) {
      switch ((SpeedmineModule.Swap)this.swapConfig.getValue()) {
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
      switch ((SpeedmineModule.Swap)this.swapConfig.getValue()) {
         case SILENT:
            Managers.INVENTORY.syncToClient();
            break;
         case SILENT_ALT:
            Managers.INVENTORY.setSlotAlt(slot);
      }
   }

   private void stopMiningInternal(SpeedmineModule.MiningData data) {
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
      Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public boolean isBlockDelayGrim() {
      return System.currentTimeMillis() - this.lastBreak <= 280L && this.grimConfig.getValue();
   }

   private boolean isDataPacketMine(SpeedmineModule.MiningData data) {
      return this.miningQueue.size() == 2 && data == this.miningQueue.getLast();
   }

   public float calcBlockBreakingDelta(BlockState state, BlockGetter world, BlockPos pos) {
      if (this.swapConfig.getValue() == SpeedmineModule.Swap.OFF) {
         return state.getDestroyProgress(mc.player, mc.level, pos);
      } else {
         float f = state.getDestroySpeed(world, pos);
         if (f == -1.0F) {
            return 0.0F;
         } else {
            int i = this.canHarvest(state) ? 30 : 100;
            return this.getBlockBreakingSpeed(state) / f / i;
         }
      }
   }

   private float getBlockBreakingSpeed(BlockState block) {
      int tool = AutoToolModule.getInstance().getBestTool(block);
      float f = mc.player.getInventory().getItem(tool).getDestroySpeed(block);
      if (f > 1.0F) {
         ItemStack stack = mc.player.getInventory().getItem(tool);
         int i = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
         if (i > 0 && !stack.isEmpty()) {
            f += i * i + 1;
         }
      }

      if (MobEffectUtil.hasDigSpeed(mc.player)) {
         f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2F;
      }

      if (mc.player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
         float g = switch (mc.player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         f *= g;
      }

      if (!mc.player.onGround()) {
         f /= 5.0F;
      }

      return f;
   }

   private boolean canHarvest(BlockState state) {
      if (state.requiresCorrectToolForDrops()) {
         int tool = AutoToolModule.getInstance().getBestTool(state);
         return mc.player.getInventory().getItem(tool).isCorrectToolForDrops(state);
      } else {
         return true;
      }
   }

   public boolean isMining() {
      return !this.miningQueue.isEmpty();
   }

   public static class MiningData {
      private boolean attemptedBreak;
      private long breakTime;
      private final BlockPos pos;
      private final Direction direction;
      private float lastDamage;
      private float blockDamage;
      private boolean started;

      public MiningData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
      }

      public void setAttemptedBreak(boolean attemptedBreak) {
         this.attemptedBreak = attemptedBreak;
         if (attemptedBreak) {
            this.resetBreakTime();
         }
      }

      public void resetBreakTime() {
         this.breakTime = System.currentTimeMillis();
      }

      public boolean hasAttemptedBreak() {
         return this.attemptedBreak;
      }

      public boolean passedAttemptedBreakTime(long time) {
         return System.currentTimeMillis() - this.breakTime >= time;
      }

      public float damage(float dmg) {
         this.lastDamage = this.blockDamage;
         this.blockDamage += dmg;
         return this.blockDamage;
      }

      public void setDamage(float blockDamage) {
         this.blockDamage = blockDamage;
      }

      public void resetDamage() {
         this.started = false;
         this.blockDamage = 0.0F;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public int getSlot() {
         return AutoToolModule.getInstance().getBestToolNoFallback(this.getState());
      }

      public BlockState getState() {
         return Globals.mc.level.getBlockState(this.pos);
      }

      public float getBlockDamage() {
         return this.blockDamage;
      }

      public float getLastDamage() {
         return this.lastDamage;
      }

      public boolean isStarted() {
         return this.started;
      }

      public void setStarted() {
         this.started = true;
      }
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }

   public static enum SpeedmineMode {
      PACKET,
      DAMAGE;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }
}
