package net.shoreline.client.impl.event.gui.screen;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.shoreline.eventbus.event.Event;

public class ConnectScreenEvent extends Event {
   private final ServerAddress address;
   private final ServerData info;

   public ConnectScreenEvent(ServerAddress address, ServerData info) {
      this.address = address;
      this.info = info;
   }

   public ServerAddress getAddress() {
      return this.address;
   }

   public ServerData getInfo() {
      return this.info;
   }
}
