package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorCamera;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Matrix4f;

public class TracersModule extends ToggleModule {
   Config<TracersModule.RenderMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "Render tracers to entities not visible on the screen", TracersModule.RenderMode.NORMAL, TracersModule.RenderMode.values())
   );
   Config<TracersModule.Target> targetConfig = this.register(
      new EnumConfig<>("Target", "The body part of the entity to target", TracersModule.Target.FEET, TracersModule.Target.values())
   );
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the tracer", 1.0F, 1.0F, 5.0F));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Render tracers to player", true));
   Config<Color> playersColorConfig = this.register(
      new ColorConfig("PlayersColor", "The render color for players", new Color(200, 60, 60), false, () -> this.playersConfig.getValue())
   );
   Config<Boolean> friendsConfig = this.register(
      new BooleanConfig("Friends", "Render tracers to players that are friended", false, () -> this.playersConfig.getValue())
   );
   Config<Boolean> invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Render tracers to invisible entities", false));
   Config<Color> invisiblesColorConfig = this.register(
      new ColorConfig("InvisiblesColor", "The render color for invisibles", new Color(200, 100, 0), false, () -> this.invisiblesConfig.getValue())
   );
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Render tracers to monsters", false));
   Config<Color> monstersColorConfig = this.register(
      new ColorConfig("MonstersColor", "The render color for monsters", new Color(200, 60, 60), false, () -> this.monstersConfig.getValue())
   );
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Render tracers to animals", false));
   Config<Color> animalsColorConfig = this.register(
      new ColorConfig("AnimalsColor", "The render color for animals", new Color(0, 200, 0), false, () -> this.animalsConfig.getValue())
   );
   Config<Boolean> vehiclesConfig = this.register(new BooleanConfig("Vehicles", "Render tracers to vehicles", false));
   Config<Color> vehiclesColorConfig = this.register(
      new ColorConfig("VehiclesColor", "The render color for vehicles", new Color(200, 100, 0), false, () -> this.vehiclesConfig.getValue())
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Render tracers to items", false));
   Config<Color> itemsColorConfig = this.register(
      new ColorConfig("ItemsColor", "The render color for items", new Color(255, 255, 255), false, () -> this.itemsConfig.getValue())
   );

   public TracersModule() {
      super("Tracers", "Draws a tracer to all entities in render distance", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.getCameraEntity() != null && mc.getCameraEntity() instanceof Player playerEntity && !mc.options.hideGui) {
         PoseStack matrixStack = new PoseStack();
         double d = mc.options.fov().get().intValue();
         matrixStack.mulPose(mc.gameRenderer.getProjectionMatrix(d));
         Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
         RenderSystem.setProjectionMatrix(matrixStack.last().pose(), VertexSorting.DISTANCE_TO_ORIGIN);
         RenderBuffers.preRender();
         Vec3 playerPos = Interpolation.getRenderPosition(playerEntity, event.getTickDelta());
         Camera camera = mc.gameRenderer.getMainCamera();
         double eyeHeight = Mth.lerp(event.getTickDelta(), ((AccessorCamera)camera).getLastCameraY(), ((AccessorCamera)camera).getCameraY());
         double x1 = playerEntity.getX() - playerPos.x();
         double y1 = playerEntity.getY() - playerPos.y() + eyeHeight;
         double z1 = playerEntity.getZ() - playerPos.z();
         float pitch = playerEntity.getXRot();
         float yaw = playerEntity.getYRot();
         if (FreecamModule.getInstance().isEnabled()) {
            Vec3 pos1 = FreecamModule.getInstance().getCameraPosition();
            Vec3 pos2 = Interpolation.getRenderPosition(pos1, FreecamModule.getInstance().getLastCameraPosition(), event.getTickDelta());
            float[] rotations = FreecamModule.getInstance().getCameraRotations();
            x1 = pos1.x - pos2.x;
            y1 = pos1.y - pos2.y;
            z1 = pos1.z - pos2.z;
            yaw = rotations[0];
            pitch = rotations[1];
         }

         Vec3 pos = new Vec3(0.0, 0.0, 1.0).xRot(-((float)Math.toRadians(pitch))).yRot(-((float)Math.toRadians(yaw))).add(new Vec3(x1, y1, z1));

         for (Entity entity : mc.level.entitiesForRendering()) {
            boolean shouldDraw = switch ((TracersModule.RenderMode)this.modeConfig.getValue()) {
               case ON_SCREEN -> RenderManager.isFrustumVisible(entity.getBoundingBox());
               case OFF_SCREEN -> !RenderManager.isFrustumVisible(entity.getBoundingBox());
               case NORMAL -> true;
            };
            if (entity != null
               && entity.isAlive()
               && entity != mc.player
               && shouldDraw
               && (!(entity instanceof Player) || !Managers.SOCIAL.isFriend(entity.getDisplayName()) || this.friendsConfig.getValue())) {
               Color color = this.getTracerColor(entity);
               if (color != null) {
                  Vec3 entityPos = Interpolation.getRenderPosition(entity, event.getTickDelta());
                  double x2 = entity.getX() - entityPos.x();
                  double y2 = entity.getY() - entityPos.y();
                  double z2 = entity.getZ() - entityPos.z();
                  RenderManager.renderLine(
                     event.getMatrices(), pos, new Vec3(x2, y2, z2).add(0.0, this.getTargetY(entity), 0.0), this.widthConfig.getValue(), color.getRGB()
                  );
               }
            }
         }

         RenderBuffers.postRender();
         RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
      }
   }

   private Color getTracerColor(Entity entity) {
      if (entity.isInvisible() && this.invisiblesConfig.getValue()) {
         return this.invisiblesColorConfig.getValue();
      } else if (entity instanceof Player player && this.playersConfig.getValue()) {
         return Managers.SOCIAL.isFriend(player.getName()) ? SocialsModule.getInstance().getFriendColor() : this.playersColorConfig.getValue();
      } else if (EntityUtil.isMonster(entity) && this.monstersConfig.getValue()) {
         return this.monstersColorConfig.getValue();
      } else if ((EntityUtil.isPassive(entity) || EntityUtil.isNeutral(entity)) && this.animalsConfig.getValue()) {
         return this.animalsColorConfig.getValue();
      } else if (EntityUtil.isVehicle(entity) && this.vehiclesConfig.getValue()) {
         return this.vehiclesColorConfig.getValue();
      } else {
         return entity instanceof ItemEntity && this.itemsConfig.getValue() ? this.itemsColorConfig.getValue() : null;
      }
   }

   private double getTargetY(Entity entity) {
      return switch ((TracersModule.Target)this.targetConfig.getValue()) {
         case FEET -> 0.0;
         case TORSO -> entity.getBbHeight() / 2.0;
         case HEAD -> entity.getEyeHeight(entity.getPose());
      };
   }

   public static enum RenderMode {
      ON_SCREEN,
      OFF_SCREEN,
      NORMAL;
   }

   public static enum Target {
      FEET,
      TORSO,
      HEAD;
   }
}
