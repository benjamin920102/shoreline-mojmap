package net.shoreline.client.impl.event.gui.hud;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class PlayerListNameEvent extends Event {
   private Component playerName;
   private final UUID id;

   public PlayerListNameEvent(Component playerName, UUID id) {
      this.playerName = playerName;
      this.id = id;
   }

   public void setPlayerName(Component playerName) {
      this.playerName = playerName;
   }

   public Component getPlayerName() {
      return this.playerName;
   }

   public UUID getId() {
      return this.id;
   }
}
