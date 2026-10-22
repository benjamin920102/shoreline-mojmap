package net.shoreline.client.impl.module.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.gui.screen.MouseDraggedEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class BetterInvModule extends ToggleModule {
   Config<Boolean> dragItemMoveConfig = this.register(new BooleanConfig("DragItemMove", "Allows you to hold down shift and drag move items.", true));
   Config<Boolean> fastDropConfig = this.register(new BooleanConfig("FastDrop", "Drops items from the hotbar faster", false));
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "The delay for dropping items", 0, 0, 4, () -> this.fastDropConfig.getValue()));
   private int dropTicks;

   public BetterInvModule() {
      super("BetterInv", "Makes your inventory better", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.fastDropConfig.getValue()) {
         if (mc.options.keyDrop.isDown() && this.dropTicks > this.delayConfig.getValue()) {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            this.dropTicks = 0;
         }

         this.dropTicks++;
      }
   }

   @EventListener
   public void onMouseDragged(MouseDraggedEvent event) {
      if (this.dragItemMoveConfig.getValue()) {
         event.cancel();
      }
   }
}
