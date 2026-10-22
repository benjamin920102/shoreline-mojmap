package net.shoreline.client.impl.module.render;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EntityListConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.chunk.light.RenderSkylightEvent;
import net.shoreline.client.impl.event.entity.FireworkTickEvent;
import net.shoreline.client.impl.event.entity.ItemTickEvent;
import net.shoreline.client.impl.event.entity.RenderFireEntityEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.impl.event.render.HurtCamEvent;
import net.shoreline.client.impl.event.render.RenderEntityInWorldEvent;
import net.shoreline.client.impl.event.render.RenderFloatingItemEvent;
import net.shoreline.client.impl.event.render.RenderFogEvent;
import net.shoreline.client.impl.event.render.RenderNauseaEvent;
import net.shoreline.client.impl.event.render.RenderWorldBorderEvent;
import net.shoreline.client.impl.event.render.block.RenderTileEntityEvent;
import net.shoreline.client.impl.event.render.block.entity.RenderSignTextEvent;
import net.shoreline.client.impl.event.render.entity.RenderArmorEvent;
import net.shoreline.client.impl.event.render.entity.RenderFireworkRocketEvent;
import net.shoreline.client.impl.event.render.entity.RenderItemEvent;
import net.shoreline.client.impl.event.render.entity.RenderItemFrameEvent;
import net.shoreline.client.impl.event.render.entity.RenderWitherSkullEvent;
import net.shoreline.client.impl.event.toast.RenderToastEvent;
import net.shoreline.client.impl.event.world.BlindnessEvent;
import net.shoreline.client.impl.module.exploit.GodModeModule;
import net.shoreline.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class NoRenderModule extends ToggleModule {
   private static NoRenderModule INSTANCE;
   Config<Boolean> hurtCamConfig = this.register(new BooleanConfig("NoHurtCam", "Prevents the hurt camera shake effect from rendering", true));
   Config<Boolean> armorConfig = this.register(new BooleanConfig("Armor", "Prevents armor pieces from rendering", false));
   Config<Boolean> fireOverlayConfig = this.register(new BooleanConfig("Overlay-Fire", "Prevents the fire Hud overlay from rendering", true));
   Config<Boolean> portalOverlayConfig = this.register(new BooleanConfig("Overlay-Portal", "Prevents the portal Hud overlay from rendering", true));
   Config<Boolean> waterOverlayConfig = this.register(new BooleanConfig("Overlay-Water", "Prevents the water Hud overlay from rendering", true));
   Config<Boolean> blockOverlayConfig = this.register(new BooleanConfig("Overlay-Block", "Prevents the block Hud overlay from rendering", true));
   Config<Boolean> spyglassOverlayConfig = this.register(new BooleanConfig("Overlay-Spyglass", "Prevents the spyglass Hud overlay from rendering", false));
   Config<Boolean> pumpkinOverlayConfig = this.register(new BooleanConfig("Overlay-Pumpkin", "Prevents the pumpkin Hud overlay from rendering", true));
   Config<Boolean> bossOverlayConfig = this.register(new BooleanConfig("Overlay-BossBar", "Prevents the boss bar Hud overlay from rendering", true));
   Config<Boolean> nauseaConfig = this.register(new BooleanConfig("Nausea", "Prevents nausea effect from rendering (includes portal effect)", false));
   Config<Boolean> blindnessConfig = this.register(new BooleanConfig("Blindness", "Prevents blindness effect from rendering", false));
   Config<Boolean> frostbiteConfig = this.register(new BooleanConfig("Frostbite", "Prevents frostbite effect from rendering", false));
   Config<Boolean> skylightConfig = this.register(new BooleanConfig("Skylight", "Prevents skylight from rendering", true));
   Config<Boolean> witherSkullsConfig = this.register(new BooleanConfig("WitherSkulls", "Prevents flying wither skulls from rendering", false));
   Config<Boolean> itemFramesConfig = this.register(new BooleanConfig("ItemFrames", "Prevents items on item frames from rendering", false));
   Config<Boolean> entitiesConfig = this.register(new BooleanConfig("Entities", "Prevents entities from rendering", false));
   Config<List<EntityType>> entitiesListConfig = this.register(new EntityListConfig<>("EntityList", "The render entity list"));
   Config<Boolean> tileEntitiesConfig = this.register(
      new BooleanConfig("TileEntities", "Prevents special tile entity properties from rendering (i.e. enchantment table books or cutting table saws)", false)
   );
   Config<Boolean> signTextConfig = this.register(new BooleanConfig("SignText", "Prevents the text on signs from rendering", false));
   Config<Boolean> fireEntityConfig = this.register(new BooleanConfig("FireEntities", "Prevents fire from rendering on entities", false));
   Config<Boolean> fireworksConfig = this.register(new BooleanConfig("Fireworks", "Prevents firework entities from rendering", true));
   Config<Boolean> totemConfig = this.register(new BooleanConfig("Totems", "Prevents totem pop overlay from rendering", false));
   Config<Boolean> worldBorderConfig = this.register(new BooleanConfig("WorldBorder", "Prevents world border from rendering", false));
   Config<NoRenderModule.FogRender> fogConfig = this.register(
      new EnumConfig<>("Fog", "Prevents fog from rendering in the world", NoRenderModule.FogRender.OFF, NoRenderModule.FogRender.values())
   );
   Config<NoRenderModule.ItemRender> itemsConfig = this.register(
      new EnumConfig<>("Items", "Prevents dropped items from rendering", NoRenderModule.ItemRender.OFF, NoRenderModule.ItemRender.values())
   );
   Config<Boolean> guiToastConfig = this.register(new BooleanConfig("GuiToast", "Prevents advancements from rendering", true));
   Config<Boolean> terrainScreenConfig = this.register(new BooleanConfig("TerrainScreen", "Prevents downloading terrain screen from rendering", false));

   public NoRenderModule() {
      super("NoRender", "Prevents certain game elements from rendering", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static NoRenderModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.itemsConfig.getValue() == NoRenderModule.ItemRender.REMOVE) {
            for (Entity entity : Lists.newArrayList(mc.level.entitiesForRendering())) {
               if (entity instanceof ItemEntity) {
                  mc.level.removeEntity(entity.getId(), RemovalReason.DISCARDED);
               }
            }
         }

         if (this.terrainScreenConfig.getValue() && mc.screen instanceof ReceivingLevelScreen && !GodModeModule.getInstance().isPortal()) {
            mc.screen = null;
         }
      }
   }

   @EventListener
   public void onHurtCam(HurtCamEvent event) {
      if (this.hurtCamConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderArmor(RenderArmorEvent event) {
      if (this.armorConfig.getValue() && event.getEntity() instanceof Player) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayFire(RenderOverlayEvent.Fire event) {
      if (this.fireOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayPortal(RenderOverlayEvent.Portal event) {
      if (this.portalOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayWater(RenderOverlayEvent.Water event) {
      if (this.waterOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayBlock(RenderOverlayEvent.Block event) {
      if (this.blockOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlaySpyglass(RenderOverlayEvent.Spyglass event) {
      if (this.spyglassOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayPumpkin(RenderOverlayEvent.Pumpkin event) {
      if (this.pumpkinOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayBossBar(RenderOverlayEvent.BossBar event) {
      if (this.bossOverlayConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderOverlayFrostbite(RenderOverlayEvent.Frostbite event) {
      if (this.frostbiteConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderNausea(RenderNauseaEvent event) {
      if (this.nauseaConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onBlindness(BlindnessEvent event) {
      if (this.blindnessConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderSkylight(RenderSkylightEvent event) {
      if (this.skylightConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderWitherSkull(RenderWitherSkullEvent event) {
      if (this.witherSkullsConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderItemFrame(RenderItemFrameEvent event) {
      if (this.itemFramesConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderEnchantingTableBook(RenderTileEntityEvent.EnchantingTableBook event) {
      if (this.tileEntitiesConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderEntityInWorld(RenderEntityInWorldEvent event) {
      if (this.shouldSkipEntity(event.getEntity())) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderSignText(RenderSignTextEvent event) {
      if (this.signTextConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderFireworkRocket(RenderFireworkRocketEvent event) {
      if (this.fireworksConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onFireworkTick(FireworkTickEvent event) {
      if (this.fireworksConfig.getValue() && !((AccessorFireworkRocketEntity)event.getFireworkRocketEntity()).hookWasShotByEntity()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderFloatingItem(RenderFloatingItemEvent event) {
      if (this.totemConfig.getValue() && event.getFloatingItem() == Items.TOTEM_OF_UNDYING) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderWorldBorder(RenderWorldBorderEvent event) {
      if (this.worldBorderConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onRenderFog(RenderFogEvent event) {
      if (this.fogConfig.getValue() == NoRenderModule.FogRender.LIQUID_VISION && mc.player != null && mc.player.isEyeInFluid(FluidTags.LAVA)) {
         event.cancel();
         event.setStart(event.getViewDistance() * 4.0F);
         event.setEnd(event.getViewDistance() * 4.25F);
      } else if (this.fogConfig.getValue() == NoRenderModule.FogRender.CLEAR) {
         event.cancel();
         event.setStart(event.getViewDistance() * 4.0F);
         event.setEnd(event.getViewDistance() * 4.25F);
      }
   }

   @EventListener
   public void onRenderItem(RenderItemEvent event) {
      if (this.itemsConfig.getValue() == NoRenderModule.ItemRender.HIDE) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderToast(RenderToastEvent event) {
      if (this.guiToastConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderFireEntity(RenderFireEntityEvent event) {
      if (this.fireEntityConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onItemTick(ItemTickEvent event) {
      if (this.itemsConfig.getValue() != NoRenderModule.ItemRender.OFF) {
         event.cancel();
      }
   }

   public boolean shouldSkipEntity(Entity entity) {
      return this.entitiesConfig.getValue() && this.entitiesListConfig.getValue().contains(entity.getType());
   }

   public static enum FogRender {
      CLEAR,
      LIQUID_VISION,
      OFF;
   }

   public static enum ItemRender {
      REMOVE,
      HIDE,
      OFF;
   }
}
