package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.BoxRender;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.render.RenderBlockOutlineEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.eventbus.annotation.EventListener;

public class BlockHighlightModule extends ToggleModule {
   Config<BoxRender> boxModeConfig = this.register(new EnumConfig<>("BoxMode", "Box rendering mode", BoxRender.OUTLINE, BoxRender.values()));
   Config<Boolean> entitiesConfig = this.register(new BooleanConfig("Debug-Entities", "Highlights entity bounding boxes for debug purposes", false));
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the highlight", 1.0F, 1.0F, 5.0F));
   Config<Boolean> fadeConfig = this.register(new BooleanConfig("Fade", "Fades the block highlight", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 200, 1000, () -> false));
   private double distance;
   private final Map<AABB, Animation> fadeList = new HashMap<>();

   public BlockHighlightModule() {
      super("BlockHighlight", "Highlights the block the player is facing", ModuleCategory.RENDER);
   }

   @Override
   public String getModuleData() {
      if (mc.level == null) {
         return super.getModuleData();
      } else if (mc.hitResult instanceof BlockHitResult result && mc.level.getBlockState(result.getBlockPos()).isAir()) {
         return super.getModuleData();
      } else {
         DecimalFormat decimal = new DecimalFormat("0.0");
         return decimal.format(this.distance);
      }
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.level != null) {
         if (!this.fadeConfig.getValue() && !this.fadeList.isEmpty()) {
            this.fadeList.clear();
         }

         AABB render = null;
         HitResult result = mc.hitResult;
         if (result != null) {
            Vec3 pos = Managers.POSITION.getEyePos();
            if (this.entitiesConfig.getValue() && result.getType() == Type.ENTITY) {
               Entity entity = ((EntityHitResult)result).getEntity();
               render = entity.getBoundingBox();
               this.distance = pos.distanceTo(entity.position());
               if (this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
               }
            } else if (result.getType() == Type.BLOCK) {
               BlockPos hpos = ((BlockHitResult)result).getBlockPos();
               BlockState state = mc.level.getBlockState(hpos);
               VoxelShape outlineShape = state.getShape(mc.level, hpos);
               if (outlineShape.isEmpty()) {
                  return;
               }

               AABB render1 = outlineShape.bounds();
               render = new AABB(
                  hpos.getX() + render1.minX,
                  hpos.getY() + render1.minY,
                  hpos.getZ() + render1.minZ,
                  hpos.getX() + render1.maxX,
                  hpos.getY() + render1.maxY,
                  hpos.getZ() + render1.maxZ
               );
               this.distance = pos.distanceTo(hpos.getCenter());
               if (this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
               }
            }
         }

         RenderBuffers.preRender();
         if (this.fadeConfig.getValue()) {
            for (Entry<AABB, Animation> set : this.fadeList.entrySet()) {
               AABB box = set.getKey();
               set.getValue().setState(false);
               if (!(set.getValue().getFactor() < 0.01F)) {
                  int boxAlpha = (int)(40.0 * set.getValue().getFactor());
                  int lineAlpha = (int)(145.0 * set.getValue().getFactor());
                  this.renderBb(event.getMatrices(), box, ColorsModule.getInstance().getRGB(boxAlpha), ColorsModule.getInstance().getRGB(lineAlpha));
               }
            }
         } else if (render != null) {
            this.renderBb(event.getMatrices(), render, ColorsModule.getInstance().getRGB(40), ColorsModule.getInstance().getRGB(145));
         }

         RenderBuffers.postRender();
      }
   }

   private void renderBb(PoseStack matrixStack, AABB render, int color, int lineColor) {
      switch ((BoxRender)this.boxModeConfig.getValue()) {
         case FILL:
            RenderManager.renderBox(matrixStack, render, color);
            RenderManager.renderBoundingBox(matrixStack, render, this.widthConfig.getValue(), lineColor);
            break;
         case OUTLINE:
            RenderManager.renderBoundingBox(matrixStack, render, this.widthConfig.getValue(), lineColor);
      }
   }

   @EventListener
   public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
      event.cancel();
   }
}
