package net.shoreline.client.impl.manager.network;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.shoreline.client.impl.event.gui.screen.ConnectScreenEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.imixin.IClientPlayNetworkHandler;
import net.shoreline.client.mixin.accessor.AccessorClientWorld;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.PerSecondCounter;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * @author linus
 * @since 1.0
 */
public class NetworkManager implements Globals
{
    private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();

    private ServerAddress address;
    private ServerData info;

    private final PerSecondCounter outgoingCounter = new PerSecondCounter();
    private final PerSecondCounter incomingCounter = new PerSecondCounter();

    public NetworkManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    @EventListener
    public void onConnect(ConnectScreenEvent event)
    {
        address = event.getAddress();
        info = event.getInfo();
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event)
    {
        PACKET_CACHE.clear();
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        outgoingCounter.updateCounter();
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Inbound event)
    {
        incomingCounter.updateCounter();
    }

    public void connect(final ServerAddress address, final ServerData info)
    {
        if (mc.getConnection() == null)
        {
            return;
        }
        mc.getConnection().getConnection().initiateServerboundPlayConnection(address.getHost(), address.getPort(),
                new ClientHandshakePacketListenerImpl(mc.getConnection().getConnection(), mc, info, null, false, null, null, null));
    }

    /**
     * @param p
     */
    public void sendPacket(final Packet<?> p)
    {
        if (mc.getConnection() != null)
        {
            PACKET_CACHE.add(p);
            mc.getConnection().send(p);
        }
    }

    public void sendQuietPacket(final Packet<?> p)
    {
        if (mc.getConnection() != null)
        {
            PACKET_CACHE.add(p);
            ((IClientPlayNetworkHandler) mc.getConnection()).sendQuietPacket(p);
        }
    }

    /**
     * @param p
     */
    public void sendSequencedPacket(final PredictiveAction p)
    {
        if (mc.level != null)
        {
            BlockStatePredictionHandler updater =
                    ((AccessorClientWorld) mc.level).hookGetPendingUpdateManager().startPredicting();
            try
            {
                int i = updater.currentSequence();
                Packet<ServerGamePacketListener> packet = p.predict(i);
                sendPacket(packet);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                if (updater != null)
                {
                    try
                    {
                        updater.close();
                    }
                    catch (Throwable e1)
                    {
                        e1.printStackTrace();
                        e.addSuppressed(e1);
                    }
                }
                throw e;
            }
            if (updater != null)
            {
                updater.close();
            }
        }
    }

    /**
     * @return
     */
    public int getClientLatency()
    {
        if (mc.getConnection() != null)
        {
            final PlayerInfo playerEntry =
                    mc.getConnection().getPlayerInfo(mc.player.getGameProfile().getId());
            if (playerEntry != null)
            {
                return playerEntry.getLatency();
            }
        }
        return 0;
    }

    public ServerAddress getAddress()
    {
        return address;
    }

    public void setAddress(ServerAddress address)
    {
        this.address = address;
    }

    public ServerData getInfo()
    {
        return info;
    }

    public void setInfo(ServerData info)
    {
        this.info = info;
    }

    public boolean isCrystalPvpCC()
    {
        return getServerIp().contains("crystalpvp.cc");
    }

    public boolean is2b2t()
    {
        return getServerIp().contains("2b2t.org");
    }

    public int getOutgoingPPS()
    {
        return outgoingCounter.getPerSecond();
    }

    public int getIncomingPPS()
    {
        return incomingCounter.getPerSecond();
    }

    public String getServerIp()
    {
        if (info != null)
        {
            return info.ip;
        }
        return "Singleplayer";
    }

    /**
     * @param p
     * @return
     */
    public boolean isCached(Packet<?> p)
    {
        return PACKET_CACHE.contains(p);
    }
}
