package net.shoreline.client.impl.manager.combat;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class TotemManager implements Globals {
   private final ConcurrentMap<UUID, TotemManager.TotemData> totems = new ConcurrentHashMap<>();

   public TotemManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.level != null) {
         if (event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35) {
            Entity entity = packet.getEntity(mc.level);
            if (entity != null && entity.isAlive()) {
               if (this.totems.containsKey(entity.getUUID())) {
                  this.totems
                     .replace(entity.getUUID(), new TotemManager.TotemData(System.currentTimeMillis(), this.totems.get(entity.getUUID()).getPops() + 1));
               } else {
                  this.totems.put(entity.getUUID(), new TotemManager.TotemData(System.currentTimeMillis(), 1));
               }
            }
         }
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onRemoveEntity(EntityDeathEvent event) {
      this.totems.remove(event.getEntity().getUUID());
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.totems.clear();
   }

   public int getTotems(Entity entity) {
      return this.totems.getOrDefault(entity.getUUID(), new TotemManager.TotemData(0L, 0)).getPops();
   }

   public long getLastPopTime(Entity entity) {
      return this.totems.getOrDefault(entity.getUUID(), new TotemManager.TotemData(-1L, 0)).getLastPopTime();
   }

   public static class TotemData {
      private final long lastPopTime;
      private final int pops;

      public TotemData(long lastPopTime, int pops) {
         this.lastPopTime = lastPopTime;
         this.pops = pops;
      }

      public int getPops() {
         return this.pops;
      }

      public long getLastPopTime() {
         return this.lastPopTime;
      }
   }
}
