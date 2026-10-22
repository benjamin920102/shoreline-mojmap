package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.shoreline.client.impl.event.render.entity.RenderCrystalEvent;
import net.shoreline.eventbus.EventBus;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public class MixinEndCrystalEntityRenderer
{
    //
    @Shadow
    @Final
    private ModelPart cube;
    //
    @Shadow
    @Final
    private ModelPart glass;

    @Shadow
    @Final
    private static RenderType RENDER_TYPE;

    @Shadow
    @Final
    private ModelPart base;

    @Shadow
    @Final
    private static float SIN_45;

    /**
     * @param endCrystalEntity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     * @param ci
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookRender(EndCrystal endCrystalEntity, float f, float g,
                            PoseStack matrixStack, MultiBufferSource vertexConsumerProvider,
                            int i, CallbackInfo ci)
    {
        RenderCrystalEvent renderCrystalEvent = new RenderCrystalEvent(endCrystalEntity,
                f, g, matrixStack, i, cube, glass);
        // Does it matter if render comes before cancelling?
        EventBus.INSTANCE.dispatch(renderCrystalEvent);
        ci.cancel();
        if (!renderCrystalEvent.isCanceled())
        {
            matrixStack.pushPose();
            float h = !renderCrystalEvent.getBounce() ? -1.0f : EndCrystalRenderer.getY(endCrystalEntity, g);
            float j = (float) ((endCrystalEntity.time + g) * renderCrystalEvent.getSpin()) * 3.0f;
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RENDER_TYPE);
            matrixStack.pushPose();
            matrixStack.scale(renderCrystalEvent.getScale(), renderCrystalEvent.getScale(), renderCrystalEvent.getScale());
            matrixStack.scale(2.0f, 2.0f, 2.0f);
            matrixStack.translate(0.0f, -0.5f, 0.0f);
            int k = OverlayTexture.NO_OVERLAY;
            if (endCrystalEntity.showsBottom()) {
                this.base.render(matrixStack, vertexConsumer, i, k);
            }
            matrixStack.mulPose(Axis.YP.rotationDegrees(j));
            matrixStack.translate(0.0f, 1.5f + h / 2.0f, 0.0f);
            matrixStack.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SIN_45, 0.0f, SIN_45));
            this.glass.render(matrixStack, vertexConsumer, i, k);
            float l = 0.875f;
            matrixStack.scale(0.875f, 0.875f, 0.875f);
            matrixStack.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SIN_45, 0.0f, SIN_45));
            matrixStack.mulPose(Axis.YP.rotationDegrees(j));
            this.glass.render(matrixStack, vertexConsumer, i, k);
            matrixStack.scale(0.875f, 0.875f, 0.875f);
            matrixStack.mulPose(new Quaternionf().setAngleAxis(1.0471976f, SIN_45, 0.0f, SIN_45));
            matrixStack.mulPose(Axis.YP.rotationDegrees(j));
            this.cube.render(matrixStack, vertexConsumer, i, k);
            matrixStack.scale(1.0f / renderCrystalEvent.getScale(), 1.0f / renderCrystalEvent.getScale(), 1.0f / renderCrystalEvent.getScale());
            matrixStack.popPose();
            matrixStack.popPose();
            BlockPos blockPos = endCrystalEntity.getBeamTarget();
            if (blockPos != null) {
                float m = (float)blockPos.getX() + 0.5f;
                float n = (float)blockPos.getY() + 0.5f;
                float o = (float)blockPos.getZ() + 0.5f;
                float p = (float)((double)m - endCrystalEntity.getX());
                float q = (float)((double)n - endCrystalEntity.getY());
                float r = (float)((double)o - endCrystalEntity.getZ());
                matrixStack.translate(p, q, r);
                EnderDragonRenderer.renderCrystalBeams(-p, -q + h, -r, g, endCrystalEntity.time, matrixStack, vertexConsumerProvider, i);
            }
        }
    }
}
