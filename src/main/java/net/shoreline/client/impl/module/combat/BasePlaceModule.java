package net.shoreline.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.ExplosionUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class BasePlaceModule extends ObsidianPlacerModule {
   Config<Float> placeRangeConfig = this.register(new NumberConfig<>("PlaceRange", "The placement range for bases", 0.0F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Float> minDamageConfig = this.register(new NumberConfig<>("MinDamage", "Minimum damage required to place base", 1.0F, 4.0F, 10.0F));
   Config<Boolean> assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where base blocks are being placed", true));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private BlockPos crystalBase;
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();

   public BasePlaceModule() {
      super("BasePlace", "Places obsidian for crystal placements", ModuleCategory.COMBAT);
   }

   @Override
   public void onDisable() {
      this.crystalBase = null;
      this.packets.clear();
      this.fadeList.clear();
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (AutoCrystalModule.getInstance().isEnabled() && !AutoCrystalModule.getInstance().isPlacing()) {
         Player target = this.getClosestPlayer(this.enemyRangeConfig.getValue().floatValue());
         if (target != null) {
            this.crystalBase = this.getCrystalBase(target);
            if (this.crystalBase != null) {
               BlockState state = mc.level.getBlockState(this.crystalBase);
               int slot = this.getResistantBlockItem().slot();
               if (slot != -1 && state.canBeReplaced()) {
                  this.placeBlock(this.crystalBase, slot);
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
         if (this.crystalBase != null && mc.level.isEmptyBlock(this.crystalBase)) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.crystalBase, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue()) {
            if (state) {
               Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            } else {
               Managers.ROTATION.setRotationSilentSync();
            }
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
   }

   private BlockPos getCrystalBase(Player player) {
      List<BlockPos> targetBlocks = this.getSphere(this.placeRangeConfig.getValue().floatValue(), mc.player.getEyePosition());
      double damage = 0.0;
      BlockPos crystalBase = null;

      for (BlockPos pos : targetBlocks) {
         BlockPos basePos = pos.below();
         if (basePos.getY() < EntityUtil.getRoundedBlockPos(player).getY()) {
            Long placed = this.packets.get(basePos);
            if ((
                  !(this.shiftDelayConfig.getValue() > 0.0F)
                     || placed == null
                     || !((float)(System.currentTimeMillis() - placed) < this.shiftDelayConfig.getValue() * 50.0F)
               )
               && AutoCrystalModule.getInstance().isCrystalHitboxClear(pos)) {
               double dist = mc.player.distanceToSqr(basePos.getCenter());
               if (!(dist > ((NumberConfig)this.placeRangeConfig).getValueSq())) {
                  double dmg1 = ExplosionUtil.getDamageTo(player, pos.getCenter(), this.assumeArmorConfig.getValue());
                  if (!(dmg1 < this.minDamageConfig.getValue().floatValue())
                     && (
                        AirPlaceModule.getInstance().isEnabled()
                           || Managers.INTERACT.getInteractDirectionInternal(basePos, this.strictDirectionConfig.getValue()) != null
                     )
                     && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, basePos, CollisionContext.empty())
                     && dmg1 > damage) {
                     crystalBase = basePos;
                     damage = dmg1;
                  }
               }
            }
         }
      }

      return crystalBase;
   }

   private List<BlockPos> getSphere(double rad, Vec3 origin) {
      List<BlockPos> sphere = new ArrayList<>();

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.x() + x), (int)(origin.y() + y), (int)(origin.z() + z));
               BlockPos p = new BlockPos(pos);
               sphere.add(p);
            }
         }
      }

      return sphere;
   }
}
