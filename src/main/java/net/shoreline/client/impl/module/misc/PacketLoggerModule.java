package net.shoreline.client.impl.module.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
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

public class PacketLoggerModule extends ToggleModule {
   Config<Boolean> logConfig = this.register(new BooleanConfig("Log", "Adds packets to the logs", false));
   Config<Boolean> chatConfig = this.register(new BooleanConfig("LogChat", "Logs packets in the chats", false));
   Config<Boolean> disconnectConfig = this.register(new BooleanConfig("LogDisconnect", "Logs packets on client disconnect", true));
   Config<Boolean> moveFullConfig = this.register(new BooleanConfig("PlayerMoveFull", "Logs PlayerMoveC2SPacket", false));
   Config<Boolean> moveLookConfig = this.register(new BooleanConfig("PlayerMoveLook", "Logs PlayerMoveC2SPacket", false));
   Config<Boolean> movePosConfig = this.register(new BooleanConfig("PlayerMovePosition", "Logs PlayerMoveC2SPacket", false));
   Config<Boolean> moveGroundConfig = this.register(new BooleanConfig("PlayerMoveGround", "Logs PlayerMoveC2SPacket", false));
   Config<Boolean> vehicleMoveConfig = this.register(new BooleanConfig("VehicleMove", "Logs VehicleMoveC2SPacket", false));
   Config<Boolean> playerActionConfig = this.register(new BooleanConfig("PlayerAction", "Logs PlayerActionC2SPacket", false));
   Config<Boolean> updateSlotConfig = this.register(new BooleanConfig("UpdateSelectedSlot", "Logs UpdateSelectedSlotC2SPacket", false));
   Config<Boolean> clickSlotConfig = this.register(new BooleanConfig("ClickSlot", "Logs ClickSlotC2SPacket", false));
   Config<Boolean> pickInventoryConfig = this.register(new BooleanConfig("PickInventory", "Logs PickFromInventoryC2SPacket", false));
   Config<Boolean> handSwingConfig = this.register(new BooleanConfig("HandSwing", "Logs HandSwingC2SPacket", false));
   Config<Boolean> interactEntityConfig = this.register(new BooleanConfig("InteractEntity", "Logs PlayerInteractEntityC2SPacket", false));
   Config<Boolean> interactBlockConfig = this.register(new BooleanConfig("InteractBlock", "Logs PlayerInteractBlockC2SPacket", false));
   Config<Boolean> interactItemConfig = this.register(new BooleanConfig("InteractItem", "Logs PlayerInteractItemC2SPacket", false));
   Config<Boolean> commandConfig = this.register(new BooleanConfig("ClientCommand", "Logs ClientCommandC2SPacket", false));
   Config<Boolean> statusConfig = this.register(new BooleanConfig("ClientStatus", "Logs ClientStatusC2SPacket", false));
   Config<Boolean> closeScreenConfig = this.register(new BooleanConfig("CloseScreen", "Logs CloseHandledScreenC2SPacket", false));
   Config<Boolean> teleportConfirmConfig = this.register(new BooleanConfig("TeleportConfirm", "Logs TeleportConfirmC2SPacket", false));
   Config<Boolean> pongConfig = this.register(new BooleanConfig("Pong", "Logs CommonPongC2SPacket", false));
   private final List<PacketLoggerModule.PacketLog> packetLogs = new CopyOnWriteArrayList<>();

   public PacketLoggerModule() {
      super("PacketLogger", "Logs client packets", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      Shoreline.info("PacketLogger enabled ...");
   }

   @Override
   public void onDisable() {
      Shoreline.info("PacketLogger disabled ...");
   }

   private void logPacket(Packet<?> packet, String msg, Object... args) {
      String s = String.format(msg, args);
      if (this.logConfig.getValue()) {
         Shoreline.info(s);
      }

      if (this.chatConfig.getValue()) {
         this.sendModuleMessage(s);
      }

      this.packetLogs.add(new PacketLoggerModule.PacketLog(packet, System.currentTimeMillis()));
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.packetLogs.removeIf(l -> System.currentTimeMillis() - l.time() > 3000L);
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      if (this.disconnectConfig.getValue()) {
         Map<ResourceLocation, Integer> packetCountMap = new HashMap<>();

         for (PacketLoggerModule.PacketLog packetLog : this.packetLogs) {
            Packet<?> packet = packetLog.packet();
            ResourceLocation identifier = packet.type().id();
            if (packetCountMap.containsKey(identifier)) {
               packetCountMap.replace(identifier, packetCountMap.get(identifier) + 1);
            } else {
               packetCountMap.put(identifier, 1);
            }
         }

         List<String> strings = new ArrayList<>();

         for (Entry<ResourceLocation, Integer> entry : packetCountMap.entrySet()) {
            ResourceLocation packet = entry.getKey();
            strings.add(packet.toShortLanguageKey() + ": " + entry.getValue());
         }

         Shoreline.info(String.join(",", strings));
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof PosRot packet && this.moveFullConfig.getValue()) {
         StringBuilder builder = new StringBuilder();
         builder.append("PlayerMove Full - ");
         if (packet.hasPosition()) {
            builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         if (packet.hasRotation()) {
            builder.append("yaw: ").append(packet.getYRot(0.0F)).append(", pitch: ").append(packet.getXRot(0.0F)).append(" ");
         }

         builder.append(" onground: ").append(packet.isOnGround());
         this.logPacket(packet, builder.toString());
      }

      if (event.getPacket() instanceof Pos packet && this.movePosConfig.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove PosGround - ");
         if (packet.hasPosition()) {
            builderx.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(packet, builderx.toString());
      }

      if (event.getPacket() instanceof Rot packet && this.moveLookConfig.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove LookGround - ");
         if (packet.hasRotation()) {
            builderx.append("yaw: ").append(packet.getYRot(0.0F)).append(", pitch: ").append(packet.getXRot(0.0F)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(packet, builderx.toString());
      }

      if (event.getPacket() instanceof StatusOnly packet && this.moveGroundConfig.getValue()) {
         String s = "PlayerMove Ground - onground: " + packet.isOnGround();
         this.logPacket(packet, s);
      }

      if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet && this.vehicleMoveConfig.getValue()) {
         this.logPacket(
            packet, "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.getX(), packet.getY(), packet.getZ(), packet.getYRot(), packet.getXRot()
         );
      }

      if (event.getPacket() instanceof ServerboundPlayerActionPacket packet && this.playerActionConfig.getValue()) {
         this.logPacket(
            packet,
            "PlayerAction - action: %s, direction: %s, pos: %s",
            packet.getAction().name(),
            packet.getDirection().name(),
            packet.getPos().toShortString()
         );
      }

      if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet && this.updateSlotConfig.getValue()) {
         this.logPacket(packet, "UpdateSlot - slot: %d", packet.getSlot());
      }

      if (event.getPacket() instanceof ServerboundSwingPacket packet && this.handSwingConfig.getValue()) {
         this.logPacket(packet, "HandSwing - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof ServerboundPongPacket packet && this.pongConfig.getValue()) {
         this.logPacket(packet, "Pong - %d", packet.getId());
      }

      if (event.getPacket() instanceof ServerboundInteractPacket packet && mc.level != null && this.interactEntityConfig.getValue()) {
         this.logPacket(packet, "InteractEntity");
      }

      if (event.getPacket() instanceof ServerboundUseItemOnPacket packet && this.interactBlockConfig.getValue()) {
         BlockHitResult blockHitResult = packet.getHitResult();
         this.logPacket(
            packet,
            "InteractBlock - pos: %s, dir: %s, hand: %s",
            blockHitResult.getBlockPos().toShortString(),
            blockHitResult.getDirection().name(),
            packet.getHand().name()
         );
      }

      if (event.getPacket() instanceof ServerboundUseItemPacket packet && this.interactItemConfig.getValue()) {
         this.logPacket(packet, "InteractItem - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof ServerboundContainerClosePacket packet && this.closeScreenConfig.getValue()) {
         this.logPacket(packet, "CloseScreen - id: %s", packet.getContainerId());
      }

      if (event.getPacket() instanceof ServerboundPlayerCommandPacket packet && this.commandConfig.getValue()) {
         this.logPacket(packet, "ClientCommand - mode: %s", packet.getAction().name());
      }

      if (event.getPacket() instanceof ServerboundClientCommandPacket packet && this.statusConfig.getValue()) {
         this.logPacket(packet, "ClientStatus - mode: %s", packet.getAction().name());
      }

      if (event.getPacket() instanceof ServerboundContainerClickPacket packet && this.clickSlotConfig.getValue()) {
         this.logPacket(
            packet,
            "ClickSlot - type: %s, slot: %s, button: %s, id: %s",
            packet.getClickType().name(),
            packet.getSlotNum(),
            packet.getButtonNum(),
            packet.getContainerId()
         );
      }

      if (event.getPacket() instanceof ServerboundPickItemPacket packet && this.pickInventoryConfig.getValue()) {
         this.logPacket(packet, "PickInventory - slot: %s", packet.getSlot());
      }

      if (event.getPacket() instanceof ServerboundAcceptTeleportationPacket packet && this.teleportConfirmConfig.getValue()) {
         this.logPacket(packet, "TeleportConfirm - id: %s", packet.getId());
      }
   }

   public record PacketLog(Packet<?> packet, long time) {
   }
}
