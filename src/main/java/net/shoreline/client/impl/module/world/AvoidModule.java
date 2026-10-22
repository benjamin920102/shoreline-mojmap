package net.shoreline.client.impl.module.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.PlayerClimbEvent;
import net.shoreline.client.impl.event.world.BlockCollisionEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.world.BlockUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AvoidModule extends ToggleModule {
   Config<Boolean> voidConfig = this.register(new BooleanConfig("Void", "Prevents player from falling into the void", true));
   Config<Boolean> fireConfig = this.register(new BooleanConfig("Fire", "Prevents player from walking into fire", false));
   Config<Boolean> berryBushConfig = this.register(new BooleanConfig("BerryBush", "Prevents player from walking into sweet berry bushes", false));
   Config<Boolean> cactiConfig = this.register(new BooleanConfig("Cactus", "Prevents player from walking into cacti", false));
   Config<Boolean> unloadedConfig = this.register(new BooleanConfig("Unloaded", "Prevents player from entering chunks that haven't been loaded", false));
   Config<Boolean> noClimbConfig = this.register(new BooleanConfig("Climb", "Prevents player from climbing up blocks", false));

   public AvoidModule() {
      super("Avoid", "Prevents player from entering harmful areas", ModuleCategory.WORLD);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE
         && this.voidConfig.getValue()
         && !mc.player.isSpectator()
         && mc.player.getY() < mc.level.getMinBuildHeight()) {
         Managers.MOVEMENT.setMotionY(0.0);
      }
   }

   @EventListener
   public void onBlockCollision(BlockCollisionEvent event) {
      BlockPos pos = event.getPos();
      if (this.fireConfig.getValue() && event.getBlock() == Blocks.FIRE && mc.player.getY() < pos.getY() + 1.0
         || this.cactiConfig.getValue() && event.getBlock() == Blocks.CACTUS
         || this.berryBushConfig.getValue() && event.getBlock() == Blocks.SWEET_BERRY_BUSH
         || this.unloadedConfig.getValue() && !BlockUtil.isBlockLoaded(pos.getX(), pos.getZ())) {
         event.cancel();
         event.setVoxelShape(Shapes.block());
      }
   }

   @EventListener
   public void onClimb(PlayerClimbEvent event) {
      if (this.noClimbConfig.getValue()) {
         event.cancel();
      }
   }
}
