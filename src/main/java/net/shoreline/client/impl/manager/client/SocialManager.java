package net.shoreline.client.impl.manager.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.shoreline.client.api.social.SocialRelation;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.chat.ChatUtil;

public class SocialManager implements Globals {
   private final ConcurrentMap<String, SocialRelation> relationships = new ConcurrentHashMap<>();

   public boolean isRelation(String name, SocialRelation relation) {
      return this.relationships.get(name) == relation;
   }

   public boolean isFriend(String name) {
      return !SocialsModule.getInstance().isFriendsEnabled() ? false : this.isRelation(name, SocialRelation.FRIEND);
   }

   public boolean isFriendInternal(String name) {
      return this.isRelation(name, SocialRelation.FRIEND);
   }

   public boolean isFriend(Component name) {
      return !SocialsModule.getInstance().isFriendsEnabled() ? false : name != null && this.isRelation(name.getString(), SocialRelation.FRIEND);
   }

   public void addRelation(String name, SocialRelation relation) {
      if (mc.player == null || !name.equals(mc.player.getDisplayName().getString())) {
         SocialRelation relationship = this.relationships.get(name);
         if (relationship != null) {
            this.relationships.replace(name, relation);
         } else {
            this.relationships.put(name, relation);
         }
      }
   }

   public void addFriend(String name) {
      this.addRelation(name, SocialRelation.FRIEND);
      if (SocialsModule.getInstance().shouldNotify() && mc.level != null) {
         for (PlayerInfo playerListEntry : mc.player.connection.getOnlinePlayers()) {
            String playerName = playerListEntry.getProfile().getName();
            String[] names = playerName.split(" ");

            for (String name1 : names) {
               if (name1.equals(name)) {
                  ChatUtil.serverSendCommand("msg " + name + " I just added you as a friend on Shoreline!");
                  break;
               }
            }
         }
      }
   }

   public void addFriend(Component name) {
      this.addFriend(name.getString());
   }

   public SocialRelation remove(String playerName) {
      return this.relationships.remove(playerName);
   }

   public SocialRelation remove(Component playerName) {
      return this.relationships.remove(playerName.getString());
   }

   public Collection<String> getRelations(SocialRelation relation) {
      List<String> friends = new ArrayList<>();

      for (Entry<String, SocialRelation> relationship : this.relationships.entrySet()) {
         if (relationship.getValue() == relation) {
            friends.add(relationship.getKey());
         }
      }

      return friends;
   }

   public Collection<String> getFriends() {
      return this.getRelations(SocialRelation.FRIEND);
   }
}
