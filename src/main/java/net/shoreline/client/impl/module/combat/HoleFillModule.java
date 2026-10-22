package net.shoreline.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.manager.combat.hole.Hole;
import net.shoreline.client.impl.manager.combat.hole.HoleType;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

public class HoleFillModule extends ObsidianPlacerModule {
   private static HoleFillModule INSTANCE;
   Config<Boolean> obsidianConfig = this.register(new BooleanConfig("Obsidian", "Fills obsidian holes", true));
   Config<Boolean> doublesConfig = this.register(new BooleanConfig("Doubles", "Fills double holes", false));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("PlaceRange", "The range to fill nearby holes", 0.1F, 4.0F, 6.0F));
   Config<Boolean> websConfig = this.register(new BooleanConfig("Webs", "Fills holes with webs", false));
   Config<Boolean> autoConfig = this.register(new BooleanConfig("Auto", "Fills holes when enemies are within a certain range", false));
   Config<Float> targetRangeConfig = this.register(
      new NumberConfig<>("TargetRange", "The range from the target to the hole", 0.5F, 3.0F, 5.0F, () -> this.autoConfig.getValue())
   );
   Config<Float> enemyRangeConfig = this.register(
      new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F, () -> this.autoConfig.getValue())
   );
   Config<Boolean> attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of hole fill", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 5));
   Config<Integer> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0, 1, 5));
   Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disables after filling all holes", false));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where blocks are being filled", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private int shiftDelay;
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private List<BlockPos> fills = new ArrayList<>();

   public HoleFillModule() {
      super("HoleFill", "Fills in nearby holes with blocks", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static HoleFillModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
      this.fills.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      int blocksPlaced = 0;
      if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.fills.clear();
      } else {
         int slot = this.websConfig.getValue() ? this.getBlockItemSlot(Blocks.COBWEB) : this.getResistantBlockItem().slot();
         if (slot == -1) {
            this.fills.clear();
         } else if (this.shiftDelayConfig.getValue() > 0 && this.shiftDelay < this.shiftDelayConfig.getValue()) {
            this.shiftDelay++;
         } else {
            List<BlockPos> holes = new ArrayList<>();

            for (Hole hole : Managers.HOLE.getHoles()) {
               if (!hole.isQuad()
                  && (!hole.isDouble() || this.doublesConfig.getValue())
                  && (hole.getSafety() != HoleType.OBSIDIAN || this.obsidianConfig.getValue())
                  && !(hole.squaredDistanceTo(mc.player) > ((NumberConfig)this.rangeConfig).getValueSq())
                  && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, hole.getPos(), CollisionContext.empty())) {
                  if (this.autoConfig.getValue()) {
                     for (Player entity : mc.level.players()) {
                        if (entity != mc.player && !Managers.SOCIAL.isFriend(entity.getName())) {
                           double dist = mc.player.distanceTo(entity);
                           if (!(dist > this.enemyRangeConfig.getValue().floatValue())
                              && (!(entity.getY() >= hole.y()) || !(hole.squaredDistanceTo(entity) > ((NumberConfig)this.targetRangeConfig).getValueSq()))) {
                              holes.add(hole.getPos());
                              break;
                           }
                        }
                     }
                  } else {
                     holes.add(hole.getPos());
                  }
               }
            }

            this.fills = holes;
            if (this.fills.isEmpty()) {
               if (this.autoDisableConfig.getValue()) {
                  this.disable();
               }
            } else {
               if (this.attackConfig.getValue()) {
                  this.attackBlockingCrystals(this.fills);
               }

               while (blocksPlaced < this.shiftTicksConfig.getValue() && blocksPlaced < this.fills.size()) {
                  BlockPos targetPos = this.fills.get(blocksPlaced);
                  blocksPlaced++;
                  this.shiftDelay = 0;
                  this.placeBlock(targetPos, slot);
               }

               if (this.rotateConfig.getValue()) {
                  Managers.ROTATION.setRotationSilentSync();
               }
            }
         }
      }
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

   private void placeBlock(BlockPos targetPos, int slot) {
      Managers.INTERACT.placeBlock(targetPos, slot, this.strictDirectionConfig.getValue(), false, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
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
         if (this.fills.isEmpty()) {
            return;
         }

         for (BlockPos pos : this.fills) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(pos, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   public boolean isPlacing() {
      return !this.fills.isEmpty();
   }
}
