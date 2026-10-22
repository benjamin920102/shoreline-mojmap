package net.shoreline.client.impl.module.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.CapesEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.mixin.accessor.AccessorGameOptions;
import net.shoreline.eventbus.annotation.EventListener;

public final class CapesModule extends ToggleModule {
   public static CapesModule instance;
   public Config<CapesModule.Capes> clientConfig = this.register(
      new EnumConfig<>("Client", "Shows client capes", CapesModule.Capes.OFF, CapesModule.Capes.values())
   );
   Config<Boolean> optifineConfig = this.register(new BooleanConfig("Optifine", "Shows optifine capes", true));
   Config<CapesModule.CapeType> capeTypeConfig = this.register(
      new EnumConfig<>("Type", "Cape version to display", CapesModule.CapeType.RELEASE, CapesModule.CapeType.values())
   );
   private boolean capesEnabled;

   public CapesModule() {
      super("Capes", "Shows player capes", ModuleCategory.CLIENT);
      this.enable();
      instance = this;
   }

   @Override
   public void onEnable() {
      if (mc.options != null) {
         this.capesEnabled = ((AccessorGameOptions)mc.options).getPlayerModelParts().contains(PlayerModelPart.CAPE);
         mc.options.toggleModelPart(PlayerModelPart.CAPE, true);
      }
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         mc.options.toggleModelPart(PlayerModelPart.CAPE, this.capesEnabled);
      }
   }

   @EventListener
   public void onGameJoinEvent(GameJoinEvent event) {
      this.onEnable();
   }

   @EventListener
   public void onCapes(CapesEvent event) {
      if (this.clientConfig.getValue() != CapesModule.Capes.OFF) {
         if (this.optifineConfig.getValue() || this.clientConfig.getValue() != CapesModule.Capes.OFF) {
            event.cancel();
            event.setShowOptifine(this.optifineConfig.getValue());
            String capePath = this.getCapePath();
            event.setTexture(ResourceLocation.fromNamespaceAndPath("shoreline", capePath));
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
   }

   private String getCapePath() {
      StringBuilder capePath = new StringBuilder("cape");
      switch ((CapesModule.Capes)this.clientConfig.getValue()) {
         case WHITE:
            capePath.append("/white_bg");
            break;
         case BLACK:
            capePath.append("/black_bg");
      }

      switch ((CapesModule.CapeType)this.capeTypeConfig.getValue()) {
         case RELEASE:
            capePath.append("/white.png");
            break;
         case BETA:
            capePath.append("/blue.png");
            break;
         case DEV:
            capePath.append("/red.png");
      }

      return capePath.toString();
   }

   public static enum CapeType {
      RELEASE,
      BETA,
      DEV;
   }

   public static enum Capes {
      WHITE,
      BLACK,
      OFF;
   }
}
