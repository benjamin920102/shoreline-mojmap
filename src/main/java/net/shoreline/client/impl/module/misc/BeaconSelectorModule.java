package net.shoreline.client.impl.module.misc;

import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.ScreenOpenEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.gui.beacon.BeaconSelectorScreen;
import net.shoreline.eventbus.annotation.EventListener;

public class BeaconSelectorModule extends ToggleModule {
   private MobEffect primaryEffect;
   private MobEffect secondaryEffect;
   private boolean customBeacon;

   public BeaconSelectorModule() {
      super("BeaconSelector", "Allows you to change beacon effects", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundSetBeaconPacket var2) {
         ;
      }
   }

   @EventListener
   public void onScreenOpen(ScreenOpenEvent event) {
      if (event.getScreen() instanceof BeaconScreen screen && !this.customBeacon) {
         event.cancel();
         this.customBeacon = true;
         mc.setScreen(new BeaconSelectorScreen(screen.getMenu(), mc.player.getInventory(), screen.getTitle()));
         this.customBeacon = false;
      }
   }
}
