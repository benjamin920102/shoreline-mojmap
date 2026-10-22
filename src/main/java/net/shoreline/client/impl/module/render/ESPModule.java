package net.shoreline.client.impl.module.render;

import java.awt.Color;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.EntityOutlineEvent;
import net.shoreline.client.impl.event.entity.decoration.TeamColorEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class ESPModule extends ToggleModule {
   private static ESPModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The ESP render range", 10.0F, 50.0F, 200.0F));
   Config<ESPModule.ESPMode> modeConfig = this.register(new EnumConfig<>("Mode", "ESP rendering mode", ESPModule.ESPMode.BOX, ESPModule.ESPMode.values()));
   Config<Boolean> fillConfig = this.register(
      new BooleanConfig("Fill", "Fills the box render", false, () -> this.modeConfig.getValue() == ESPModule.ESPMode.BOX)
   );
   Config<Float> widthConfig = this.register(
      new NumberConfig<>("Width", "ESP rendering line width", 0.1F, 2.0F, 5.0F, () -> this.modeConfig.getValue() == ESPModule.ESPMode.BOX)
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Render players through walls", true));
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Render self through walls", true));
   Config<Color> playersColorConfig = this.register(
      new ColorConfig(
         "PlayersColor", "The render color for players", new Color(200, 60, 60), false, () -> this.playersConfig.getValue() || this.selfConfig.getValue()
      )
   );
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Render monsters through walls", true));
   Config<Color> monstersColorConfig = this.register(
      new ColorConfig("MonstersColor", "The render color for monsters", new Color(200, 60, 60), false, () -> this.monstersConfig.getValue())
   );
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Render animals through walls", true));
   Config<Color> animalsColorConfig = this.register(
      new ColorConfig("AnimalsColor", "The render color for animals", new Color(0, 200, 0), false, () -> this.animalsConfig.getValue())
   );
   Config<Boolean> vehiclesConfig = this.register(new BooleanConfig("Vehicles", "Render vehicles through walls", false));
   Config<Color> vehiclesColorConfig = this.register(
      new ColorConfig("VehiclesColor", "The render color for vehicles", new Color(200, 100, 0), false, () -> this.vehiclesConfig.getValue())
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Render dropped items through walls", false));
   Config<Color> itemsColorConfig = this.register(
      new ColorConfig("ItemsColor", "The render color for items", new Color(200, 100, 0), false, () -> this.itemsConfig.getValue())
   );
   Config<Boolean> crystalsConfig = this.register(new BooleanConfig("EndCrystals", "Render end crystals through walls", false));
   Config<Color> crystalsColorConfig = this.register(
      new ColorConfig("EndCrystalsColor", "The render color for end crystals", new Color(200, 100, 200), false, () -> this.crystalsConfig.getValue())
   );

   public ESPModule() {
      super("ESP", "See entities and objects through walls", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static ESPModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.modeConfig.getValue() == ESPModule.ESPMode.BOX) {
         RenderBuffers.preRender();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player && this.checkESP(entity)) {
               Color espColor = this.getESPColor(entity);
               AABB box = Interpolation.getInterpolatedEntityBox(entity);
               if (this.fillConfig.getValue()) {
                  RenderManager.renderBox(event.getMatrices(), box, ColorUtil.withAlpha(espColor.getRGB(), 60));
               }

               RenderManager.renderBoundingBox(event.getMatrices(), box, this.widthConfig.getValue(), ColorUtil.withAlpha(espColor.getRGB(), 144));
            }
         }

         RenderBuffers.postRender();
      }
   }

   @EventListener
   public void onEntityOutline(EntityOutlineEvent event) {
      if (mc.player != null && this.modeConfig.getValue() == ESPModule.ESPMode.GLOW && this.checkESP(event.getEntity())) {
         if (mc.player.distanceToSqr(event.getEntity()) > ((NumberConfig)this.rangeConfig).getValueSq()) {
            return;
         }

         event.cancel();
      }
   }

   @EventListener
   public void onTeamColor(TeamColorEvent event) {
      if (mc.player != null && this.modeConfig.getValue() == ESPModule.ESPMode.GLOW && this.checkESP(event.getEntity())) {
         if (mc.player.distanceToSqr(event.getEntity()) > ((NumberConfig)this.rangeConfig).getValueSq()) {
            return;
         }

         event.cancel();
         event.setColor(this.getESPColor(event.getEntity()).getRGB());
      }
   }

   public Color getESPColor(Entity entity) {
      if (entity instanceof Player player) {
         return Managers.SOCIAL.isFriend(player.getName()) ? SocialsModule.getInstance().getFriendColor() : this.playersColorConfig.getValue();
      } else if (EntityUtil.isMonster(entity)) {
         return this.monstersColorConfig.getValue();
      } else if (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) {
         return this.animalsColorConfig.getValue();
      } else if (EntityUtil.isVehicle(entity)) {
         return this.vehiclesColorConfig.getValue();
      } else if (entity instanceof EndCrystal) {
         return this.crystalsColorConfig.getValue();
      } else {
         return entity instanceof ItemEntity ? this.itemsColorConfig.getValue() : null;
      }
   }

   public boolean checkESP(Entity entity) {
      return entity instanceof Player && this.playersConfig.getValue()
         ? this.selfConfig.getValue() || entity != mc.player
         : EntityUtil.isMonster(entity) && this.monstersConfig.getValue()
            || (EntityUtil.isNeutral(entity) || EntityUtil.isPassive(entity)) && this.animalsConfig.getValue()
            || EntityUtil.isVehicle(entity) && this.vehiclesConfig.getValue()
            || entity instanceof EndCrystal && this.crystalsConfig.getValue()
            || entity instanceof ItemEntity && this.itemsConfig.getValue();
   }

   public static enum ESPMode {
      BOX,
      GLOW;
   }
}
