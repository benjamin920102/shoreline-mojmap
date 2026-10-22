package net.shoreline.client.impl.manager.world;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.module.world.SpeedmineModule;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class BlockManager implements Globals {
   private final List<BlockManager.BreakEntry> breakPositions = new CopyOnWriteArrayList<>();

   public BlockManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onTick(PlayerUpdateEvent event) {
      if (mc.player != null && mc.level != null) {
         for (BlockManager.BreakEntry blockEntry : this.breakPositions) {
            blockEntry.updateDamage();
         }
      } else {
         this.breakPositions.clear();
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
            if (this.countBreaks(packet.getId()) >= 2L) {
               this.breakPositions
                  .stream()
                  .filter(d -> d.getEntityId() == packet.getId())
                  .min(Comparator.comparingLong(BlockManager.BreakEntry::getStartTime))
                  .ifPresent(this.breakPositions::remove);
            }

            BlockManager.BreakEntry data = new BlockManager.BreakEntry(packet.getId(), packet.getPos());
            data.startMining();
            this.breakPositions.add(data);
         }
      }
   }

   public long countBreaks(int entityId) {
      return this.breakPositions.stream().filter(d -> d.getEntityId() == entityId).count();
   }

   public boolean isInstantMine(BlockPos pos) {
      return this.breakPositions.getFirst().getPos().equals(pos);
   }

   public boolean isBreaking(BlockPos pos) {
      return this.breakPositions.stream().anyMatch(d -> d.getPos().equals(pos));
   }

   public boolean isPassed(BlockPos pos, float blockDamage) {
      return this.breakPositions.stream().anyMatch(d -> d.getPos().equals(pos) && d.getBlockDamage() >= blockDamage);
   }

   public Set<BlockPos> getMines(float blockDamage) {
      return this.breakPositions.stream().filter(d -> this.isPassed(d.getPos(), blockDamage)).map(BlockManager.BreakEntry::getPos).collect(Collectors.toSet());
   }

   public static class BreakEntry {
      private final int entityId;
      private final BlockPos pos;
      private long startTime;
      private float blockDamage;
      private boolean started;

      public BreakEntry(int entityId, BlockPos pos) {
         this.entityId = entityId;
         this.pos = pos;
      }

      public void updateDamage() {
         if (this.started) {
            this.blockDamage = this.blockDamage
               + SpeedmineModule.getInstance().calcBlockBreakingDelta(Globals.mc.level.getBlockState(this.pos), Globals.mc.level, this.pos);
         }
      }

      public void startMining() {
         this.started = true;
         this.startTime = System.currentTimeMillis();
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public float getBlockDamage() {
         return Math.min(this.blockDamage, 1.0F);
      }

      public int getEntityId() {
         return this.entityId;
      }

      public long getStartTime() {
         return this.startTime;
      }
   }
}
