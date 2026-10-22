package net.shoreline.client.impl.manager.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.shoreline.client.api.waypoint.UserWaypoint;
import net.shoreline.client.api.waypoint.Waypoint;

public class WaypointManager {
   private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();

   public void register(Waypoint waypoint) {
      this.waypoints.add(waypoint);
   }

   public void register(Waypoint... waypoints) {
      for (Waypoint waypoint : waypoints) {
         this.register(waypoint);
      }
   }

   public boolean remove(Waypoint waypoint) {
      return this.waypoints.remove(waypoint);
   }

   public boolean removeContains(String waypoint) {
      return this.waypoints.removeIf(w -> w.getName().contains(waypoint));
   }

   public boolean remove(String waypoint) {
      return this.waypoints.removeIf(w -> w.getName().equalsIgnoreCase(waypoint));
   }

   public boolean contains(String waypoint) {
      return this.waypoints.stream().anyMatch(w -> w.getName().contains(waypoint));
   }

   public void clear() {
      this.waypoints.removeIf(w -> !(w instanceof UserWaypoint));
   }

   public List<Waypoint> getWaypoints() {
      return this.waypoints;
   }

   public List<String> getIps() {
      List<String> ips = new ArrayList<>();

      for (Waypoint waypoint : this.getWaypoints()) {
         ips.add(waypoint.getIp());
      }

      return ips;
   }
}
