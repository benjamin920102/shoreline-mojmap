package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.manager.combat.hole.Hole;
import net.shoreline.client.impl.manager.combat.hole.HoleType;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

public class HoleESPModule extends ToggleModule {
   private static HoleESPModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to display holes", 3.0F, 5.0F, 25.0F));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Renders an outline around the hole", true));
   Config<Boolean> crossConfig = this.register(new BooleanConfig("Cross", "Renders a cross on the floor of the hole", false));
   Config<Float> heightConfig = this.register(new NumberConfig<>("Size", "Render height of holes", -1.0F, 1.0F, 1.0F));
   Config<Boolean> ignoreSelfConfig = this.register(new BooleanConfig("IgnoreSelf", "Ignores the hole the player is standing in", false));
   Config<Boolean> obsidianCheckConfig = this.register(new BooleanConfig("Obsidian", "Displays obsidian holes", true));
   Config<Boolean> obsidianBedrockConfig = this.register(new BooleanConfig("Obsidian-Bedrock", "Displays mixed obsidian and bedrock holes", true));
   Config<Boolean> doubleConfig = this.register(
      new BooleanConfig("Double", "Displays double holes where the player can stand in the middle of two blocks to block explosion damage", false)
   );
   Config<Boolean> quadConfig = this.register(
      new BooleanConfig("Quad", "Displays quad holes where the player can stand in the middle of four blocks to block explosion damage", false)
   );
   Config<Boolean> voidConfig = this.register(new BooleanConfig("Void", "Displays void holes in the world", false));
   Config<Color> obsidianConfig = this.register(
      new ColorConfig("ObsidianColor", "The color for rendering obsidian holes", new Color(255, 0, 0, 80), () -> this.obsidianCheckConfig.getValue())
   );
   Config<Color> mixedConfig = this.register(
      new ColorConfig("Obsidian-BedrockColor", "The color for rendering mixed holes", new Color(255, 255, 0, 80), () -> this.obsidianBedrockConfig.getValue())
   );
   Config<Color> bedrockConfig = this.register(new ColorConfig("BedrockColor", "The color for rendering bedrock holes", new Color(0, 255, 0, 80)));
   Config<Color> voidColorConfig = this.register(
      new ColorConfig("VoidColor", "The color for rendering bedrock holes", new Color(255, 0, 0, 140), () -> this.voidConfig.getValue())
   );
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 300, 1000, () -> false));
   private final Map<Hole, Animation> fadeList = new HashMap<>();

   public HoleESPModule() {
      super("HoleESP", "Displays nearby blast resistant holes", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
   }

   public static HoleESPModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null) {
         for (Hole hole : Managers.HOLE.getHoles()) {
            if (!this.containsPos(this.fadeList.keySet(), hole)) {
               Animation anim = new Animation(false, this.fadeTimeConfig.getValue().intValue());
               this.fadeList.put(hole, anim);
            }
         }

         RenderBuffers.preRender();

         for (Entry<Hole, Animation> set : this.fadeList.entrySet()) {
            Hole holex = set.getKey();
            double dist = holex.squaredDistanceTo(mc.player);
            if (dist > ((NumberConfig)this.rangeConfig).getValueSq()) {
               set.getValue().setState(false);
            } else if (this.ignoreSelfConfig.getValue() && mc.player.getBoundingBox().intersects(holex.getBoundingBox(0.5))) {
               set.getValue().setState(false);
            } else if ((!holex.isDoubleX() && !holex.isDoubleZ() || this.doubleConfig.getValue())
               && (!holex.isQuad() || this.quadConfig.getValue())
               && (holex.getSafety() != HoleType.VOID || this.voidConfig.getValue())
               && (holex.getSafety() != HoleType.OBSIDIAN || this.obsidianCheckConfig.getValue())
               && (holex.getSafety() != HoleType.OBSIDIAN_BEDROCK || this.obsidianBedrockConfig.getValue())) {
               set.getValue().setState(this.containsPos(Managers.HOLE.getHoles(), holex));
            } else {
               set.getValue().setState(false);
            }

            if (!(set.getValue().getFactor() < 0.01F)) {
               Color color = this.getHoleColor(holex.getSafety());
               int boxAlpha = (int)(color.getAlpha() * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               this.renderHole(event.getMatrices(), holex, this.getHoleColor(holex.getSafety(), boxAlpha), this.getHoleColor(holex.getSafety(), lineAlpha));
            }
         }

         RenderBuffers.postRender();
      }
   }

   private void renderHole(PoseStack matrixStack, Hole hole, int color1, int color2) {
      AABB render = hole.getBoundingBox(this.heightConfig.getValue().floatValue());
      RenderManager.renderBox(matrixStack, render, color1);
      if (this.outlineConfig.getValue()) {
         RenderManager.renderBoundingBox(matrixStack, render, 1.5F, color2);
      }

      if (this.crossConfig.getValue()) {
         RenderManager.renderBoundingCross(matrixStack, render, 2.0F, color2);
      }
   }

   private boolean containsPos(Set<Hole> set, Hole hole) {
      return set.stream()
         .anyMatch(
            hole1 -> {
               if (hole1.isDoubleX() != hole.isDoubleX()
                  || hole1.isDoubleZ() != hole.isDoubleZ()
                  || hole1.isQuad() != hole.isQuad()
                  || hole1.isStandard() != hole.isStandard()) {
                  return false;
               } else {
                  return hole.getSafety() != hole1.getSafety() ? false : hole.equals(hole1);
               }
            }
         );
   }

   private int getHoleColor(HoleType holeType, int alpha) {
      return switch (holeType) {
         case OBSIDIAN -> ((ColorConfig)this.obsidianConfig).getRgb(alpha);
         case OBSIDIAN_BEDROCK -> ((ColorConfig)this.mixedConfig).getRgb(alpha);
         case BEDROCK -> ((ColorConfig)this.bedrockConfig).getRgb(alpha);
         case VOID -> ((ColorConfig)this.voidColorConfig).getRgb(alpha);
      };
   }

   private Color getHoleColor(HoleType holeType) {
      return switch (holeType) {
         case OBSIDIAN -> (Color)this.obsidianConfig.getValue();
         case OBSIDIAN_BEDROCK -> (Color)this.mixedConfig.getValue();
         case BEDROCK -> (Color)this.bedrockConfig.getValue();
         case VOID -> (Color)this.voidColorConfig.getValue();
      };
   }

   public double getRange() {
      return this.rangeConfig.getValue().floatValue();
   }
}
