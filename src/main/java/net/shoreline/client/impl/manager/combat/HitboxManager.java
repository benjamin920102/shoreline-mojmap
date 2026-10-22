package net.shoreline.client.impl.manager.combat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData.DataItem;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class HitboxManager implements Globals {
   private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

   public HitboxManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundSetEntityDataPacket packet) {
            Entity entity = mc.level.getEntity(packet.id());
            if (!(entity instanceof Player)) {
               return;
            }

            for (DataValue<?> serializedEntry : packet.packedItems()) {
               DataItem<?> entry = entity.getEntityData().itemsById[serializedEntry.id()];
               if (entry.getAccessor().equals(Entity.DATA_POSE)) {
                  if (!serializedEntry.value().equals(Pose.SWIMMING)) {
                     this.serverCrawling.remove(entity);
                  } else if (!this.serverCrawling.contains(entity)) {
                     this.serverCrawling.add(entity);
                  }
               }
            }
         }
      }
   }

   public boolean isServerCrawling(Entity entity) {
      return this.serverCrawling.contains(entity);
   }

   public AABB getCrawlingBoundingBox(Entity entity) {
      return entity.getDimensions(Pose.SWIMMING).makeBoundingBox(entity.position());
   }
}
