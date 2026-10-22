package net.shoreline.client.impl.module.misc;

import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.eventbus.annotation.EventListener;

public class FakePlayerModule extends ToggleModule {
   private FakePlayerEntity fakePlayer;

   public FakePlayerModule() {
      super("FakePlayer", "Spawns an indestructible client-side player", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.level != null) {
         this.fakePlayer = new FakePlayerEntity(mc.player, "FakePlayer");
         this.fakePlayer.spawnPlayer();
      }
   }

   @Override
   public void onDisable() {
      if (this.fakePlayer != null) {
         this.fakePlayer.despawnPlayer();
         this.fakePlayer = null;
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.fakePlayer = null;
      this.disable();
   }

   @EventListener
   public void onPushEntity(PushEntityEvent event) {
      if (event.getPushed().equals(mc.player) && event.getPusher().equals(this.fakePlayer)) {
         event.setCanceled(true);
      }
   }
}
