package net.shoreline.client.impl.module.render;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.gui.hud.RenderCrosshairEvent;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.render.animation.Easing;
import net.shoreline.eventbus.annotation.EventListener;

public class CrosshairModule extends ToggleModule {
   Config<Float> lengthConfig = this.register(new NumberConfig<>("Length", "The crosshair length", 0.0F, 1.0F, 2.5F));
   Config<Float> thicknessConfig = this.register(new NumberConfig<>("Thickness", "The crosshair thickness", 0.1F, 0.5F, 2.0F));
   Config<Integer> gapConfig = this.register(new NumberConfig<>("Gap", "The gap between the lines", 1, 2, 5));
   Config<Boolean> dynamicConfig = this.register(new BooleanConfig("Dynamic", "Indicates when the player is moving", false));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Outlines the crosshair", true));
   Config<Float> outlineThicknessConfig = this.register(
      new NumberConfig<>("OutlineThickness", "The width of the outline", 0.1F, 0.3F, 0.5F, () -> this.outlineConfig.getValue())
   );
   Config<Float> opacityConfig = this.register(new NumberConfig<>("Opacity", "The crosshair opacity", 0.1F, 1.0F, 1.0F));
   Config<Color> colorConfig = this.register(new ColorConfig("Color", "The crosshair color", Color.WHITE));
   private final Animation gapAnimation = new Animation(false, 100.0F, Easing.LINEAR);

   public CrosshairModule() {
      super("Crosshair", "Custom crosshairs", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderCrosshair(RenderCrosshairEvent event) {
      event.cancel();
      if (mc.options.getCameraType().isFirstPerson() && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         GuiGraphics context = event.getContext();
         float x = context.guiWidth() / 2.0F - 0.5F;
         float y = context.guiHeight() / 2.0F - 0.5F;
         float halfLength = this.lengthConfig.getValue() * 10.0F / 2.0F;
         float o1 = this.outlineThicknessConfig.getValue();
         float o2 = o1 * 2.0F;
         float o3 = o1 * 3.0F;
         boolean moving = MovementUtil.isMovingInput() || mc.player.isShiftKeyDown() || mc.player.onClimbable() || !mc.player.onGround();
         double gap = this.gapConfig.getValue().intValue();
         if (this.dynamicConfig.getValue()) {
            this.gapAnimation.setState(moving);
            gap += 2.0 * this.gapAnimation.getFactor();
         }

         if (this.outlineConfig.getValue()) {
            RenderManager.rect(
               context.pose(),
               x - halfLength - gap - o2,
               y - this.thicknessConfig.getValue() - o1,
               halfLength + o3,
               this.thicknessConfig.getValue() * 2.0F + o2,
               ColorUtil.withAlpha(Color.BLACK.getRGB(), this.opacityConfig.getValue())
            );
            RenderManager.rect(
               context.pose(),
               x + gap - o2,
               y - this.thicknessConfig.getValue() - o1,
               halfLength + o3,
               this.thicknessConfig.getValue() * 2.0F + o2,
               ColorUtil.withAlpha(Color.BLACK.getRGB(), this.opacityConfig.getValue())
            );
         }

         RenderManager.rect(
            context.pose(),
            x - halfLength - gap,
            y - this.thicknessConfig.getValue(),
            halfLength,
            this.thicknessConfig.getValue() * 2.0F,
            ColorUtil.withAlpha(this.colorConfig.getValue().getRGB(), this.opacityConfig.getValue())
         );
         RenderManager.rect(
            context.pose(),
            x + gap,
            y - this.thicknessConfig.getValue(),
            halfLength,
            this.thicknessConfig.getValue() * 2.0F,
            ColorUtil.withAlpha(this.colorConfig.getValue().getRGB(), this.opacityConfig.getValue())
         );
         if (this.outlineConfig.getValue()) {
            RenderManager.rect(
               context.pose(),
               x - this.thicknessConfig.getValue() - o1,
               y - halfLength - gap - o1,
               this.thicknessConfig.getValue() * 2.0F + o2,
               halfLength + o3,
               ColorUtil.withAlpha(Color.BLACK.getRGB(), this.opacityConfig.getValue())
            );
            RenderManager.rect(
               context.pose(),
               x - this.thicknessConfig.getValue() - o1,
               y + gap - o1,
               this.thicknessConfig.getValue() * 2.0F + o2,
               halfLength + o3,
               ColorUtil.withAlpha(Color.BLACK.getRGB(), this.opacityConfig.getValue())
            );
         }

         RenderManager.rect(
            context.pose(),
            x - this.thicknessConfig.getValue(),
            y - halfLength - gap,
            this.thicknessConfig.getValue() * 2.0F,
            halfLength,
            ColorUtil.withAlpha(this.colorConfig.getValue().getRGB(), this.opacityConfig.getValue())
         );
         RenderManager.rect(
            context.pose(),
            x - this.thicknessConfig.getValue(),
            y + gap,
            this.thicknessConfig.getValue() * 2.0F,
            halfLength,
            ColorUtil.withAlpha(this.colorConfig.getValue().getRGB(), this.opacityConfig.getValue())
         );
      }
   }
}
