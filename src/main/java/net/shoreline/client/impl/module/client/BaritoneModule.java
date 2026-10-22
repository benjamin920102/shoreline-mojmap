package net.shoreline.client.impl.module.client;

import baritone.api.BaritoneAPI;
import java.awt.Color;
import net.minecraft.network.chat.Component;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.gui.hud.ChatMessageEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.util.FormattingUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class BaritoneModule extends ConcurrentModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Baritone block reach distance", 0.0F, 4.0F, 5.0F));
   Config<Boolean> placeConfig = this.register(new BooleanConfig("Place", "Allow baritone to place blocks", true));
   Config<Boolean> breakConfig = this.register(new BooleanConfig("Break", "Allow baritone to break blocks", true));
   Config<Boolean> sprintConfig = this.register(new BooleanConfig("Sprint", "Allow baritone to sprint", true));
   Config<Boolean> inventoryConfig = this.register(new BooleanConfig("UseInventory", "Allow baritone to use player inventory", false));
   Config<Boolean> vinesConfig = this.register(new BooleanConfig("Vines", "Allow baritone to climb vines", true));
   Config<Boolean> jump256Config = this.register(new BooleanConfig("JumpAt256", "Allow baritone to jump at 256 blocks", false));
   Config<Boolean> waterBucketFallConfig = this.register(new BooleanConfig("WaterBucketFall", "Allow baritone to use waterbuckets when falling", false));
   Config<Boolean> parkourConfig = this.register(new BooleanConfig("Parkour", "Allow baritone to jump between blocks", true));
   Config<Boolean> parkourPlaceConfig = this.register(new BooleanConfig("ParkourPlace", "Allow baritone to jump and place blocks", false));
   Config<Boolean> parkourAscendConfig = this.register(new BooleanConfig("ParkourAscend", "Allow baritone to jump up blocks", true));
   Config<Boolean> diagonalAscendConfig = this.register(new BooleanConfig("DiagonalAscend", "Allow baritone to jump up blocks diagonally", false));
   Config<Boolean> diagonalDescendConfig = this.register(new BooleanConfig("DiagonalDescend", "Allow baritone to move down blocks diagonally", false));
   Config<Boolean> mineDownConfig = this.register(new BooleanConfig("MineDownward", "Allow baritone to mine down", true));
   Config<Boolean> legitMineConfig = this.register(new BooleanConfig("LegitMine", "Uses baritone legit mine", false));
   Config<Boolean> logOnArrivalConfig = this.register(new BooleanConfig("LogOnArrival", "Logout when you arrive at destination", false));
   Config<Boolean> freeLookConfig = this.register(new BooleanConfig("FreeLook", "Allows you to look around freely while using baritone", true));
   Config<Boolean> antiCheatConfig = this.register(new BooleanConfig("AntiCheat", "Uses NCP placements and breaks", false));
   Config<Boolean> strictLiquidConfig = this.register(new BooleanConfig("Strict-Liquid", "Uses strick liquid checks", false));
   Config<Boolean> censorCoordsConfig = this.register(new BooleanConfig("CensorCoords", "Censors goal coordinates in chat", false));
   Config<Boolean> censorCommandsConfig = this.register(new BooleanConfig("CensorCommands", "Censors baritone commands in chat", false));
   Config<Boolean> chatControlConfig = this.register(new BooleanConfig("ChatControl", "Allows you to type baritone commands in chat without prefix", true));
   Config<Boolean> debugConfig = this.register(new BooleanConfig("Debug", "Debugs in the chat", false));
   Config<Color> goalColor = this.register(new ColorConfig("GoalColor", "The color of the goal box", Color.GREEN, false, false));
   Config<Color> pathColor = this.register(new ColorConfig("CurrentPathColor", "The color of the path", Color.RED, false, false));
   Config<Color> nextPathColor = this.register(new ColorConfig("NextPathColor", "The color of the path", Color.MAGENTA, false, false));

   public BaritoneModule() {
      super("Baritone", "Configure baritone", ModuleCategory.CLIENT);
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig().getContainer() != this) {
         BaritoneAPI.getSettings().blockReachDistance.value = this.rangeConfig.getValue();
         BaritoneAPI.getSettings().allowPlace.value = this.placeConfig.getValue();
         BaritoneAPI.getSettings().allowBreak.value = this.breakConfig.getValue();
         BaritoneAPI.getSettings().allowSprint.value = this.sprintConfig.getValue();
         BaritoneAPI.getSettings().allowInventory.value = this.inventoryConfig.getValue();
         BaritoneAPI.getSettings().allowVines.value = this.vinesConfig.getValue();
         BaritoneAPI.getSettings().allowJumpAt256.value = this.jump256Config.getValue();
         BaritoneAPI.getSettings().allowWaterBucketFall.value = this.waterBucketFallConfig.getValue();
         BaritoneAPI.getSettings().allowParkour.value = this.parkourConfig.getValue();
         BaritoneAPI.getSettings().allowParkourAscend.value = this.parkourAscendConfig.getValue();
         BaritoneAPI.getSettings().allowParkourPlace.value = this.parkourPlaceConfig.getValue();
         BaritoneAPI.getSettings().allowDiagonalAscend.value = this.diagonalAscendConfig.getValue();
         BaritoneAPI.getSettings().allowDiagonalDescend.value = this.diagonalDescendConfig.getValue();
         BaritoneAPI.getSettings().allowDownward.value = this.mineDownConfig.getValue();
         BaritoneAPI.getSettings().legitMine.value = this.legitMineConfig.getValue();
         BaritoneAPI.getSettings().disconnectOnArrival.value = this.logOnArrivalConfig.getValue();
         BaritoneAPI.getSettings().freeLook.value = this.freeLookConfig.getValue();
         BaritoneAPI.getSettings().antiCheatCompatibility.value = this.antiCheatConfig.getValue();
         BaritoneAPI.getSettings().strictLiquidCheck.value = this.strictLiquidConfig.getValue();
         BaritoneAPI.getSettings().censorCoordinates.value = this.censorCoordsConfig.getValue();
         BaritoneAPI.getSettings().censorRanCommands.value = this.censorCommandsConfig.getValue();
         BaritoneAPI.getSettings().chatControl.value = this.chatControlConfig.getValue();
         BaritoneAPI.getSettings().chatDebug.value = this.debugConfig.getValue();
         BaritoneAPI.getSettings().colorGoalBox.value = this.goalColor.getValue();
         BaritoneAPI.getSettings().colorCurrentPath.value = this.pathColor.getValue();
         BaritoneAPI.getSettings().colorMostRecentConsidered.value = this.pathColor.getValue();
         BaritoneAPI.getSettings().colorNextPath.value = this.nextPathColor.getValue();
      }
   }

   @EventListener
   public void onChat(ChatMessageEvent event) {
      this.syncBaritoneSettings();
   }

   @EventListener
   public void onLoadWorld(LoadWorldEvent event) {
      this.syncBaritoneSettings();
   }

   private void syncBaritoneSettings() {
      this.rangeConfig.setValue(BaritoneAPI.getSettings().blockReachDistance.value);
      this.placeConfig.setValue(BaritoneAPI.getSettings().allowPlace.value);
      this.breakConfig.setValue(BaritoneAPI.getSettings().allowBreak.value);
      this.sprintConfig.setValue(BaritoneAPI.getSettings().allowSprint.value);
      this.inventoryConfig.setValue(BaritoneAPI.getSettings().allowInventory.value);
      this.vinesConfig.setValue(BaritoneAPI.getSettings().allowVines.value);
      this.jump256Config.setValue(BaritoneAPI.getSettings().allowJumpAt256.value);
      this.waterBucketFallConfig.setValue(BaritoneAPI.getSettings().allowWaterBucketFall.value);
      this.parkourConfig.setValue(BaritoneAPI.getSettings().allowParkour.value);
      this.parkourAscendConfig.setValue(BaritoneAPI.getSettings().allowParkourAscend.value);
      this.parkourPlaceConfig.setValue(BaritoneAPI.getSettings().allowParkourPlace.value);
      this.diagonalAscendConfig.setValue(BaritoneAPI.getSettings().allowDiagonalAscend.value);
      this.diagonalDescendConfig.setValue(BaritoneAPI.getSettings().allowDiagonalDescend.value);
      this.mineDownConfig.setValue(BaritoneAPI.getSettings().allowDownward.value);
      this.legitMineConfig.setValue(BaritoneAPI.getSettings().legitMine.value);
      this.logOnArrivalConfig.setValue(BaritoneAPI.getSettings().disconnectOnArrival.value);
      this.freeLookConfig.setValue(BaritoneAPI.getSettings().freeLook.value);
      this.antiCheatConfig.setValue(BaritoneAPI.getSettings().antiCheatCompatibility.value);
      this.strictLiquidConfig.setValue(BaritoneAPI.getSettings().strictLiquidCheck.value);
      this.censorCoordsConfig.setValue(BaritoneAPI.getSettings().censorCoordinates.value);
      this.censorCommandsConfig.setValue(BaritoneAPI.getSettings().censorRanCommands.value);
      this.chatControlConfig.setValue(BaritoneAPI.getSettings().chatControl.value);
      this.debugConfig.setValue(BaritoneAPI.getSettings().chatDebug.value);
      this.goalColor.setValue(BaritoneAPI.getSettings().colorGoalBox.value);
      this.pathColor.setValue(BaritoneAPI.getSettings().colorCurrentPath.value);
      this.pathColor.setValue(BaritoneAPI.getSettings().colorMostRecentConsidered.value);
      this.nextPathColor.setValue(BaritoneAPI.getSettings().colorNextPath.value);
   }

   @EventListener
   public void onChatText(ChatMessageEvent event) {
      String text = event.getText().getString();
      if (text.startsWith("[Baritone]")) {
         event.cancel();
         event.setText(Component.nullToEmpty("§s[Shoreline] §r" + FormattingUtil.toString(event.getText())));
      }
   }
}
