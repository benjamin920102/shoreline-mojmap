package net.shoreline.client.impl.module.world;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.item.BlockItem;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.BreakBlockEvent;
import net.shoreline.client.impl.event.network.InteractBlockEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class NoGlitchBlocksModule extends ToggleModule {
   Config<Boolean> placeConfig = this.register(new BooleanConfig("Place", "Places blocks only after the server confirms", false));
   Config<Boolean> destroyConfig = this.register(new BooleanConfig("Destroy", "Destroys blocks only after the server confirms", false));

   public NoGlitchBlocksModule() {
      super("NoGlitchBlocks", "Prevents blocks from being glitched in the world", ModuleCategory.WORLD);
   }

   @EventListener
   public void onInteractBlock(InteractBlockEvent event) {
      if (this.placeConfig.getValue() && mc.player.getItemInHand(event.getHand()).getItem() instanceof BlockItem && !mc.isLocalServer()) {
         event.cancel();
         Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(event.getHand(), event.getHitResult(), id));
         mc.player.swing(event.getHand());
      }
   }

   @EventListener
   public void onBreakBlock(BreakBlockEvent event) {
      if (this.destroyConfig.getValue() && !mc.isLocalServer()) {
         event.cancel();
      }
   }
}
