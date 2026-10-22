package net.shoreline.client.impl.module.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
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

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;

/**
 * @author linus
 * @since 1.0
 */
public class WaypointsModule extends ToggleModule
{
    private static WaypointsModule INSTANCE;

    Config<Boolean> logoutsConfig = register(new BooleanConfig("LogoutPoints", "Marks the position of player logouts", false));
    Config<Boolean> deathsConfig = register(new BooleanConfig("DeathPoints", "Marks the position of player deaths", false));
    Config<Boolean> coordsConfig = register(new BooleanConfig("Coords", "Shows the coordinates of the waypoint", true));
    Config<Boolean> distanceConfig = register(new BooleanConfig("Distance", "Shows the distance to the waypoint", true));
    DecimalFormat format = new DecimalFormat("0.0");

    private final Map<UUID, Player> loginPlayers = Maps.newConcurrentMap();
    private final Map<UUID, Player> logoutPlayers = Maps.newConcurrentMap();

    public WaypointsModule()
    {
        super("Waypoints", "Renders a waypoint at marked locations", ModuleCategory.RENDER);
        INSTANCE = this;
    }

    public static WaypointsModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        Managers.WAYPOINT.clear();
        loginPlayers.clear();
        logoutPlayers.clear();
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event)
    {
        onDisable();
    }

    @EventListener
    public void onLoadWorld(LoadWorldEvent event)
    {
        onDisable();
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || !logoutsConfig.getValue())
        {
            return;
        }

        if (event.getPacket() instanceof ClientboundBundlePacket packet)
        {
            for (Packet<?> packet1 : packet.subPackets())
            {
                handlePlayerListPackets(packet1);
            }
        }

        else
        {
            handlePlayerListPackets(event.getPacket());
        }
    }

    public void handlePlayerListPackets(Packet<?> packet1)
    {
        if (packet1 instanceof ClientboundPlayerInfoUpdatePacket packet
                && packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER))
        {
            for (ClientboundPlayerInfoUpdatePacket.Entry player : packet.newEntries())
            {
                for (UUID uuid : logoutPlayers.keySet())
                {
                    if (!uuid.equals(player.profile().getId()))
                    {
                        continue;
                    }
                    logoutPlayers.remove(uuid);
                }
            }
            loginPlayers.clear();
        }

        else if (packet1 instanceof ClientboundPlayerInfoRemovePacket packet)
        {
            for (UUID uuid2 : packet.profileIds())
            {
                for (UUID uuid : loginPlayers.keySet())
                {
                    if (!uuid.equals(uuid2))
                    {
                        continue;
                    }
                    final Player player = loginPlayers.get(uuid);
                    if (!logoutPlayers.containsKey(uuid))
                    {
                        logoutPlayers.put(uuid, player);
                    }
                }
            }
            loginPlayers.clear();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.POST)
        {
            return;
        }
        for (Player player : mc.level.players())
        {
            if (player == null || player.equals(mc.player))
            {
                continue;
            }
            loginPlayers.put(player.getGameProfile().getId(), player);
        }
    }

    @EventListener
    public void onRemoveEntity(EntityDeathEvent event)
    {
        if (event.getEntity() instanceof LocalPlayer && deathsConfig.getValue())
        {
            String serverIp = mc.isLocalServer() ? "Singleplayer" : Managers.NETWORK.getServerIp();
            Managers.WAYPOINT.removeContains("Last Death");
            Managers.WAYPOINT.register(new Waypoint("Last Death", serverIp, DimensionUtil.getDimension(),
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        RenderBuffers.preRender();
        for (Waypoint waypoint : Managers.WAYPOINT.getWaypoints())
        {
            if (!waypoint.getIp().equalsIgnoreCase(mc.isLocalServer() ? "Singleplayer" : Managers.NETWORK.getServerIp()) || DimensionUtil.getDimension() != waypoint.getDimension())
            {
                continue;
            }
            renderWaypointBox(event.getMatrices(), waypoint.getPos(), waypoint.getName());
        }

        if (logoutsConfig.getValue())
        {
            for (UUID uuid : logoutPlayers.keySet())
            {
                final Player data = logoutPlayers.get(uuid);
                if (data == null)
                {
                    continue;
                }
                renderWaypointBox(event.getMatrices(), data.position(),
                        data.getName().getString() + "'s Logout", data.isVisuallyCrawling());
            }
        }

        RenderBuffers.postRender();
    }

    private void renderWaypointBox(PoseStack matrixStack, Vec3 pos, String tag)
    {
        renderWaypointBox(matrixStack, pos, tag, false);
    }

    private void renderWaypointBox(PoseStack matrixStack, Vec3 pos, String tag, boolean crawlHitbox)
    {
        AABB waypointBox = crawlHitbox ? EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f).makeBoundingBox(pos) : EntityDimensions.fixed(0.6f, 2.2f).makeBoundingBox(pos);
        double center = (waypointBox.maxX - waypointBox.minX) / 2.0f;
        RenderManager.renderBoundingBox(matrixStack, waypointBox, 1.5f, ColorsModule.getInstance().getRGB(255));
        int dist = (int) Math.sqrt(mc.player.distanceToSqr(pos));
        String waypointTag = "§7" + tag + (coordsConfig.getValue() ? String.format(" XYZ %s %s %s", format.format(pos.x()), format.format(pos.y()), format.format(pos.z())) : "")
                + (distanceConfig.getValue() ? String.format(" %sm", dist) : "");
        RenderManager.renderSign(waypointTag, waypointBox.minX + center, waypointBox.maxY + 0.4, waypointBox.minZ + center, -1);
    }
}
