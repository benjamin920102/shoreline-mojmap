package net.shoreline.client.api.render.chams;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.RabbitModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.model.StaticBipedEntityModel;
import net.shoreline.client.impl.module.misc.SwingModule;
import net.shoreline.client.impl.module.render.CrystalModelModule;
import net.shoreline.client.impl.module.render.FreecamModule;
import net.shoreline.client.init.Fonts;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorAnimalModel;
import net.shoreline.client.util.Globals;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import static net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ITEM;

public class ChamsModelRenderer implements Globals
{
    private static final float SINE_45_DEGREES = (float) Math.sin(0.7853981633974483);

    private static final PoseStack matrices = new PoseStack();
    private static final Vector4f pos1 = new Vector4f();
    private static final Vector4f pos2 = new Vector4f();
    private static final Vector4f pos3 = new Vector4f();
    private static final Vector4f pos4 = new Vector4f();

    public static void renderStaticPlayerModel(PoseStack matrixStack, AbstractClientPlayer entity, StaticBipedEntityModel playerModel, float tickDelta, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        double offsetX = playerModel.getX();
        double offsetY = playerModel.getY();
        double offsetZ = playerModel.getZ();
        matrices.pushPose();
        float animationProgress;
        EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (entityRenderer instanceof PlayerRenderer renderer)
        {
            animationProgress = renderer.getBob(entity, tickDelta);
            setupPlayerTransforms(entity, matrices, animationProgress, entity.getVisualRotationYInDegrees(), tickDelta);
            matrices.scale(-1, -1, 1);
            renderer.scale(entity, matrices, tickDelta);
            matrices.translate(0, -1.5010000467300415, 0);

            render(matrixStack, playerModel.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            render(matrixStack, playerModel.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            render(matrixStack, playerModel.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            render(matrixStack, playerModel.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            render(matrixStack, playerModel.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            render(matrixStack, playerModel.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
        }
        matrices.popPose();
    }

    public static void setupPlayerTransforms(AbstractClientPlayer abstractClientPlayerEntity, PoseStack matrixStack, float f, float g, float h)
    {
        float i = abstractClientPlayerEntity.getSwimAmount(h);
        float j = abstractClientPlayerEntity.getViewXRot(h);
        if (abstractClientPlayerEntity.isFallFlying())
        {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
            float k = (float) abstractClientPlayerEntity.getFallFlyingTicks() + h;
            float l = Mth.clamp(k * k / 100.0f, 0.0f, 1.0f);
            if (!abstractClientPlayerEntity.isAutoSpinAttack())
            {
                matrixStack.mulPose(Axis.XP.rotationDegrees(l * (-90.0f - j)));
            }
            Vec3 vec3d = abstractClientPlayerEntity.getViewVector(h);
            Vec3 vec3d2 = abstractClientPlayerEntity.getDeltaMovementLerped(h);
            double d = vec3d2.horizontalDistanceSqr();
            double e = vec3d.horizontalDistanceSqr();
            if (d > 0.0 && e > 0.0)
            {
                double m = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double n = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrixStack.mulPose(Axis.YP.rotation((float) (Math.signum(n) * Math.acos(m))));
            }
        }
        else if (i > 0.0f)
        {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
            float k = abstractClientPlayerEntity.isInWater() ? -90.0f - j : -90.0f;
            float l = Mth.lerp(i, 0.0f, k);
            matrixStack.mulPose(Axis.XP.rotationDegrees(l));
            if (abstractClientPlayerEntity.isVisuallySwimming())
            {
                matrixStack.translate(0.0f, -1.0f, 0.3f);
            }
        }
        else
        {
            setupTransforms(abstractClientPlayerEntity, matrixStack, f, g, h);
        }
    }

    public static void setupTransforms(LivingEntity entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta)
    {
        if (entity.isFullyFrozen())
        {
            bodyYaw += (float) (Math.cos((double) entity.tickCount * 3.25) * Math.PI * (double) 0.4f);
        }
        if (!entity.hasPose(Pose.SLEEPING))
        {
            matrices.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        }
        if (entity.deathTime > 0)
        {
            float f = ((float) entity.deathTime + tickDelta - 1.0f) / 20.0f * 1.6f;
            if ((f = Mth.sqrt(f)) > 1.0f)
            {
                f = 1.0f;
            }
            matrices.mulPose(Axis.ZP.rotationDegrees(f * 90.0f));
        }
        else if (entity.isAutoSpinAttack())
        {
            matrices.mulPose(Axis.XP.rotationDegrees(-90.0f - entity.getXRot()));
            matrices.mulPose(Axis.YP.rotationDegrees(((float) entity.tickCount + tickDelta) * -75.0f));
        }
        else if (entity.hasPose(Pose.SLEEPING))
        {
            Direction direction = entity.getBedOrientation();
            float g = direction != null ? getYaw(direction) : bodyYaw;
            matrices.mulPose(Axis.YP.rotationDegrees(g));
            matrices.mulPose(Axis.ZP.rotationDegrees(90.0f));
            matrices.mulPose(Axis.YP.rotationDegrees(270.0f));
        }
        else if (LivingEntityRenderer.isEntityUpsideDown(entity))
        {
            matrices.translate(0.0f, entity.getBbHeight() + 0.1f, 0.0f);
            matrices.mulPose(Axis.ZP.rotationDegrees(180.0f));
        }
    }

    private static float getYaw(Direction direction)
    {
        return switch (direction)
        {
            case SOUTH -> 90.0f;
            case WEST -> 0.0f;
            case NORTH -> 270.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    public static void render(PoseStack matrixStack, Entity entity, float tickDelta, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        double offsetX = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        double offsetY = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double offsetZ = Mth.lerp(tickDelta, entity.zOld, entity.getZ());

        matrices.pushPose();
        EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);

        if (entityRenderer instanceof LivingEntityRenderer renderer)
        {
            LivingEntity livingEntity = (LivingEntity) entity;
            EntityModel<LivingEntity> model = renderer.getModel();

            if (entityRenderer instanceof PlayerRenderer r)
            {
                PlayerModel<AbstractClientPlayer> playerModel = r.getModel();

                playerModel.crouching = entity.isCrouching();
                HumanoidModel.ArmPose armPose = PlayerRenderer.getArmPose((AbstractClientPlayer) entity, InteractionHand.MAIN_HAND);
                HumanoidModel.ArmPose armPose2 = PlayerRenderer.getArmPose((AbstractClientPlayer) entity, InteractionHand.OFF_HAND);

                if (armPose.isTwoHanded())
                {
                    armPose2 = livingEntity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
                }

                if (livingEntity.getMainArm() == HumanoidArm.RIGHT)
                {
                    playerModel.rightArmPose = armPose;
                    playerModel.leftArmPose = armPose2;
                }
                else
                {
                    playerModel.rightArmPose = armPose2;
                    playerModel.leftArmPose = armPose;
                }
            }

            model.attackTime = livingEntity.getAttackAnim(tickDelta);
            model.riding = livingEntity.isPassenger();
            model.young = livingEntity.isBaby();

            boolean rotating = entity == mc.player && Managers.ROTATION.isRotating();
            float bodyYaw = rotating ? Managers.ROTATION.getRotationYaw() : Mth.rotLerp(tickDelta, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            float headYaw = rotating ? Managers.ROTATION.getRotationYaw() : Mth.rotLerp(tickDelta, livingEntity.yHeadRotO, livingEntity.yHeadRot);
            float yaw = headYaw - bodyYaw;

            float animationProgress;
            if (livingEntity.isPassenger() && livingEntity.getVehicle() instanceof LivingEntity livingEntity2)
            {
                bodyYaw = Mth.rotLerp(tickDelta, livingEntity2.yBodyRotO, livingEntity2.yBodyRot);
                yaw = headYaw - bodyYaw;
                animationProgress = Mth.wrapDegrees(yaw);

                if (animationProgress < -85)
                {
                    animationProgress = -85;
                }
                if (animationProgress >= 85)
                {
                    animationProgress = 85;
                }

                bodyYaw = headYaw - animationProgress;
                if (animationProgress * animationProgress > 2500)
                {
                    bodyYaw += animationProgress * 0.2;
                }

                yaw = headYaw - bodyYaw;
            }

            float pitch = rotating ? Managers.ROTATION.getRotationPitch() : Mth.lerp(tickDelta, livingEntity.xRotO, livingEntity.getXRot());

            animationProgress = renderer.getBob(livingEntity, tickDelta);
            float limbDistance = 0;
            float limbAngle = 0;

            if (!livingEntity.isPassenger() && livingEntity.isAlive())
            {
                limbDistance = livingEntity.walkAnimation.speed(tickDelta);
                limbAngle = livingEntity.walkAnimation.position(tickDelta);

                if (livingEntity.isBaby())
                {
                    limbAngle *= 3;
                }
                if (limbDistance > 1)
                {
                    limbDistance = 1;
                }
            }

            model.prepareMobModel(livingEntity, limbAngle, limbDistance, tickDelta);
            model.setupAnim(livingEntity, limbAngle, limbDistance, animationProgress, yaw, pitch);

            renderer.setupRotations(livingEntity, matrices, animationProgress, bodyYaw, tickDelta, 1.0f);
            matrices.scale(-1, -1, 1);
            renderer.scale(livingEntity, matrices, tickDelta);
            matrices.translate(0, -1.5010000467300415, 0);

            if (model instanceof AgeableListModel m)
            {
                if (m.young)
                {
                    matrices.pushPose();
                    float g;
                    if (((AccessorAnimalModel) m).hookGetHeadScaled())
                    {
                        g = 1.5F / ((AccessorAnimalModel) m).hookGetInvertedChildHeadScale();
                        matrices.scale(g, g, g);
                    }

                    matrices.translate(0.0D, ((AccessorAnimalModel) m).hookGetChildHeadYOffset() / 16.0f, ((AccessorAnimalModel) m).hookGetChildHeadZOffset() / 16.0f);
                    if (model instanceof HumanoidModel mo)
                    {
                        render(matrixStack, mo.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                    }
                    else
                    {
                        ((AccessorAnimalModel) m).hookGetHeadParts().forEach(modelPart -> render(matrixStack, (ModelPart) modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
                    }
                    matrices.popPose();
                    matrices.pushPose();
                    g = 1.0f / ((AccessorAnimalModel) m).hookGetInvertedChildBodyScale();
                    matrices.scale(g, g, g);
                    matrices.translate(0.0D, ((AccessorAnimalModel) m).hookGetChildBodyYOffset() / 16.0f, 0.0D);
                    if (model instanceof HumanoidModel mo)
                    {
                        render(matrixStack, mo.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                    }
                    else
                    {
                        ((AccessorAnimalModel) m).hookGetBodyParts().forEach(modelPart -> render(matrixStack, (ModelPart) modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
                    }
                    matrices.popPose();
                }
                else
                {
                    if (model instanceof HumanoidModel mo)
                    {
                        render(matrixStack, mo.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.leftLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, mo.rightLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                    }
                    else
                    {
                        ((AccessorAnimalModel) m).hookGetHeadParts().forEach(modelPart -> render(matrixStack, (ModelPart) modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
                        ((AccessorAnimalModel) m).hookGetBodyParts().forEach(modelPart -> render(matrixStack, (ModelPart) modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
                    }
                }
            }
            else
            {
                if (model instanceof HierarchicalModel m)
                {
                    render(matrixStack, m.root(), offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                }
                else if (model instanceof ListModel m)
                {
                    m.parts().forEach(modelPart -> render(matrixStack, (ModelPart) modelPart, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine));
                }
                else if (model instanceof LlamaModel m)
                {
                    if (m.young)
                    {
                        matrices.pushPose();
                        matrices.scale(0.71428573F, 0.64935064F, 0.7936508F);
                        matrices.translate(0.0D, 1.3125D, 0.2199999988079071D);
                        render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                        matrices.pushPose();
                        matrices.scale(0.625F, 0.45454544F, 0.45454544F);
                        matrices.translate(0.0D, 2.0625D, 0.0D);
                        render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                        matrices.pushPose();
                        matrices.scale(0.45454544F, 0.41322312F, 0.45454544F);
                        matrices.translate(0.0D, 2.0625D, 0.0D);
                        render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                    }
                    else
                    {
                        render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftHindLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftChest, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                    }
                }
                else if (model instanceof RabbitModel m)
                {
                    if (m.young)
                    {
                        matrices.pushPose();
                        matrices.scale(0.56666666F, 0.56666666F, 0.56666666F);
                        matrices.translate(0.0D, 1.375D, 0.125D);
                        render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.nose, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                        matrices.pushPose();
                        matrices.scale(0.4F, 0.4F, 0.4F);
                        matrices.translate(0.0D, 2.25D, 0.0D);
                        render(matrixStack, m.leftRearFoot, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightRearFoot, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.tail, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                    }
                    else
                    {
                        matrices.pushPose();
                        matrices.scale(0.6F, 0.6F, 0.6F);
                        matrices.translate(0.0D, 1.0D, 0.0D);
                        render(matrixStack, m.leftRearFoot, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightRearFoot, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightHaunch, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.body, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightFrontLeg, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.head, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.rightEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.leftEar, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.tail, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        render(matrixStack, m.nose, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
                        matrices.popPose();
                    }
                }
            }
        }

        if (entityRenderer instanceof EndCrystalRenderer renderer)
        {
            EndCrystal crystalEntity = (EndCrystal) entity;

            matrices.pushPose();
            float h = CrystalModelModule.getInstance().isEnabled() && !CrystalModelModule.getInstance().getBounce() ? -1.0f : EndCrystalRenderer.getY(crystalEntity, tickDelta);
            float j = (float) ((crystalEntity.time + tickDelta) * (CrystalModelModule.getInstance().isEnabled() ? CrystalModelModule.getInstance().getSpin() : 1.0f)) * 3.0f;
            matrices.pushPose();
            if (CrystalModelModule.getInstance().isEnabled())
            {
                float crystalScale = CrystalModelModule.getInstance().getScale();
                matrices.scale(crystalScale, crystalScale, crystalScale);
            }
            matrices.scale(2.0f, 2.0f, 2.0f);
            matrices.translate(0.0f, -0.5f, 0.0f);
            matrices.mulPose(Axis.YP.rotationDegrees(j));
            matrices.translate(0.0D, 1.5F + h / 2.0f, 0.0D);
            matrices.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
            render(matrixStack, renderer.glass, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            matrices.scale(0.875F, 0.875F, 0.875F);
            matrices.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
            matrices.mulPose(Axis.YP.rotationDegrees(j));
            render(matrixStack, renderer.glass, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            matrices.scale(0.875F, 0.875F, 0.875F);
            matrices.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SINE_45_DEGREES, 0.0f, SINE_45_DEGREES));
            matrices.mulPose(Axis.YP.rotationDegrees(j));
            // render(matrixStack, renderer.core, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            matrices.popPose();
            matrices.popPose();
        }

//        if (!NametagsModule.getInstance().isEnabled() && (entity.shouldRenderName() || entity.hasCustomName() && entity == mc.getEntityRenderDispatcher().targetedEntity))
//        {
//            renderLabel(entity.getDisplayName(), matrices, entity, tickDelta);
//        }

        matrices.popPose();
    }

    public static void renderHand(PoseStack matrixStack, float tickDelta, int lineColor, int color,
                                  float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        if (!mc.options.getCameraType().isFirstPerson() || FreecamModule.getInstance().isEnabled())
        {
            return;
        }
        double d = mc.options.fov().get();
        matrixStack.mulPose(mc.gameRenderer.getProjectionMatrix(d));
        // mc.gameRenderer.loadProjectionMatrix(mc.gameRenderer.getBasicProjectionMatrix(d));
        matrixStack.setIdentity();
        // Bob view
        if (mc.options.bobView().get())
        {
            Player playerEntity = (Player) mc.getCameraEntity();
            float f = playerEntity.walkDist - playerEntity.walkDistO;
            float g = -(playerEntity.walkDist + f * tickDelta);
            float h = Mth.lerp(tickDelta, playerEntity.oBob, playerEntity.bob);
            matrixStack.translate(Mth.sin(g * (float) Math.PI) * h * 0.5f, -Math.abs(Mth.cos(g * (float) Math.PI) * h), 0.0f);
            matrixStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(g * (float) Math.PI) * h * 3.0f));
            matrixStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(g * (float) Math.PI - 0.2f) * h) * 5.0f));
        }
        //
        float h1 = Mth.lerp(tickDelta, mc.player.xBobO, mc.player.xBob);
        float i1 = Mth.lerp(tickDelta, mc.player.yBobO, mc.player.yBob);
        matrixStack.mulPose(Axis.XP.rotationDegrees((mc.player.getViewXRot(tickDelta) - h1) * 0.1f));
        matrixStack.mulPose(Axis.YP.rotationDegrees((mc.player.getViewYRot(tickDelta) - i1) * 0.1f));
        float f1 = mc.player.getAttackAnim(tickDelta);
        InteractionHand hand = SwingModule.getInstance().isEnabled() ? SwingModule.getInstance().getPrevPreferredHand() : MoreObjects.firstNonNull(mc.player.swingingArm, InteractionHand.MAIN_HAND);
        boolean bl2;
        ItemStack itemStack = mc.player.getMainHandItem();
        ItemStack itemStack2 = mc.player.getOffhandItem();
        boolean bl = itemStack.is(Items.BOW) || itemStack2.is(Items.BOW);
        bl2 = itemStack.is(Items.CROSSBOW) || itemStack2.is(Items.CROSSBOW);
        HandRenderType handRenderType = HandRenderType.RENDER_BOTH_HANDS;
        if (!bl && !bl2)
        {
            handRenderType = HandRenderType.RENDER_BOTH_HANDS;
        }
        else if (mc.player.isUsingItem())
        {
            ItemStack itemStack1 = mc.player.getUseItem();
            InteractionHand hand1 = mc.player.getUsedItemHand();
            if (itemStack1.is(Items.BOW) || itemStack1.is(Items.CROSSBOW))
            {
                handRenderType = HandRenderType.shouldOnlyRender(hand1);
            }
            else
            {
                handRenderType = hand == InteractionHand.MAIN_HAND && mc.player.getOffhandItem().is(Items.CROSSBOW) && CrossbowItem.isCharged(mc.player.getOffhandItem()) ? HandRenderType.RENDER_MAIN_HAND_ONLY : HandRenderType.RENDER_BOTH_HANDS;
            }
        }
        else if (itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack))
        {
            handRenderType = HandRenderType.RENDER_MAIN_HAND_ONLY;
        }
        PlayerRenderer playerEntityRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        float k;
        float j;
        if (handRenderType.renderMainHand)
        {
            boolean bl1 = hand == InteractionHand.MAIN_HAND;
            boolean bl3 = mc.player.swingingArm == InteractionHand.MAIN_HAND;
            j = bl3 ? f1 : 0.0f;
            k = 1.0f - Mth.lerp(tickDelta, mc.gameRenderer.itemInHandRenderer.oMainHandHeight, mc.gameRenderer.itemInHandRenderer.mainHandHeight);
            HumanoidArm arm = bl1 ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
            if (itemStack.isEmpty() && bl1 && !mc.player.isInvisible())
            {
                renderFirstPersonItem(matrixStack, tickDelta, playerEntityRenderer, arm, j, k, lineColor, color, lineWidth, lines, fill, shine);
            }
        }
        if (handRenderType.renderOffHand)
        {
            boolean bl1 = hand == InteractionHand.OFF_HAND;
            boolean bl3 = mc.player.swingingArm == InteractionHand.MAIN_HAND;
            j = bl3 ? f1 : 0.0f;
            k = 1.0f - Mth.lerp(tickDelta, mc.gameRenderer.itemInHandRenderer.oOffHandHeight, mc.gameRenderer.itemInHandRenderer.offHandHeight);
            HumanoidArm arm = bl1 ? mc.player.getMainArm().getOpposite() : mc.player.getMainArm();
            if (itemStack.isEmpty() && bl1 && !mc.player.isInvisible())
            {
                renderFirstPersonItem(matrixStack, tickDelta, playerEntityRenderer, arm, j, k, lineColor, color, lineWidth, lines, fill, shine);
            }
        }
    }
    
    public static void renderFirstPersonItem(PoseStack matrixStack, float tickDelta, PlayerRenderer playerEntityRenderer, HumanoidArm arm, float swingProgress,
                                             float equipProgress, int lineColor, int color, float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        RenderSystem.disableDepthTest();
        double offsetX = mc.gameRenderer.getMainCamera().getPosition().x;
        double offsetY = mc.gameRenderer.getMainCamera().getPosition().y;
        double offsetZ = mc.gameRenderer.getMainCamera().getPosition().z;
        matrices.pushPose();
        boolean bl = arm != HumanoidArm.LEFT;
        float f = bl ? 1.0f : -1.0f;
        float g = Mth.sqrt(swingProgress);
        float h = -0.3f * Mth.sin(g * (float) Math.PI);
        float i = 0.4f * Mth.sin(g * ((float) Math.PI * 2));
        float j = -0.4f * Mth.sin(swingProgress * (float) Math.PI);
        matrices.translate(f * (h + 0.64000005f), i + -0.6f + equipProgress * -0.6f, j + -0.71999997f);
        matrices.mulPose(Axis.YP.rotationDegrees(f * 45.0f));
        float k = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float l = Mth.sin(g * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees(f * l * 70.0f));
        matrices.mulPose(Axis.ZP.rotationDegrees(f * k * -20.0f));
        matrices.translate(f * -1.0f, 3.6f, 3.5f);
        matrices.mulPose(Axis.ZP.rotationDegrees(f * 120.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0f));
        matrices.mulPose(Axis.YP.rotationDegrees(f * -135.0f));
        matrices.translate(f * 5.6f, 0.0f, 0.0f);
        playerEntityRenderer.setModelProperties(mc.player);
        PlayerModel model = playerEntityRenderer.getModel();
        model.attackTime = 0.0f;
        model.crouching = false;
        model.swimAmount = 0.0f;
        model.setupAnim(mc.player, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        if (arm == HumanoidArm.RIGHT)
        {
            model.rightArm.xRot = 0.0f;
            render(matrixStack, model.rightArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            model.rightSleeve.xRot = 0.0f;
            // render(matrixStack, model.rightSleeve, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
        }
        else
        {
            model.leftArm.xRot = 0.0f;
            render(matrixStack, model.leftArm, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
            model.leftSleeve.xRot = 0.0f;
            // render(matrixStack, model.leftSleeve, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
        }
        matrices.popPose();
    }
    
    public static void render(PoseStack matrixStack, ModelPart part, double offsetX, double offsetY, double offsetZ, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        if (!part.visible || (part.cubes.isEmpty() && part.children.isEmpty()))
        {
            return;
        }

        matrices.pushPose();
        part.translateAndRotate(matrices);


        for (ModelPart.Cube cuboid : part.cubes)
        {
            renderModelPart(matrixStack, cuboid, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
        }
        for (ModelPart child : part.children.values())
        {
            render(matrixStack, child, offsetX, offsetY, offsetZ, color, lineColor, lineWidth, lines, fill, shine);
        }
        matrices.popPose();
    }

    private static void renderModelPart(PoseStack matrixStack, ModelPart.Cube cuboid, double offsetX, double offsetY, double offsetZ, int color, int lineColor, float lineWidth, boolean lines, boolean fill, boolean shine)
    {
        Matrix4f matrix = matrices.last().pose();

        for (ModelPart.Polygon quad : cuboid.polygons)
        {
            pos1.set(quad.vertices[0].pos.x / 16, quad.vertices[0].pos.y / 16, quad.vertices[0].pos.z / 16, 1);
            pos1.mul(matrix);
            pos2.set(quad.vertices[1].pos.x / 16, quad.vertices[1].pos.y / 16, quad.vertices[1].pos.z / 16, 1);
            pos2.mul(matrix);
            pos3.set(quad.vertices[2].pos.x / 16, quad.vertices[2].pos.y / 16, quad.vertices[2].pos.z / 16, 1);
            pos3.mul(matrix);
            pos4.set(quad.vertices[3].pos.x / 16, quad.vertices[3].pos.y / 16, quad.vertices[3].pos.z / 16, 1);
            pos4.mul(matrix);

            if (fill) 
            {
                if (shine) 
                {
                    RenderBuffers.TEXTURE_QUADS.begin(matrixStack);
                    RenderSystem.setShaderTexture(0, ENCHANTED_GLINT_ITEM);
                    RenderBuffers.TEXTURE_QUADS.color(color);
                    RenderBuffers.TEXTURE_QUADS.vertexTex(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, 0.0f, 0.0f)
                            .vertexTex(offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z, 0.0f, 1.0f)
                            .vertexTex(offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z, 1.0f, 1.0f)
                            .vertexTex(offsetX + pos4.x, offsetY + pos4.y, offsetZ + pos4.z, 1.0f, 0.0f);
                    RenderBuffers.TEXTURE_QUADS.end();
                }
                else 
                {
                    RenderBuffers.QUADS.begin(matrixStack);
                    RenderBuffers.QUADS.color(color);
                    RenderBuffers.QUADS.vertex(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z)
                            .vertex(offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z)
                            .vertex(offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z)
                            .vertex(offsetX + pos4.x, offsetY + pos4.y, offsetZ + pos4.z);
                    RenderBuffers.QUADS.end(); 
                }
            }

            if (lines)
            {
                RenderBuffers.LINES.begin(matrixStack);
                RenderSystem.lineWidth(lineWidth);
                RenderBuffers.LINES.color(lineColor);
                RenderBuffers.LINES.vertexLine(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z);
                RenderBuffers.LINES.vertexLine(offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z, offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z);
                RenderBuffers.LINES.vertexLine(offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z, offsetX + pos4.x, offsetY + pos4.y, offsetZ + pos4.z);
                RenderBuffers.LINES.vertexLine(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z);
                RenderBuffers.LINES.end();
            }
        }
    }

    private enum HandRenderType
    {
        RENDER_BOTH_HANDS(true, true),
        RENDER_MAIN_HAND_ONLY(true, false),
        RENDER_OFF_HAND_ONLY(false, true);

        final boolean renderMainHand;
        final boolean renderOffHand;

        HandRenderType(boolean renderMainHand, boolean renderOffHand)
        {
            this.renderMainHand = renderMainHand;
            this.renderOffHand = renderOffHand;
        }

        public static HandRenderType shouldOnlyRender(InteractionHand hand)
        {
            return hand == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
        }
    }
}
