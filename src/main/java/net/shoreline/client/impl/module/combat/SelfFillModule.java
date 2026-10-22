package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class SelfFillModule extends ObsidianPlacerModule {
   Config<SelfFillModule.Mode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for block fill", SelfFillModule.Mode.BLOCK_LAG, SelfFillModule.Mode.values())
   );
   Config<Boolean> attackConfig = this.register(
      new BooleanConfig("Attack", "Attacks crystals in the way of block", true, () -> this.modeConfig.getValue() == SelfFillModule.Mode.BLOCK_LAG)
   );
   Config<Boolean> autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Automatically disables after placing block", false));
   private double prevY;

   public SelfFillModule() {
      super("SelfFill", "Fills in the block your standing on", ModuleCategory.COMBAT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (Math.abs(mc.player.getY() - this.prevY) > 0.5) {
         this.disable();
      } else if (!this.checkMultitask() || this.multitaskConfig.getValue()) {
         boolean inBlock = PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream().anyMatch(p -> !mc.level.getBlockState(p).canBeReplaced());
         BlockPos pos = EntityUtil.getRoundedBlockPos(mc.player);
         if (!inBlock && mc.player.onGround()) {
            if (this.modeConfig.getValue() == SelfFillModule.Mode.BLOCK_LAG) {
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + 1.16, mc.player.getZ(), true));
               if (!mc.level.noCollision(mc.player.getBoundingBox().move(0.0, 2.34, 0.0))) {
                  return;
               }

               double y = mc.player.getY();
               Managers.POSITION.setPositionClient(mc.player.getX(), y + 1.167, mc.player.getZ());
               this.attackPlace(pos);
               Managers.POSITION.setPositionClient(mc.player.getX(), y, mc.player.getZ());
               Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY() + 2.34, mc.player.getZ(), false));
            } else if (this.modeConfig.getValue() == SelfFillModule.Mode.WEB) {
               int slot = this.getBlockItemSlot(Blocks.COBWEB);
               if (slot == -1) {
                  return;
               }

               Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, (state, angles) -> {
                  if (this.rotateConfig.getValue()) {
                     if (state) {
                        Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                     } else {
                        Managers.ROTATION.setRotationSilentSync();
                     }
                  }
               });
            }
         }

         if (this.autoDisableConfig.getValue()) {
            this.disable();
         }
      }
   }

   private void attackPlace(BlockPos targetPos) {
      ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
      if (blockSlot != null) {
         int slot = blockSlot.slot();
         if (slot != -1) {
            this.attackPlace(targetPos, slot);
         }
      }
   }

   private void attackPlace(BlockPos targetPos, int slot) {
      if (this.attackConfig.getValue()) {
         Entity entity = mc.level.getEntities(null, new AABB(targetPos)).stream().filter(e -> e instanceof EndCrystal).findFirst().orElse(null);
         if (entity != null) {
            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(entity, mc.player.isShiftKeyDown()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         }
      }

      Managers.INTERACT.placeBlock(targetPos, slot, false, this.strictDirectionConfig.getValue(), false, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
   }

   private static enum Mode {
      BLOCK_LAG,
      WEB;
   }
}
