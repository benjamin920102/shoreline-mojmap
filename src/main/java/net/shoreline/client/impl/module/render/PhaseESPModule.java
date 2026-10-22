package net.shoreline.client.impl.module.render;

import java.awt.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

public class PhaseESPModule extends ToggleModule {
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the render", 1.0F, 1.0F, 5.0F));
   Config<Boolean> diagonalConfig = this.register(new BooleanConfig("Diagonal", "Renders safe diagonal phase blocks", true));
   Config<Boolean> safeConfig = this.register(new BooleanConfig("Safe", "Highlights safe phase blocks", false));
   Config<Color> unsafeConfig = this.register(new ColorConfig("UnsafeColor", "The color for rendering unsafe phase blocks", new Color(255, 0, 0), false, false));
   Config<Color> obsidianConfig = this.register(
      new ColorConfig("ObsidianColor", "The color for rendering obsidian phase blocks", new Color(255, 255, 0), false, false, () -> this.safeConfig.getValue())
   );
   Config<Color> bedrockConfig = this.register(
      new ColorConfig("BedrockColor", "The color for rendering bedrock phase blocks", new Color(0, 255, 0), false, false, () -> this.safeConfig.getValue())
   );

   public PhaseESPModule() {
      super("PhaseESP", "Displays safe phase blocks", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.level != null && mc.player.onGround()) {
         RenderBuffers.preRender();
         BlockPos playerPos = mc.player.blockPosition();
         Vec3 pos = mc.player.position();
         double dx = pos.x() - playerPos.getX();
         double dz = pos.z() - playerPos.getZ();

         for (Direction direction : Direction.values()) {
            if (direction.getAxis().isHorizontal()) {
               BlockPos blockPos = playerPos.relative(direction);
               if (!mc.level.getBlockState(blockPos).canBeReplaced()) {
                  Color color = this.getPhaseColor(blockPos);
                  if (color != null) {
                     double x = blockPos.getX();
                     double y = blockPos.getY();
                     double z = blockPos.getZ();
                     if (direction == Direction.EAST && dx >= 0.65) {
                        RenderManager.renderLine(event.getMatrices(), x, y, z, x, y, z + 1.0, this.widthConfig.getValue(), color.getRGB());
                     } else if (direction == Direction.WEST && dx <= 0.35) {
                        RenderManager.renderLine(event.getMatrices(), x + 1.0, y, z, x + 1.0, y, z + 1.0, this.widthConfig.getValue(), color.getRGB());
                     } else if (direction == Direction.SOUTH && dz >= 0.65) {
                        RenderManager.renderLine(event.getMatrices(), x, y, z, x + 1.0, y, z, this.widthConfig.getValue(), color.getRGB());
                     } else if (direction == Direction.NORTH && dz <= 0.35) {
                        RenderManager.renderLine(event.getMatrices(), x, y, z + 1.0, x + 1.0, y, z + 1.0, this.widthConfig.getValue(), color.getRGB());
                     }
                  }
               }
            }
         }

         if (this.diagonalConfig.getValue()) {
            double x = playerPos.getX();
            double y = playerPos.getY();
            double z = playerPos.getZ();
            BlockPos currentPos = playerPos.relative(Direction.WEST).relative(Direction.NORTH);
            Color color1 = this.getPhaseColor(currentPos);
            if (color1 != null && dx <= 0.35 && dz <= 0.35) {
               RenderManager.renderLine(event.getMatrices(), x, y, z, x, y + 1.0, z, this.widthConfig.getValue(), color1.getRGB());
            }

            currentPos = playerPos.relative(Direction.WEST).relative(Direction.SOUTH);
            Color color2 = this.getPhaseColor(currentPos);
            if (color2 != null && dx <= 0.35 && dz >= 0.65) {
               RenderManager.renderLine(event.getMatrices(), x, y, z + 1.0, x, y + 1.0, z + 1.0, this.widthConfig.getValue(), color2.getRGB());
            }

            currentPos = playerPos.relative(Direction.EAST).relative(Direction.NORTH);
            Color color3 = this.getPhaseColor(currentPos);
            if (color3 != null && dx >= 0.65 && dz <= 0.35) {
               RenderManager.renderLine(event.getMatrices(), x + 1.0, y, z, x + 1.0, y + 1.0, z, this.widthConfig.getValue(), color3.getRGB());
            }

            currentPos = playerPos.relative(Direction.EAST).relative(Direction.SOUTH);
            Color color4 = this.getPhaseColor(currentPos);
            if (color4 != null && dx >= 0.65 && dz >= 0.65) {
               RenderManager.renderLine(event.getMatrices(), x + 1.0, y, z + 1.0, x + 1.0, y + 1.0, z + 1.0, this.widthConfig.getValue(), color4.getRGB());
            }
         }

         RenderBuffers.postRender();
      }
   }

   private Color getPhaseColor(BlockPos blockPos) {
      BlockState state1 = mc.level.getBlockState(blockPos);
      if (state1.isAir()) {
         return null;
      } else {
         BlockState state = mc.level.getBlockState(blockPos.below());
         Color color = null;
         if (state.canBeReplaced()) {
            color = this.unsafeConfig.getValue();
         } else if (this.safeConfig.getValue()) {
            if (BlastResistantBlocks.isUnbreakable(state.getBlock())) {
               color = this.bedrockConfig.getValue();
            } else {
               color = this.obsidianConfig.getValue();
            }
         }

         return color;
      }
   }
}
