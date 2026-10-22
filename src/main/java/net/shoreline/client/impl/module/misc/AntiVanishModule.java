package net.shoreline.client.impl.module.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.gui.hud.ChatMessageEvent;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AntiVanishModule extends ToggleModule {
   private final Timer vanishTimer = new CacheTimer();
   private final Set<String> messageCache = new HashSet<>();
   private Map<UUID, String> playerCache = new HashMap<>();

   public AntiVanishModule() {
      super("AntiVanish", "Notifies user when a player uses /vanish", ModuleCategory.MISCELLANEOUS);
   }

   @Override
   public void onEnable() {
      this.messageCache.clear();
   }

   @EventListener
   public void onChatMessage(ChatMessageEvent event) {
      String message = event.getText().getString();
      if (message.contains("left")) {
         this.messageCache.add(message);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.vanishTimer.passed(1000)) {
            Map<UUID, String> players = this.playerCache;
            this.playerCache = mc.getConnection()
               .getOnlinePlayers()
               .stream()
               .collect(Collectors.toMap(e -> e.getProfile().getId(), e -> e.getProfile().getName()));

            for (UUID uuid : players.keySet()) {
               if (!this.playerCache.containsKey(uuid)) {
                  String name = players.get(uuid);
                  if (this.messageCache.stream().noneMatch(s -> s.contains(name))) {
                     this.sendModuleMessage("%s used /vanish!", new Object[]{name});
                  }
               }
            }

            this.messageCache.clear();
            this.vanishTimer.reset();
         }
      }
   }
}
