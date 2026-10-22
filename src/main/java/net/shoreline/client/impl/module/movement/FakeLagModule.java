package net.shoreline.client.impl.module.movement;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
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

public class FakeLagModule extends ToggleModule {
   Config<FakeLagModule.LagMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for caching packets", FakeLagModule.LagMode.BLINK, FakeLagModule.LagMode.values())
   );
   Config<Boolean> pulseConfig = this.register(new BooleanConfig("Pulse", "Releases packets at intervals", false));
   Config<Float> factorConfig = this.register(
      new NumberConfig<>("Factor", "The factor for packet intervals", 0.0F, 1.0F, 10.0F, () -> this.pulseConfig.getValue())
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders the serverside player position", true));
   private FakePlayerEntity serverModel;
   private boolean blinking;
   private final Queue<Packet<?>> packets = new LinkedBlockingQueue<>();

   public FakeLagModule() {
      super("FakeLag", "Withholds packets from the server, creating clientside lag", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && this.renderConfig.getValue()) {
         this.serverModel = new FakePlayerEntity(mc.player);
         this.serverModel.spawnPlayer();
         this.serverModel.setUUID(mc.player.getUUID());
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (!this.packets.isEmpty()) {
            for (Packet<?> p : this.packets) {
               Managers.NETWORK.sendPacket(p);
            }

            this.packets.clear();
         }

         if (this.serverModel != null) {
            this.serverModel.setUUID(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"));
            this.serverModel.despawnPlayer();
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.pulseConfig.getValue() && this.packets.size() > this.factorConfig.getValue() * 10.0F) {
         this.blinking = true;
         if (!this.packets.isEmpty()) {
            for (Packet<?> p : this.packets) {
               Managers.NETWORK.sendPacket(p);
            }
         }

         this.packets.clear();
         if (this.serverModel != null) {
            this.serverModel.copyPosition(mc.player);
            this.serverModel.setYHeadRot(mc.player.yHeadRot);
         }

         this.blinking = false;
      }
   }

   @EventListener
   public void onDisconnectEvent(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && !mc.player.isHandsBusy() && !this.blinking) {
         if (event.getPacket() instanceof ServerboundPlayerActionPacket
            || event.getPacket() instanceof ServerboundMovePlayerPacket
            || event.getPacket() instanceof ServerboundPlayerCommandPacket
            || event.getPacket() instanceof ServerboundSwingPacket
            || event.getPacket() instanceof ServerboundInteractPacket
            || event.getPacket() instanceof ServerboundUseItemOnPacket
            || event.getPacket() instanceof ServerboundUseItemPacket) {
            event.cancel();
            this.packets.add(event.getPacket());
         }
      }
   }

   public static enum LagMode {
      BLINK;
   }
}
