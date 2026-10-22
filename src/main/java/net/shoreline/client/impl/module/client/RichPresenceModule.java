package net.shoreline.client.impl.module.client;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.FinishLoadingEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class RichPresenceModule extends ToggleModule {
   private static final long APPLICATION_ID = 1388334762602528818L;
   private final RichPresence presence = new RichPresence();
   Config<Boolean> SHOW_SERVER = this.register(new BooleanConfig("Show Server", "Shows the server IP in RPC", false));
   Config<Integer> UPDATE_DELAY = this.register(new NumberConfig<>("Delay", "Delay between RPC updates", 20, 60, 100));
   private int ticks = 0;

   public RichPresenceModule() {
      super("RPC", "Discord Rich Presence integration", ModuleCategory.CLIENT);
   }

   @Override
   public void onEnable() {
      DiscordIPC.start(1388334762602528818L, null);
      this.presence.setStart(System.currentTimeMillis() / 1000L);
      this.presence.setLargeImage("beta", "(beta-fa4cb0f)");
   }

   @Override
   public void onDisable() {
      DiscordIPC.stop();
   }

   @EventListener
   public void onGameFinishedInit(FinishLoadingEvent event) {
      if (this.isEnabled()) {
         this.onEnable();
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.ticks-- <= 0) {
         this.updateDetails();
         DiscordIPC.setActivity(this.presence);
         this.ticks = this.UPDATE_DELAY.getValue();
      }
   }

   private void updateDetails() {
      if (mc == null) {
         this.presence.setDetails("Starting game...");
         this.presence.setState("(beta-fa4cb0f) b25");
      } else if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof JoinMultiplayerScreen) && !(mc.screen instanceof SelectWorldScreen)) {
         if (mc.isLocalServer()) {
            this.presence.setDetails("Playing Singleplayer");
            this.presence.setState("(beta-fa4cb0f) b25");
         } else if (mc.getCurrentServer() != null) {
            this.presence.setDetails(this.SHOW_SERVER.getValue() ? "Playing " + mc.getCurrentServer().ip : "Playing Multiplayer");
            this.presence.setState("(beta-fa4cb0f) b25");
         } else {
            this.presence.setDetails("Idling");
         }
      } else {
         this.presence.setDetails("In Menus");
         this.presence.setState("(beta-fa4cb0f) b25");
      }
   }
}
