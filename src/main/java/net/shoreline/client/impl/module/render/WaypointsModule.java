package net.shoreline.client.impl.module.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.api.waypoint.Waypoint;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.world.DimensionUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class WaypointsModule extends ToggleModule {
   private static WaypointsModule INSTANCE;
   Config<Boolean> logoutsConfig = this.register(new BooleanConfig("LogoutPoints", "Marks the position of player logouts", false));
   Config<Boolean> deathsConfig = this.register(new BooleanConfig("DeathPoints", "Marks the position of player deaths", false));
   Config<Boolean> coordsConfig = this.register(new BooleanConfig("Coords", "Shows the coordinates of the waypoint", true));
   Config<Boolean> distanceConfig = this.register(new BooleanConfig("Distance", "Shows the distance to the waypoint", true));
   DecimalFormat format = new DecimalFormat("0.0");
   private final Map<UUID, Player> loginPlayers = Maps.newConcurrentMap();
   private final Map<UUID, Player> logoutPlayers = Maps.newConcurrentMap();

   public WaypointsModule() {
      super("Waypoints", "Renders a waypoint at marked locations", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static WaypointsModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      Managers.WAYPOINT.clear();
      this.loginPlayers.clear();
      this.logoutPlayers.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.onDisable();
   }

   @EventListener
   public void onLoadWorld(LoadWorldEvent event) {
      this.onDisable();
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && this.logoutsConfig.getValue()) {
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
               this.handlePlayerListPackets(packet1);
            }
         } else {
            this.handlePlayerListPackets(event.getPacket());
         }
      }
   }

   public void handlePlayerListPackets(Packet<?> packet1) {
      if (packet1 instanceof ClientboundPlayerInfoUpdatePacket packet && packet.actions().contains(Action.ADD_PLAYER)) {
         for (Entry player : packet.newEntries()) {
            for (UUID uuid : this.logoutPlayers.keySet()) {
               if (uuid.equals(player.profile().getId())) {
                  this.logoutPlayers.remove(uuid);
               }
            }
         }

         this.loginPlayers.clear();
      } else if (packet1 instanceof ClientboundPlayerInfoRemovePacket packet) {
         for (UUID uuid2 : packet.profileIds()) {
            for (UUID uuidx : this.loginPlayers.keySet()) {
               if (uuidx.equals(uuid2)) {
                  Player player = this.loginPlayers.get(uuidx);
                  if (!this.logoutPlayers.containsKey(uuidx)) {
                     this.logoutPlayers.put(uuidx, player);
                  }
               }
            }
         }

         this.loginPlayers.clear();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         for (Player player : mc.level.players()) {
            if (player != null && !player.equals(mc.player)) {
               this.loginPlayers.put(player.getGameProfile().getId(), player);
            }
         }
      }
   }

   @EventListener
   public void onRemoveEntity(EntityDeathEvent event) {
      if (event.getEntity() instanceof LocalPlayer && this.deathsConfig.getValue()) {
         String serverIp = mc.isLocalServer() ? "Singleplayer" : Managers.NETWORK.getServerIp();
         Managers.WAYPOINT.removeContains("Last Death");
         Managers.WAYPOINT.register(new Waypoint("Last Death", serverIp, DimensionUtil.getDimension(), mc.player.getX(), mc.player.getY(), mc.player.getZ()));
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.level != null) {
         RenderBuffers.preRender();

         for (Waypoint waypoint : Managers.WAYPOINT.getWaypoints()) {
            if (waypoint.getIp().equalsIgnoreCase(mc.isLocalServer() ? "Singleplayer" : Managers.NETWORK.getServerIp())
               && DimensionUtil.getDimension() == waypoint.getDimension()) {
               this.renderWaypointBox(event.getMatrices(), waypoint.getPos(), waypoint.getName());
            }
         }

         if (this.logoutsConfig.getValue()) {
            for (UUID uuid : this.logoutPlayers.keySet()) {
               Player data = this.logoutPlayers.get(uuid);
               if (data != null) {
                  this.renderWaypointBox(event.getMatrices(), data.position(), data.getName().getString() + "'s Logout", data.isVisuallyCrawling());
               }
            }
         }

         RenderBuffers.postRender();
      }
   }

   private void renderWaypointBox(PoseStack matrixStack, Vec3 pos, String tag) {
      this.renderWaypointBox(matrixStack, pos, tag, false);
   }

   private void renderWaypointBox(PoseStack matrixStack, Vec3 pos, String tag, boolean crawlHitbox) {
      AABB waypointBox = crawlHitbox
         ? EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F).makeBoundingBox(pos)
         : EntityDimensions.fixed(0.6F, 2.2F).makeBoundingBox(pos);
      double center = (waypointBox.maxX - waypointBox.minX) / 2.0;
      RenderManager.renderBoundingBox(matrixStack, waypointBox, 1.5F, ColorsModule.getInstance().getRGB(255));
      int dist = (int)Math.sqrt(mc.player.distanceToSqr(pos));
      String waypointTag = "§7"
         + tag
         + (
            this.coordsConfig.getValue()
               ? String.format(" XYZ %s %s %s", this.format.format(pos.x()), this.format.format(pos.y()), this.format.format(pos.z()))
               : ""
         )
         + (this.distanceConfig.getValue() ? String.format(" %sm", dist) : "");
      RenderManager.renderSign(waypointTag, waypointBox.minX + center, waypointBox.maxY + 0.4, waypointBox.minZ + center, -1);
   }
}
