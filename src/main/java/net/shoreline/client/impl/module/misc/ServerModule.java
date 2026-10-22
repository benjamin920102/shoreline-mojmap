package net.shoreline.client.impl.module.misc;

import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.gui.screen.MenuDisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

import java.time.Instant;
import java.util.BitSet;

import static net.minecraft.network.protocol.game.ClientboundGameEventPacket.DEMO_EVENT;

/**
 * @author xgraza
 * @since 1.0
 */
public final class ServerModule extends ConcurrentModule
{
    Config<Boolean> demoConfig = register(new BooleanConfig("NoDemo", "Prevents servers from forcing you to a demo screen", true));
    Config<Boolean> resourcePackConfig = register(new BooleanConfig("NoResourcePack", "Prevents server from forcing resource pack", false));
    Config<Boolean> antiCrashConfig = register(new BooleanConfig("NoServerCrash", "Prevents server packets from crashing the client", false));
    Config<Boolean> illegalDisconnectConfig = register(new BooleanConfig("IllegalDisconnect", "Disconnects by getting kicked from server", false));

    public ServerModule()
    {
        super("Server", "Prevents servers actions on player", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onPacketInbound(final PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof ClientboundGameEventPacket packet)
        {
            if (packet.getEvent() == DEMO_EVENT && !mc.isDemo() && demoConfig.getValue())
            {
                Shoreline.info("Server attempted to use Demo mode features on you!");
                event.cancel();
            }
        }
        if (event.getPacket() instanceof ClientboundResourcePackPushPacket && resourcePackConfig.getValue())
        {
            event.cancel();
            Managers.NETWORK.sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), ServerboundResourcePackPacket.Action.DECLINED));
        }
        if (mc.level == null)
        {
            return;
        }
        if (antiCrashConfig.getValue())
        {
            // Out of bounds packets from server
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet
                    && (packet.getX() > 30000000 || packet.getY() > mc.level.getMaxBuildHeight()
                    || packet.getZ() > 30000000 || packet.getX() < -30000000
                    || packet.getY() < mc.level.getMinBuildHeight() || packet.getZ() < -30000000))
            {
                event.cancel();
            }
            else if (event.getPacket() instanceof ClientboundExplodePacket packet
                    && (packet.getX() > 30000000 || packet.getY() > mc.level.getMaxBuildHeight()
                    || packet.getZ() > 30000000 || packet.getX() < -30000000
                    || packet.getY() < mc.level.getMinBuildHeight() || packet.getZ() < -30000000
                    || packet.getPower() > 1000 || packet.getToBlow().size() > 1000
                    || packet.getKnockbackX() > 1000 || packet.getKnockbackY() > 1000
                    || packet.getKnockbackZ() > 1000 || packet.getKnockbackX() < -1000
                    || packet.getKnockbackY() < -1000 || packet.getKnockbackZ() < -1000))
            {
                event.cancel();
            }
            else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
                    && (packet.getXa() > 1000 || packet.getYa() > 1000 ||
                    packet.getZa() > 1000 || packet.getXa() < -1000 ||
                    packet.getYa() < -1000 || packet.getZa() < -1000))
            {
                event.cancel();
            }
            else if (event.getPacket() instanceof ClientboundLevelParticlesPacket packet && packet.getCount() > 500)
            {
                event.cancel();
            }
        }
    }

    @EventListener
    public void onMenuDisconnect(MenuDisconnectEvent event)
    {
        if (illegalDisconnectConfig.getValue())
        {
            // event.cancel();
            Managers.NETWORK.sendPacket(new ServerboundChatPacket(
                    "§",
                    Instant.now(),
                    Crypt.SaltSupplier.getLong(),
                    null,
                    new LastSeenMessages.Update(1, new BitSet(2)))); // Illegal packet
        }
    }
}
