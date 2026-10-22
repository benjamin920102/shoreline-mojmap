package net.shoreline.client.impl.module.misc;

import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketLoggerModule extends ToggleModule
{

    Config<Boolean> logConfig = register(new BooleanConfig("Log", "Adds packets to the logs", false));
    Config<Boolean> chatConfig = register(new BooleanConfig("LogChat", "Logs packets in the chats", false));
    Config<Boolean> disconnectConfig = register(new BooleanConfig("LogDisconnect", "Logs packets on client disconnect", true));
    Config<Boolean> moveFullConfig = register(new BooleanConfig("PlayerMoveFull", "Logs PlayerMoveC2SPacket", false));
    Config<Boolean> moveLookConfig = register(new BooleanConfig("PlayerMoveLook", "Logs PlayerMoveC2SPacket", false));
    Config<Boolean> movePosConfig = register(new BooleanConfig("PlayerMovePosition", "Logs PlayerMoveC2SPacket", false));
    Config<Boolean> moveGroundConfig = register(new BooleanConfig("PlayerMoveGround", "Logs PlayerMoveC2SPacket", false));
    Config<Boolean> vehicleMoveConfig = register(new BooleanConfig("VehicleMove", "Logs VehicleMoveC2SPacket", false));
    Config<Boolean> playerActionConfig = register(new BooleanConfig("PlayerAction", "Logs PlayerActionC2SPacket", false));
    Config<Boolean> updateSlotConfig = register(new BooleanConfig("UpdateSelectedSlot", "Logs UpdateSelectedSlotC2SPacket", false));
    Config<Boolean> clickSlotConfig = register(new BooleanConfig("ClickSlot", "Logs ClickSlotC2SPacket", false));
    Config<Boolean> pickInventoryConfig = register(new BooleanConfig("PickInventory", "Logs PickFromInventoryC2SPacket", false));
    Config<Boolean> handSwingConfig = register(new BooleanConfig("HandSwing", "Logs HandSwingC2SPacket", false));
    Config<Boolean> interactEntityConfig = register(new BooleanConfig("InteractEntity", "Logs PlayerInteractEntityC2SPacket", false));
    Config<Boolean> interactBlockConfig = register(new BooleanConfig("InteractBlock", "Logs PlayerInteractBlockC2SPacket", false));
    Config<Boolean> interactItemConfig = register(new BooleanConfig("InteractItem", "Logs PlayerInteractItemC2SPacket", false));
    Config<Boolean> commandConfig = register(new BooleanConfig("ClientCommand", "Logs ClientCommandC2SPacket", false));
    Config<Boolean> statusConfig = register(new BooleanConfig("ClientStatus", "Logs ClientStatusC2SPacket", false));
    Config<Boolean> closeScreenConfig = register(new BooleanConfig("CloseScreen", "Logs CloseHandledScreenC2SPacket", false));
    Config<Boolean> teleportConfirmConfig = register(new BooleanConfig("TeleportConfirm", "Logs TeleportConfirmC2SPacket", false));
    Config<Boolean> pongConfig = register(new BooleanConfig("Pong", "Logs CommonPongC2SPacket", false));

    private final List<PacketLog> packetLogs = new CopyOnWriteArrayList<>();

    public PacketLoggerModule()
    {
        super("PacketLogger", "Logs client packets", ModuleCategory.MISCELLANEOUS);
    }

    @Override
    public void onEnable()
    {
        Shoreline.info("PacketLogger enabled ...");
    }

    @Override
    public void onDisable()
    {
        Shoreline.info("PacketLogger disabled ...");
    }

    private void logPacket(Packet<?> packet, String msg, Object... args)
    {
        String s = String.format(msg, args);
        if (logConfig.getValue())
        {
            Shoreline.info(s);
        }
        if (chatConfig.getValue())
        {
            sendModuleMessage(s);
        }

        packetLogs.add(new PacketLog(packet, System.currentTimeMillis()));
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE)
        {
            packetLogs.removeIf(l -> System.currentTimeMillis() - l.time() > 3000);
        }
    }

    @EventListener
    public void onDisconnect(DisconnectEvent event)
    {
        if (!disconnectConfig.getValue())
        {
            return;
        }

        Map<ResourceLocation, Integer> packetCountMap = new HashMap<>();
        for (PacketLog packetLog : packetLogs)
        {
            Packet<?> packet = packetLog.packet();
            ResourceLocation identifier = packet.type().id();
            if (packetCountMap.containsKey(identifier))
            {
                packetCountMap.replace(identifier, packetCountMap.get(identifier) + 1);
            }
            else
            {
                packetCountMap.put(identifier, 1);
            }
        }

        List<String> strings = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : packetCountMap.entrySet())
        {
            ResourceLocation packet = entry.getKey();
            strings.add(packet.toShortLanguageKey() + ": " + entry.getValue());
        }

        Shoreline.info(String.join(",", strings));
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket.PosRot packet && moveFullConfig.getValue())
        {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove Full - ");
            if (packet.hasPosition())
            {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            if (packet.hasRotation())
            {
                builder.append("yaw: ").append(packet.getYRot(0.0f)).append(", pitch: ").append(packet.getXRot(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPacket.Pos packet && movePosConfig.getValue())
        {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove PosGround - ");
            if (packet.hasPosition())
            {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPacket.Rot packet && moveLookConfig.getValue())
        {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove LookGround - ");
            if (packet.hasRotation())
            {
                builder.append("yaw: ").append(packet.getYRot(0.0f)).append(", pitch: ").append(packet.getXRot(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPacket.StatusOnly packet && moveGroundConfig.getValue())
        {
            String s = "PlayerMove Ground - onground: " + packet.isOnGround();
            logPacket(packet, s);
        }
        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet && vehicleMoveConfig.getValue())
        {
            logPacket(packet, "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.getX(), packet.getY(), packet.getZ(), packet.getYRot(), packet.getXRot());
        }
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet && playerActionConfig.getValue())
        {
            logPacket(packet, "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString());
        }
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet && updateSlotConfig.getValue())
        {
            logPacket(packet, "UpdateSlot - slot: %d", packet.getSlot());
        }
        if (event.getPacket() instanceof ServerboundSwingPacket packet && handSwingConfig.getValue())
        {
            logPacket(packet, "HandSwing - hand: %s", packet.getHand().name());
        }
        if (event.getPacket() instanceof ServerboundPongPacket packet && pongConfig.getValue())
        {
            logPacket(packet, "Pong - %d", packet.getId());
        }
        if (event.getPacket() instanceof ServerboundInteractPacket packet && mc.level != null && interactEntityConfig.getValue())
        {
            logPacket(packet, "InteractEntity");
        }
        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet && interactBlockConfig.getValue())
        {
            BlockHitResult blockHitResult = packet.getHitResult();
            logPacket(packet, "InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getDirection().name(), packet.getHand().name());
        }
        if (event.getPacket() instanceof ServerboundUseItemPacket packet && interactItemConfig.getValue())
        {
            logPacket(packet, "InteractItem - hand: %s", packet.getHand().name());
        }
        if (event.getPacket() instanceof ServerboundContainerClosePacket packet && closeScreenConfig.getValue())
        {
            logPacket(packet, "CloseScreen - id: %s", packet.getContainerId());
        }
        if (event.getPacket() instanceof ServerboundPlayerCommandPacket packet && commandConfig.getValue())
        {
            logPacket(packet, "ClientCommand - mode: %s", packet.getAction().name());
        }
        if (event.getPacket() instanceof ServerboundClientCommandPacket packet && statusConfig.getValue())
        {
            logPacket(packet, "ClientStatus - mode: %s", packet.getAction().name());
        }
        if (event.getPacket() instanceof ServerboundContainerClickPacket packet && clickSlotConfig.getValue())
        {
            logPacket(packet, "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.getClickType().name(), packet.getSlotNum(), packet.getButtonNum(), packet.getContainerId());
        }
        if (event.getPacket() instanceof ServerboundPickItemPacket packet && pickInventoryConfig.getValue())
        {
            logPacket(packet, "PickInventory - slot: %s", packet.getSlot());
        }
        if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket packet && teleportConfirmConfig.getValue())
        {
            logPacket(packet, "TeleportConfirm - id: %s", packet.getId());
        }
    }

    public record PacketLog(Packet<?> packet, long time) {}
}
