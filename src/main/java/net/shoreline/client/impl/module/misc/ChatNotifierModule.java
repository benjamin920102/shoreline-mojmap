package net.shoreline.client.impl.module.misc;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.eventbus.annotation.EventListener;

public final class ChatNotifierModule extends ToggleModule {
   Config<Boolean> totemPopConfig = this.register(new BooleanConfig("TotemPop", "Notifies in chat when a player pops a totem", true));
   Config<Boolean> visualRangeConfig = this.register(new BooleanConfig("VisualRange", "Notifies in chat when player enters visual range", false));
   Config<Boolean> joinConfig = this.register(new BooleanConfig("Join", "Notifies in chat when a player joins", false));
   Config<Boolean> leaveConfig = this.register(new BooleanConfig("Leave", "Notifies in chat when a player leaves", false));
   Config<Boolean> friendsConfig = this.register(new BooleanConfig("Friends", "Notifies for friends", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Notifies you if the server you join is running Grim", false));

   public ChatNotifierModule() {
      super("ChatNotifier", "Notifies in chat", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35 && this.totemPopConfig.getValue()) {
            Entity entity = packet.getEntity(mc.level);
            if (!(entity instanceof LivingEntity)) {
               return;
            }

            int totems = Managers.TOTEM.getTotems(entity);
            String playerName = entity.getName().getString();
            boolean isFriend = Managers.SOCIAL.isFriend(playerName);
            if (isFriend && !this.friendsConfig.getValue() || entity == mc.player) {
               return;
            }

            ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + playerName + "§f popped §s" + totems + "§f totems", entity.hashCode());
         }

         if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet && this.joinConfig.getValue()) {
            if (!packet.actions().contains(Action.ADD_PLAYER) || mc.player.tickCount < 20) {
               return;
            }

            packet.entries()
               .stream()
               .filter(data -> data != null && data.profile() != null)
               .filter(data -> data.profile().getName() != null && !data.profile().getName().isEmpty() || data.profile().getId() != null)
               .forEach(data -> {
                  String namex = data.profile().getName();
                  if (namex != null) {
                     boolean isFriendx = Managers.SOCIAL.isFriend(namex);
                     if (!this.friendsConfig.getValue() && isFriendx) {
                        return;
                     }

                     ChatUtil.clientSendMessage((isFriendx ? "§g" : "§7") + namex + "§f has joined the server", namex.hashCode());
                  }
               });
         }

         if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket packet && this.leaveConfig.getValue()) {
            if (mc.getConnection() == null) {
               return;
            }

            for (UUID uuid : packet.profileIds()) {
               String name = null;

               for (PlayerInfo info : new ArrayList<>(mc.getConnection().getOnlinePlayers())) {
                  if (info != null) {
                     GameProfile gameProfile = info.getProfile();
                     if (gameProfile.getId().equals(uuid)) {
                        name = gameProfile.getName();
                     }
                  }
               }

               if (name != null) {
                  boolean isFriend = Managers.SOCIAL.isFriend(name);
                  if (!this.friendsConfig.getValue() && isFriend) {
                     return;
                  }

                  ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + name + "§f has left the server", name.hashCode());
               }
            }
         }
      }
   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      if (this.visualRangeConfig.getValue() && event.getEntity() instanceof Player playerEntity && !(event.getEntity() instanceof FakePlayerEntity)) {
         String playerName = event.getEntity().getName().getString();
         boolean isFriend = Managers.SOCIAL.isFriend(playerName);
         if ((!isFriend || this.friendsConfig.getValue()) && event.getEntity() != mc.player && !playerName.equalsIgnoreCase("ShaderPlayer")) {
            mc.executeIfPossible(
               () -> ChatUtil.clientSendMessageRaw(
                  "§s[VisualRange] " + (isFriend ? "§g" : "§7") + playerName + "§f entered your visual range", playerEntity.hashCode()
               )
            );
         }
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.visualRangeConfig.getValue() && event.getEntity() instanceof Player playerEntity && !(event.getEntity() instanceof FakePlayerEntity)) {
         String playerName = event.getEntity().getName().getString();
         boolean isFriend = Managers.SOCIAL.isFriend(playerName);
         if ((!isFriend || this.friendsConfig.getValue()) && event.getEntity() != mc.player && !playerName.equalsIgnoreCase("ShaderPlayer")) {
            mc.executeIfPossible(
               () -> ChatUtil.clientSendMessageRaw(
                  "§s[VisualRange] " + (isFriend ? "§g" : "§c") + playerName + "§f left your visual range", playerEntity.hashCode()
               )
            );
         }
      }
   }

   @EventListener
   public void onEntityDeath(EntityDeathEvent event) {
      if (this.totemPopConfig.getValue()) {
         int totems = Managers.TOTEM.getTotems(event.getEntity());
         if (totems != 0) {
            String playerName = event.getEntity().getName().getString();
            boolean isFriend = Managers.SOCIAL.isFriend(playerName);
            if ((!isFriend || this.friendsConfig.getValue()) && event.getEntity() != mc.player) {
               ChatUtil.clientSendMessage(
                  (isFriend ? "§g" : "§7") + playerName + "§f died after popping §s" + totems + "§f totems", event.getEntity().hashCode()
               );
            }
         }
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if (this.grimConfig.getValue() && !mc.isLocalServer()) {
         if (Managers.ANTICHEAT.isGrim()) {
            ChatUtil.clientSendMessage("This server is running GrimAC", 102);
         } else {
            ChatUtil.clientSendMessage("This server is not running GrimAC", 102);
         }
      }
   }
}
