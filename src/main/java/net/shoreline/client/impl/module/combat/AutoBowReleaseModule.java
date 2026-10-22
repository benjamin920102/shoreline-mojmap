package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AutoBowReleaseModule extends ToggleModule {
   Config<Boolean> crossbowConfig = this.register(new BooleanConfig("Crossbow", "Automatically releases crossbow when fully charged", false));
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("Ticks", "Ticks before releasing the bow charge", 3, 5, 20));
   Config<Boolean> tpsSyncConfig = this.register(new BooleanConfig("TPS-Sync", "Sync bow release to server ticks", false));

   public AutoBowReleaseModule() {
      super("AutoBowRelease", "Automatically releases a charged bow", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (!SelfBowModule.getInstance().isEnabled()) {
         if (event.getStage() == StageEvent.EventStage.POST) {
            ItemStack mainhand = mc.player.getMainHandItem();
            if (mainhand.getItem() == Items.BOW) {
               float off = this.tpsSyncConfig.getValue() ? 20.0F - Managers.TICK.getTpsAverage() : 0.0F;
               if (mc.player.getTicksUsingItem() + off >= this.ticksConfig.getValue().intValue()) {
                  Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
                  mc.player.releaseUsingItem();
               }
            } else if (this.crossbowConfig.getValue()
               && mainhand.getItem() == Items.CROSSBOW
               && (float)mc.player.getTicksUsingItem() / CrossbowItem.getChargeDuration(mc.player.getMainHandItem(), mc.player) > 1.0F) {
               Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
               mc.player.releaseUsingItem();
               mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
         }
      }
   }
}
