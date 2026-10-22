package net.shoreline.client.impl.event.network;

import net.minecraft.client.player.Input;
import net.shoreline.eventbus.event.Event;

public class MovementSlowdownEvent extends Event {
   public final Input input;

   public MovementSlowdownEvent(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }
}
