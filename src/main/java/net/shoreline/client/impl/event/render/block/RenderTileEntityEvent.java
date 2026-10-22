package net.shoreline.client.impl.event.render.block;

import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

public class RenderTileEntityEvent extends Event {
   @Cancelable
   public static class EnchantingTableBook extends RenderTileEntityEvent {
   }
}
