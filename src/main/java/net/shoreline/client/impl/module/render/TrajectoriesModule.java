package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.util.Mth;
import net.minecraft.util.math.*;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Matrix4f;

/**
 * @author linus
 * @since 1.0
 */
public class TrajectoriesModule extends ToggleModule
{
    public TrajectoriesModule()
    {
        super("Trajectories", "Renders the trajectory path of projectiles", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        ItemStack mainhand = mc.player.getMainHandItem();
        Item throwItem = mainhand.getItem();
        PoseStack matrixStack = new PoseStack();
        double d = mc.options.fov().get();
        matrixStack.mulPose(mc.gameRenderer.getProjectionMatrix(d));
        Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrixStack.last().pose(), VertexSorting.DISTANCE_TO_ORIGIN);
        RenderBuffers.preRender();
        if (((throwItem instanceof BowItem || throwItem instanceof CrossbowItem || throwItem instanceof TridentItem) && mc.player.getTicksUsingItem() > 0) || isThrowableItem(throwItem))
        {
            float yaw = mc.player.getYRot();
            double x = Interpolation.interpolateDouble(mc.player.xo, mc.player.getX(), mc.getTimer().getGameTimeDeltaPartialTick(true));
            double y = Interpolation.interpolateDouble(mc.player.yo, mc.player.getY(), mc.getTimer().getGameTimeDeltaPartialTick(true));
            double z = Interpolation.interpolateDouble(mc.player.zo, mc.player.getZ(), mc.getTimer().getGameTimeDeltaPartialTick(true));
            y = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;

            if (throwItem == mc.player.getMainHandItem().getItem())
            {
                x = x - Mth.cos(yaw / 180.0f * Mth.PI) * 0.16f;
                z = z - Mth.sin(yaw / 180.0f * Mth.PI) * 0.16f;
            }
            else
            {
                x = x + Mth.cos(yaw / 180.0f * Mth.PI) * 0.16f;
                z = z + Mth.sin(yaw / 180.0f * Mth.PI) * 0.16f;
            }
            final float n = throwItem instanceof BowItem ? 1.0f : 0.4f;
            double motionX = -Mth.sin(yaw / 180.0f * Mth.PI) * Mth.cos(mc.player.getXRot() / 180.0f * Mth.PI) * n;
            double motionY = -Mth.sin((mc.player.getXRot() + (throwItem instanceof SplashPotionItem || throwItem instanceof LingeringPotionItem || throwItem instanceof ExperienceBottleItem ? -20 : 0)) / 180.0f * Mth.PI) * n;
            double motionZ = Mth.cos(yaw / 180.0f * Mth.PI) * Mth.cos(mc.player.getXRot() / 180.0f * Mth.PI) * n;
            float power = mc.player.getTicksUsingItem() / 20.0f;
            power = (power * power + power * 2.0f) / 3.0f;
            if (power > 1.0f)
            {
                power = 1.0f;
            }
            final float dist = Mth.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
            motionX /= dist;
            motionY /= dist;
            motionZ /= dist;
            final float pow = (throwItem instanceof BowItem ? (power * 2.0f) : throwItem instanceof CrossbowItem ? 2.2f : 1.0f) * getProjectilePower(throwItem);
            motionX *= pow;
            motionY *= pow;
            motionZ *= pow;
            if (!mc.player.onGround())
            {
                motionY += mc.player.getDeltaMovement().y();
            }

            float size = throwItem instanceof BowItem || throwItem instanceof CrossbowItem ? 0.3f : 0.25f;
            Vec3 lastPos;
            boolean hasLanded = false;
            while (!hasLanded)
            {
                boolean landing = false;
                lastPos = new Vec3(x, y, z);
                x += motionX;
                y += motionY;
                z += motionZ;
                BlockPos blockPos = BlockPos.containing(x, y, z);
                if (mc.level.getBlockState(blockPos).getBlock() == Blocks.WATER)
                {
                    motionX *= 0.8;
                    motionY *= 0.8;
                    motionZ *= 0.8;
                }
                else
                {
                    motionX *= 0.99;
                    motionY *= 0.99;
                    motionZ *= 0.99;
                }

                if (throwItem instanceof BowItem)
                {
                    motionY -= 0.05000000074505806;
                }
                else if (mc.player.getMainHandItem().getItem() instanceof CrossbowItem)
                {
                    motionY -= 0.05000000074505806;
                }
                else
                {
                    motionY -= 0.03f;
                }

                AABB projectileBox = new AABB(x - size, y - size, z - size, x + size, y + size, z + size);
                for (Entity entity : mc.level.getEntities(null, projectileBox))
                {
                    if (entity == mc.player || isThrowableEntity(entity))
                    {
                        continue;
                    }
                    RenderManager.renderBox(event.getMatrices(),
                            Interpolation.getInterpolatedEntityBox(entity), ColorsModule.getInstance().getRGB(40));
                    RenderManager.renderBoundingBox(event.getMatrices(),
                            Interpolation.getInterpolatedEntityBox(entity), 1.5f, ColorsModule.getInstance().getRGB(145));
                    landing = true;
                }

                Vec3 pos = new Vec3(x, y, z);
                BlockHitResult result = mc.level.clip(new ClipContext(lastPos, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                if (result != null && result.getType() == HitResult.Type.BLOCK)
                {
                    landing = true;
                    Direction side = result.getDirection();
                    if (side == Direction.NORTH || side == Direction.SOUTH)
                    {
                        RenderManager.renderSide(event.getMatrices(), x - 0.6f, y - 0.6f, z, x + 0.6f, y + 0.6f, z, side, ColorsModule.getInstance().getRGB(80));
                    }
                    else if (side == Direction.WEST || side == Direction.EAST)
                    {
                        RenderManager.renderSide(event.getMatrices(), x, y - 0.6f, z - 0.6f, x, y + 0.6f, z + 0.6f, side, ColorsModule.getInstance().getRGB(80));
                    }
                    else
                    {
                        RenderManager.renderSide(event.getMatrices(), x - 0.6f, y, z - 0.6f, x + 0.6f, y, z + 0.6f, side, ColorsModule.getInstance().getRGB(80));
                    }
                }

                if (y < mc.level.getMinBuildHeight())
                {
                    break;
                }

                if (motionX == 0.0 && motionY == 0.0 && motionZ == 0.0)
                {
                    continue;
                }

                RenderManager.renderLine(event.getMatrices(), lastPos, pos, 1.5f, ColorsModule.getInstance().getRGB());
                if (landing)
                {
                    hasLanded = true;
                }
            }
        }
        RenderBuffers.postRender();
        RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
    }

    private float getProjectilePower(Item item)
    {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem)
        {
            return 0.4f;
        }

        if (item instanceof ExperienceBottleItem)
        {
            return 0.5f;
        }

        if (item instanceof TridentItem)
        {
            return 2.0f;
        }

        return 1.5f;
    }

    private boolean isThrowableItem(Item item)
    {
        return item instanceof EnderpearlItem || item instanceof ExperienceBottleItem
                || item instanceof SnowballItem || item instanceof EggItem
                || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
    }

    private boolean isThrowableEntity(Entity entity)
    {
        return entity instanceof ThrownExperienceBottle || entity instanceof ThrownEnderpearl || entity instanceof ExperienceOrb || entity instanceof ThrownPotion || entity instanceof Arrow
                || entity instanceof ThrownTrident || entity instanceof Snowball || entity instanceof ThrownEgg;
    }
}
