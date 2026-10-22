package net.shoreline.client.impl.manager.anticheat;

import net.minecraft.world.phys.Vec3;

public record SetbackData(Vec3 position, long timeMS, int teleportID) {
   public long timeSince() {
      return System.currentTimeMillis() - this.timeMS;
   }
}
