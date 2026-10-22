package net.shoreline.client.impl.module.movement;

import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author linus
 * @since 1.0
 */
public class FakeLagModule extends ToggleModule
{

    //
    Config<LagMode> modeConfig = register(new EnumConfig<>("Mode", "The mode for caching packets", LagMode.BLINK, LagMode.values()));
    Config<Boolean> pulseConfig = register(new BooleanConfig("Pulse", "Releases packets at intervals", false));
    Config<Float> factorConfig = register(new NumberConfig<>("Factor", "The factor for packet intervals", 0.0f, 1.0f, 10.0f, () -> pulseConfig.getValue()));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Renders the serverside player position", true));
    //
    private FakePlayerEntity serverModel;
    //
    private boolean blinking;
    private final Queue<Packet<?>> packets = new LinkedBlockingQueue<>();

    /**
     *
     */
    public FakeLagModule()
    {
        super("FakeLag", "Withholds packets from the server, creating clientside lag", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable()
    {
        if (mc.player != null && renderConfig.getValue())
        {
            serverModel = new FakePlayerEntity(mc.player);
            serverModel.spawnPlayer();
            serverModel.setUUID(mc.player.getUUID());
        }
    }

    @Override
    public void onDisable()
    {
        if (mc.player == null)
        {
            return;
        }
        if (!packets.isEmpty())
        {
            for (Packet<?> p : packets)
            {
                Managers.NETWORK.sendPacket(p);
            }
            packets.clear();
        }
        if (serverModel != null)
        {
            serverModel.setUUID(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"));
            serverModel.despawnPlayer();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE && pulseConfig.getValue() && packets.size() > factorConfig.getValue() * 10.0f)
        {
            blinking = true;
            if (!packets.isEmpty())
            {
                for (Packet<?> p : packets)
                {
                    Managers.NETWORK.sendPacket(p);
                }
            }
            packets.clear();
            if (serverModel != null)
            {
                serverModel.copyPosition(mc.player);
                serverModel.setYHeadRot(mc.player.yHeadRot);
            }
            blinking = false;
        }
    }

    @EventListener
    public void onDisconnectEvent(DisconnectEvent event)
    {
        // packets.clear();
        disable();
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (mc.player == null || mc.player.isHandsBusy() || blinking)
        {
            return;
        }
        if (event.getPacket() instanceof ServerboundPlayerActionPacket || event.getPacket() instanceof ServerboundMovePlayerPacket
                || event.getPacket() instanceof ServerboundPlayerCommandPacket || event.getPacket() instanceof ServerboundSwingPacket
                || event.getPacket() instanceof ServerboundInteractPacket || event.getPacket() instanceof ServerboundUseItemOnPacket
                || event.getPacket() instanceof ServerboundUseItemPacket)
        {
            event.cancel();
            packets.add(event.getPacket());
        }
    }

    public enum LagMode
    {
        BLINK
    }
}
