package net.shoreline.client.impl.module.misc;

import java.time.Instant;
import java.util.BitSet;
import net.minecraft.network.chat.LastSeenMessages.Update;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.util.Crypt.SaltSupplier;
import net.shoreline.client.Shoreline;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.gui.screen.MenuDisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public final class ServerModule extends ConcurrentModule {
   Config<Boolean> demoConfig = this.register(new BooleanConfig("NoDemo", "Prevents servers from forcing you to a demo screen", true));
   Config<Boolean> resourcePackConfig = this.register(new BooleanConfig("NoResourcePack", "Prevents server from forcing resource pack", false));
   Config<Boolean> antiCrashConfig = this.register(new BooleanConfig("NoServerCrash", "Prevents server packets from crashing the client", false));
   Config<Boolean> illegalDisconnectConfig = this.register(new BooleanConfig("IllegalDisconnect", "Disconnects by getting kicked from server", false));

   public ServerModule() {
      super("Server", "Prevents servers actions on player", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundGameEventPacket packet
         && packet.getEvent() == ClientboundGameEventPacket.DEMO_EVENT
         && !mc.isDemo()
         && this.demoConfig.getValue()) {
         Shoreline.info("Server attempted to use Demo mode features on you!");
         event.cancel();
      }

      if (event.getPacket() instanceof ClientboundResourcePackPushPacket && this.resourcePackConfig.getValue()) {
         event.cancel();
         Managers.NETWORK.sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), Action.DECLINED));
      }

      if (mc.level != null) {
         if (this.antiCrashConfig.getValue()) {
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet
               && (
                  packet.getX() > 3.0E7
                     || packet.getY() > mc.level.getMaxBuildHeight()
                     || packet.getZ() > 3.0E7
                     || packet.getX() < -3.0E7
                     || packet.getY() < mc.level.getMinBuildHeight()
                     || packet.getZ() < -3.0E7
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof ClientboundExplodePacket packet
               && (
                  packet.getX() > 3.0E7
                     || packet.getY() > mc.level.getMaxBuildHeight()
                     || packet.getZ() > 3.0E7
                     || packet.getX() < -3.0E7
                     || packet.getY() < mc.level.getMinBuildHeight()
                     || packet.getZ() < -3.0E7
                     || packet.getPower() > 1000.0F
                     || packet.getToBlow().size() > 1000
                     || packet.getKnockbackX() > 1000.0F
                     || packet.getKnockbackY() > 1000.0F
                     || packet.getKnockbackZ() > 1000.0F
                     || packet.getKnockbackX() < -1000.0F
                     || packet.getKnockbackY() < -1000.0F
                     || packet.getKnockbackZ() < -1000.0F
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
               && (
                  packet.getXa() > 1000.0
                     || packet.getYa() > 1000.0
                     || packet.getZa() > 1000.0
                     || packet.getXa() < -1000.0
                     || packet.getYa() < -1000.0
                     || packet.getZa() < -1000.0
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof ClientboundLevelParticlesPacket packet && packet.getCount() > 500) {
               event.cancel();
            }
         }
      }
   }

   @EventListener
   public void onMenuDisconnect(MenuDisconnectEvent event) {
      if (this.illegalDisconnectConfig.getValue()) {
         Managers.NETWORK.sendPacket(new ServerboundChatPacket("§", Instant.now(), SaltSupplier.getLong(), null, new Update(1, new BitSet(2))));
      }
   }
}
