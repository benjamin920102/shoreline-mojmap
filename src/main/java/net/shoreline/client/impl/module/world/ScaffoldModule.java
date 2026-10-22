package net.shoreline.client.impl.module.world;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BlockListConfig;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.BlockPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

public final class ScaffoldModule extends BlockPlacerModule {
   Config<Boolean> rotateHoldConfig = this.register(
      new BooleanConfig("RotateHold", "Holds rotations to scaffold blocks", false, () -> this.rotateConfig.getValue())
   );
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Uses grim interactions", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Uses grim new interactions", false));
   Config<ScaffoldModule.Selection> selectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of blocks to use for scaffold", ScaffoldModule.Selection.ALL, ScaffoldModule.Selection.values())
   );
   Config<List<Block>> whitelistConfig = this.register(new BlockListConfig<>("Whitelist", "Valid block whitelist", Blocks.DIRT, Blocks.OBSIDIAN));
   Config<List<Block>> blacklistConfig = this.register(new BlockListConfig<>("Blacklist", "Valid block blacklist", Blocks.SHULKER_BOX));
   Config<Boolean> keepYConfig = this.register(new BooleanConfig("KeepY", "Keeps the same y-level", false));
   Config<Boolean> towerConfig = this.register(new BooleanConfig("Tower", "Goes up faster when holding down space", true, () -> !this.grimNewConfig.getValue()));
   Config<ScaffoldModule.BlockPicker> pickerConfig = this.register(
      new EnumConfig<>("BlockSelection", "How to pick a block from the hotbar", ScaffoldModule.BlockPicker.NORMAL, ScaffoldModule.BlockPicker.values())
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> false));
   Config<Boolean> stopMotionConfig = this.register(new BooleanConfig("StopMotion", "Stops player motion when placing blocks", false));
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private ScaffoldModule.BlockData blockData;
   private ScaffoldModule.BlockData renderData;
   private float[] lastAngles;
   private int groundPosY;

   public ScaffoldModule() {
      super("Scaffold", "Places blocks at the players feet", ModuleCategory.WORLD, 790);
   }

   @Override
   protected void onDisable() {
      if (mc.player != null) {
         Managers.INVENTORY.syncToClient();
      }

      this.groundPosY = -1;
      this.lastAngles = null;
      this.blockData = null;
      this.renderData = null;
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.blockData = null;
         this.renderData = null;
      } else {
         ScaffoldModule.BlockSlot blockSlot = this.getBlockSlot();
         int slot = blockSlot.slot();
         if (slot == -1) {
            this.blockData = null;
            this.renderData = null;
         } else {
            this.renderData = this.getBlockData(false);
            this.blockData = this.getBlockData(this.rotateHoldConfig.getValue());
            if (this.blockData == null) {
               if (this.grimNewConfig.getValue() && this.rotateConfig.getValue()) {
                  float yaw = mc.player.getYRot();
                  if (mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
                     if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                        yaw -= 45.0F;
                     } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                        yaw += 45.0F;
                     }
                  } else if (mc.options.keyDown.isDown() && !mc.options.keyUp.isDown()) {
                     yaw += 180.0F;
                     if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                        yaw += 45.0F;
                     } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                        yaw -= 45.0F;
                     }
                  } else if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                     yaw -= 90.0F;
                  } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                     yaw += 90.0F;
                  }

                  this.setRotation(Mth.wrapDegrees(yaw), 90.0F);
               }
            } else {
               this.calcRotations(this.blockData);
               if (this.blockData.getAngles() == null) {
                  if (!this.isGrim() && this.rotateConfig.getValue() && this.lastAngles != null) {
                     this.setRotation(this.lastAngles[0], this.lastAngles[1]);
                  }
               } else {
                  if (!this.isGrim() && Managers.INVENTORY.getServerSlot() != slot) {
                     Managers.INVENTORY.setSlot(slot);
                  }

                  Vec3 prevMotion = mc.player.getDeltaMovement();
                  if (this.stopMotionConfig.getValue()) {
                     mc.player.setDeltaMovement(0.0, 0.0, 0.0);
                  }

                  boolean result = Managers.INTERACT.placeBlock(this.blockData.getBlockPos(), slot, false, false, false, (state, angles) -> {
                     if (this.rotateConfig.getValue()) {
                        float[] rotations = this.blockData.getAngles();
                        if (rotations == null) {
                           return;
                        }

                        this.lastAngles = rotations;
                        if (state) {
                           if (this.grimConfig.getValue()) {
                              Managers.ROTATION.setRotationSilent(rotations[0], rotations[1]);
                           } else {
                              this.setRotation(rotations[0], rotations[1]);
                           }
                        } else if (this.grimConfig.getValue()) {
                           Managers.ROTATION.setRotationSilentSync();
                        }
                     }
                  });
                  if (this.stopMotionConfig.getValue()) {
                     mc.player.setDeltaMovement(prevMotion);
                  }

                  if (result && !this.isGrim() && this.towerConfig.getValue() && mc.options.keyJump.isDown()) {
                     Vec3 velocity = mc.player.getDeltaMovement();
                     double velocityY = velocity.y;
                     if (mc.player.onGround() || velocityY < 0.1 || velocityY <= 0.16477328182606651) {
                        mc.player.setDeltaMovement(velocity.x, 0.42F, velocity.z);
                     }
                  }
               }
            }
         }
      }
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
         if (this.renderData == null || this.renderData.getHitResult() == null) {
            return;
         }

         if (this.renderConfig.getValue()) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.renderData.getBlockPos(), animation);
         }

         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
      }
   }

   private void calcRotations(ScaffoldModule.BlockData blockData) {
      BlockPos pos = blockData.getHitResult().getBlockPos();
      Direction side = blockData.getHitResult().getDirection();
      Vec3 basicHitVec = pos.getCenter().add(side.getStepX() * 0.5F, side.getStepY() * 0.5F, side.getStepZ() * 0.5F);
      blockData.setAngles(RotationUtil.getRotationsTo(mc.player.getEyePosition(), basicHitVec));
      blockData.setHitResult(new BlockHitResult(basicHitVec, side, pos, false));
   }

   private ScaffoldModule.BlockData getBlockData(boolean hold) {
      int posY = (int)Math.round(mc.player.getY()) - 1;
      if (this.keepYConfig.getValue() && MovementUtil.isInputtingMovement()) {
         if (mc.player.onGround() || this.groundPosY == -1) {
            this.groundPosY = (int)Math.floor(mc.player.getY()) - 1;
         }

         posY = this.groundPosY;
      }

      BlockPos pos = PositionUtil.getRoundedBlockPos(mc.player.getX(), posY, mc.player.getZ());
      if (!hold && !mc.level.getBlockState(pos).canBeReplaced()) {
         return null;
      } else {
         for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!mc.level.getBlockState(neighbor).canBeReplaced()) {
               return ScaffoldModule.BlockData.basic(neighbor, direction.getOpposite());
            }
         }

         for (Direction directionx : Direction.values()) {
            BlockPos neighbor = pos.relative(directionx);
            if (mc.level.getBlockState(neighbor).canBeReplaced()) {
               for (Direction direction1 : Direction.values()) {
                  BlockPos neighbor1 = neighbor.relative(direction1);
                  if (!mc.level.getBlockState(neighbor1).canBeReplaced()) {
                     return ScaffoldModule.BlockData.basic(neighbor1, direction1.getOpposite());
                  }
               }
            }
         }

         return null;
      }
   }

   private ScaffoldModule.BlockSlot getBlockSlot() {
      ItemStack serverStack = Managers.INVENTORY.getServerItem();
      if (!serverStack.isEmpty() && serverStack.getItem() instanceof BlockItem blockItem && this.validScaffoldBlock(blockItem.getBlock())) {
         return new ScaffoldModule.BlockSlot(blockItem.getBlock(), Managers.INVENTORY.getServerSlot());
      } else {
         Block block = null;
         int blockSlot = -1;
         int count = 0;

         for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem blockItem && this.validScaffoldBlock(blockItem.getBlock())) {
               if (this.pickerConfig.getValue() == ScaffoldModule.BlockPicker.NORMAL) {
                  return new ScaffoldModule.BlockSlot(blockItem.getBlock(), i);
               }

               if (blockSlot == -1 || itemStack.getCount() > count) {
                  block = blockItem.getBlock();
                  blockSlot = i;
                  count = itemStack.getCount();
               }
            }
         }

         return new ScaffoldModule.BlockSlot(block, blockSlot);
      }
   }

   private boolean validScaffoldBlock(Block block) {
      return switch ((ScaffoldModule.Selection)this.selectionConfig.getValue()) {
         case WHITELIST -> ((BlockListConfig)this.whitelistConfig).contains(block);
         case BLACKLIST -> !((BlockListConfig)this.blacklistConfig).contains(block);
         case ALL -> true;
      };
   }

   public boolean isGrim() {
      return this.grimConfig.getValue() || this.grimNewConfig.getValue();
   }

   private static class BlockData {
      private BlockHitResult hitResult;
      private float[] angles;

      public BlockData(BlockHitResult hitResult, float[] angles) {
         this.hitResult = hitResult;
         this.angles = angles;
      }

      public BlockHitResult getHitResult() {
         return this.hitResult;
      }

      public BlockPos getBlockPos() {
         return this.hitResult.getBlockPos().relative(this.hitResult.getDirection());
      }

      public void setHitResult(BlockHitResult hitResult) {
         this.hitResult = hitResult;
      }

      public float[] getAngles() {
         return this.angles;
      }

      public void setAngles(float[] angles) {
         this.angles = angles;
      }

      public static ScaffoldModule.BlockData basic(BlockPos pos, Direction direction) {
         return new ScaffoldModule.BlockData(new BlockHitResult(pos.getCenter(), direction, pos, false), null);
      }
   }

   private static enum BlockPicker {
      NORMAL,
      GREATEST;
   }

   private record BlockSlot(Block block, int slot) {
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }
}
