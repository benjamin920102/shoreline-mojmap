package net.shoreline.client.impl.module.render;

import net.minecraft.world.entity.player.Player;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.render.entity.RenderEntityInvisibleEvent;
import net.shoreline.eventbus.annotation.EventListener;

public final class TrueSightModule extends ToggleModule {
   Config<Boolean> onlyPlayersConfig = this.register(new BooleanConfig("OnlyPlayers", "Only reveal invisible players", true));

   public TrueSightModule() {
      super("TrueSight", "Allows you to see invisible entities", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderEntityInvisible(RenderEntityInvisibleEvent event) {
      if (event.getEntity().isInvisible() && (!this.onlyPlayersConfig.getValue() || event.getEntity() instanceof Player)) {
         event.cancel();
      }
   }
}
