package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Quaternionf;

import java.awt.*;

/**
 * @author linus
 * @since 1.0
 */
public class SkeletonModule extends ToggleModule
{
    Config<Float> widthConfig = register(new NumberConfig<>("Width", "The line width of the skeleton", 1.0f, 1.0f, 5.0f));

    public SkeletonModule()
    {
        super("Skeleton", "Renders a skeleton to show player limbs", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent.Game event)
    {
        PoseStack matrixStack = event.getMatrices();
        float g = event.getTickDelta();
        RenderBuffers.preRender();
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (entity == null || !entity.isAlive())
            {
                continue;
            }
            if (entity instanceof Player playerEntity)
            {
                if (mc.options.getCameraType().isFirstPerson() && playerEntity == mc.player)
                {
                    continue;
                }
                Vec3 skeletonPos = Interpolation.getInterpolatedPosition(entity, g);
                PlayerRenderer livingEntityRenderer =
                        (PlayerRenderer) (LivingEntityRenderer<?, ?>) mc.getEntityRenderDispatcher().getRenderer(playerEntity);
                PlayerModel<Player> playerEntityModel =
                        (PlayerModel) livingEntityRenderer.getModel();
                boolean rotating = Managers.ROTATION.isRotating();
                float h = Mth.rotLerp(g,
                        rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.yBodyRotO, rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.yBodyRot);
                float j = Mth.rotLerp(g,
                        rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.yHeadRotO, rotating ? Managers.ROTATION.getRotationYaw() : playerEntity.yHeadRot);
                float q = playerEntity.walkAnimation.position() - playerEntity.walkAnimation.speed() * (1.0f - g);
                float p = playerEntity.walkAnimation.speed(g);
                float o = (float) playerEntity.tickCount + g;
                float k = j - h;
                float m = rotating ? Managers.ROTATION.getRotationPitch() : playerEntity.getViewXRot(g);
                playerEntityModel.prepareMobModel(playerEntity, q, p, g);
                playerEntityModel.setupAnim(playerEntity, q, p, o, k, m);
                boolean swimming = playerEntity.isVisuallySwimming();
                boolean sneaking = playerEntity.isShiftKeyDown();
                boolean flying = playerEntity.isFallFlying();
                ModelPart head = playerEntityModel.head;
                ModelPart leftArm = playerEntityModel.leftArm;
                ModelPart rightArm = playerEntityModel.rightArm;
                ModelPart leftLeg = playerEntityModel.leftLeg;
                ModelPart rightLeg = playerEntityModel.rightLeg;
                matrixStack.pushPose();
                matrixStack.translate(skeletonPos.x, skeletonPos.y, skeletonPos.z);
                if (swimming)
                {
                    matrixStack.translate(0, 0.35f, 0);
                }
                matrixStack.mulPose(new Quaternionf().setAngleAxis((h + 180.0f) * Math.PI / 180.0f, 0, -1, 0));
                if (swimming || flying)
                {
                    matrixStack.mulPose(new Quaternionf().setAngleAxis((90.0f + m) * Math.PI / 180.0f, -1, 0, 0));
                }
                if (swimming)
                {
                    matrixStack.translate(0, -0.95f, 0);
                }
                RenderBuffers.LINES.begin(matrixStack);
                RenderSystem.lineWidth(widthConfig.getValue());
                Color skeletonColor = ColorsModule.getInstance().getColor();
                if (Managers.SOCIAL.isFriend(playerEntity.getName()))
                {
                    skeletonColor = SocialsModule.getInstance().getFriendColor();
                }
                RenderBuffers.LINES.color(skeletonColor.getRGB());
                RenderBuffers.LINES.vertexLine(0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0, 0, sneaking ? 1.05f : 1.4f, 0);
                RenderBuffers.LINES.vertexLine(-0.37f, sneaking ? 1.05f : 1.35f, 0, 0.37f, sneaking ? 1.05f : 1.35f, 0);
                RenderBuffers.LINES.vertexLine(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                RenderBuffers.LINES.end();
                matrixStack.pushPose();
                matrixStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
                rotateSkeleton(matrixStack, head);
                RenderBuffers.LINES.updateMatrices(matrixStack);
                RenderBuffers.LINES.vertexLine(0, 0, 0, 0, 0.25f, 0);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotateSkeleton(matrixStack, rightLeg);
                RenderBuffers.LINES.updateMatrices(matrixStack);
                RenderBuffers.LINES.vertexLine(0, 0, 0, 0, -0.6f, 0);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
                rotateSkeleton(matrixStack, leftLeg);
                RenderBuffers.LINES.updateMatrices(matrixStack);
                RenderBuffers.LINES.vertexLine(0, 0, 0, 0, -0.6f, 0);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotateSkeleton(matrixStack, rightArm);
                RenderBuffers.LINES.updateMatrices(matrixStack);
                RenderBuffers.LINES.vertexLine(0, 0, 0, 0, -0.55f, 0);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
                rotateSkeleton(matrixStack, leftArm);
                RenderBuffers.LINES.updateMatrices(matrixStack);
                RenderBuffers.LINES.vertexLine(0, 0, 0, 0, -0.55f, 0);
                matrixStack.popPose();
                matrixStack.popPose();
            }
        }
        RenderBuffers.postRender();
        RenderBuffers.LINES.end();
    }

    private void rotateSkeleton(PoseStack matrix, ModelPart modelPart)
    {
        if (modelPart.zRot != 0.0f)
        {
            matrix.mulPose(Axis.ZP.rotation(modelPart.zRot));
        }
        if (modelPart.yRot != 0.0f)
        {
            matrix.mulPose(Axis.YN.rotation(modelPart.yRot));
        }
        if (modelPart.xRot != 0.0f)
        {
            matrix.mulPose(Axis.XN.rotation(modelPart.xRot));
        }
    }
}

