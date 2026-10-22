package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Matrix4f;

public class TrajectoriesModule extends ToggleModule {
   public TrajectoriesModule() {
      super("Trajectories", "Renders the trajectory path of projectiles", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      ItemStack mainhand = mc.player.getMainHandItem();
      Item throwItem = mainhand.getItem();
      PoseStack matrixStack = new PoseStack();
      double d = mc.options.fov().get().intValue();
      matrixStack.mulPose(mc.gameRenderer.getProjectionMatrix(d));
      Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
      RenderSystem.setProjectionMatrix(matrixStack.last().pose(), VertexSorting.DISTANCE_TO_ORIGIN);
      RenderBuffers.preRender();
      if ((throwItem instanceof BowItem || throwItem instanceof CrossbowItem || throwItem instanceof TridentItem) && mc.player.getTicksUsingItem() > 0
         || this.isThrowableItem(throwItem)) {
         float yaw = mc.player.getYRot();
         double x = Interpolation.interpolateDouble(mc.player.xo, mc.player.getX(), mc.getTimer().getGameTimeDeltaPartialTick(true));
         double y = Interpolation.interpolateDouble(mc.player.yo, mc.player.getY(), mc.getTimer().getGameTimeDeltaPartialTick(true));
         double z = Interpolation.interpolateDouble(mc.player.zo, mc.player.getZ(), mc.getTimer().getGameTimeDeltaPartialTick(true));
         y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
         if (throwItem == mc.player.getMainHandItem().getItem()) {
            x -= Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
            z -= Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
         } else {
            x += Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
            z += Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
         }

         float n = throwItem instanceof BowItem ? 1.0F : 0.4F;
         double motionX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(mc.player.getXRot() / 180.0F * (float) Math.PI) * n;
         double motionY = -Mth.sin(
               (
                     mc.player.getXRot()
                        + (
                           !(throwItem instanceof SplashPotionItem)
                                 && !(throwItem instanceof LingeringPotionItem)
                                 && !(throwItem instanceof ExperienceBottleItem)
                              ? 0
                              : -20
                        )
                  )
                  / 180.0F
                  * (float) Math.PI
            )
            * n;
         double motionZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(mc.player.getXRot() / 180.0F * (float) Math.PI) * n;
         float power = mc.player.getTicksUsingItem() / 20.0F;
         power = (power * power + power * 2.0F) / 3.0F;
         if (power > 1.0F) {
            power = 1.0F;
         }

         float dist = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
         motionX /= dist;
         motionY /= dist;
         motionZ /= dist;
         float pow = (throwItem instanceof BowItem ? power * 2.0F : (throwItem instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getProjectilePower(throwItem);
         motionX *= pow;
         motionY *= pow;
         motionZ *= pow;
         if (!mc.player.onGround()) {
            motionY += mc.player.getDeltaMovement().y();
         }

         float size = !(throwItem instanceof BowItem) && !(throwItem instanceof CrossbowItem) ? 0.25F : 0.3F;
         boolean hasLanded = false;

         while (!hasLanded) {
            boolean landing = false;
            Vec3 lastPos = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            BlockPos blockPos = BlockPos.containing(x, y, z);
            if (mc.level.getBlockState(blockPos).getBlock() == Blocks.WATER) {
               motionX *= 0.8;
               motionY *= 0.8;
               motionZ *= 0.8;
            } else {
               motionX *= 0.99;
               motionY *= 0.99;
               motionZ *= 0.99;
            }

            if (throwItem instanceof BowItem) {
               motionY -= 0.05F;
            } else if (mc.player.getMainHandItem().getItem() instanceof CrossbowItem) {
               motionY -= 0.05F;
            } else {
               motionY -= 0.03F;
            }

            AABB projectileBox = new AABB(x - size, y - size, z - size, x + size, y + size, z + size);

            for (Entity entity : mc.level.getEntities(null, projectileBox)) {
               if (entity != mc.player && !this.isThrowableEntity(entity)) {
                  RenderManager.renderBox(event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), ColorsModule.getInstance().getRGB(40));
                  RenderManager.renderBoundingBox(
                     event.getMatrices(), Interpolation.getInterpolatedEntityBox(entity), 1.5F, ColorsModule.getInstance().getRGB(145)
                  );
                  landing = true;
               }
            }

            Vec3 pos = new Vec3(x, y, z);
            BlockHitResult result = mc.level.clip(new ClipContext(lastPos, pos, Block.COLLIDER, Fluid.NONE, mc.player));
            if (result != null && result.getType() == Type.BLOCK) {
               landing = true;
               Direction side = result.getDirection();
               if (side == Direction.NORTH || side == Direction.SOUTH) {
                  RenderManager.renderSide(event.getMatrices(), x - 0.6F, y - 0.6F, z, x + 0.6F, y + 0.6F, z, side, ColorsModule.getInstance().getRGB(80));
               } else if (side != Direction.WEST && side != Direction.EAST) {
                  RenderManager.renderSide(event.getMatrices(), x - 0.6F, y, z - 0.6F, x + 0.6F, y, z + 0.6F, side, ColorsModule.getInstance().getRGB(80));
               } else {
                  RenderManager.renderSide(event.getMatrices(), x, y - 0.6F, z - 0.6F, x, y + 0.6F, z + 0.6F, side, ColorsModule.getInstance().getRGB(80));
               }
            }

            if (y < mc.level.getMinBuildHeight()) {
               break;
            }

            if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
               RenderManager.renderLine(event.getMatrices(), lastPos, pos, 1.5F, ColorsModule.getInstance().getRGB());
               if (landing) {
                  hasLanded = true;
               }
            }
         }
      }

      RenderBuffers.postRender();
      RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
   }

   private float getProjectilePower(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.4F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.5F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private boolean isThrowableItem(Item item) {
      return item instanceof EnderpearlItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private boolean isThrowableEntity(Entity entity) {
      return entity instanceof ThrownExperienceBottle
         || entity instanceof ThrownEnderpearl
         || entity instanceof ExperienceOrb
         || entity instanceof ThrownPotion
         || entity instanceof Arrow
         || entity instanceof ThrownTrident
         || entity instanceof Snowball
         || entity instanceof ThrownEgg;
   }
}
