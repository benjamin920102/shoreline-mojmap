package net.shoreline.client.impl.module.render;

import net.minecraft.network.chat.Component;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.gui.hud.PlayerListColumnsEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListIconEvent;
import net.shoreline.client.impl.event.gui.hud.PlayerListNameEvent;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class ExtraTabModule extends ToggleModule {
   Config<Integer> sizeConfig = this.register(new NumberConfig<>("Size", "The number of players to show", 80, 200, 1000));
   Config<Integer> columnsConfig = this.register(new NumberConfig<>("Columns", "The number columns to show.", 1, 20, 100));
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Highlights yourself in the tab list.", false));
   Config<Boolean> friendsConfig = this.register(new BooleanConfig("Friends", "Highlights friends in the tab list.", true));

   public ExtraTabModule() {
      super("ExtraTab", "Expands the tab list size to allow for more players", ModuleCategory.RENDER);
   }

   @EventListener
   public void onPlayerListName(PlayerListNameEvent event) {
      String[] names = event.getPlayerName().getString().split(" ");
      if (this.selfConfig.getValue()) {
         for (String s : names) {
            String name1 = this.stripControlCodes(s);
            if (name1.equals(mc.getGameProfile().getName())) {
               event.cancel();
               event.setPlayerName(Component.nullToEmpty("§s" + event.getPlayerName().getString()));
               return;
            }
         }
      }

      if (this.friendsConfig.getValue() && SocialsModule.getInstance().isFriendsEnabled()) {
         for (String sx : names) {
            String name1 = this.stripControlCodes(sx);
            if (Managers.SOCIAL.isFriend(name1)) {
               event.cancel();
               event.setPlayerName(Component.nullToEmpty("§g" + event.getPlayerName().getString()));
               break;
            }
         }
      }
   }

   @EventListener
   public void onPlayerListIcon(PlayerListIconEvent.Width event) {
      String[] names = event.getText().split(" ");

      for (String name : names) {
         String var7 = this.stripControlCodes(name);
      }
   }

   @EventListener
   public void onPlayerListIconRender(PlayerListIconEvent.Render event) {
      String[] names = event.getPlayerNameText().getString().split(" ");

      for (String name : names) {
         String var7 = this.stripControlCodes(name);
      }
   }

   @EventListener
   public void onPlayerList(PlayerListEvent event) {
      event.cancel();
      event.setSize(this.sizeConfig.getValue());
   }

   @EventListener
   public void onPlayerListColumns(PlayerListColumnsEvent event) {
      event.cancel();
      event.setTabHeight(this.columnsConfig.getValue());
   }

   private String stripControlCodes(String string) {
      StringBuilder builder = new StringBuilder();
      boolean skip = false;

      for (char c : string.toCharArray()) {
         if (c == 167) {
            skip = true;
         } else if (skip) {
            skip = false;
         } else {
            builder.append(c);
         }
      }

      return builder.toString();
   }
}
