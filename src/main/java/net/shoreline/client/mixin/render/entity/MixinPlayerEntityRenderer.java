package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.event.render.entity.RenderPlayerEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @see PlayerRenderer
 * @since 1.0
 */
@Mixin(PlayerRenderer.class)
public class MixinPlayerEntityRenderer
{
    //
    @Unique
    private float yaw, prevYaw, bodyYaw, prevBodyYaw, headYaw, prevHeadYaw;
    @Unique
    private float pitch, prevPitch;
    @Unique
    private boolean prevSneaking;
    @Unique
    private boolean prevFallFlying;

    /**
     * @param abstractClientPlayerEntity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     * @param ci
     */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/render /VertexConsumerProvider;I)V", at = @At(value = "HEAD"))
    private void onRenderHead(AbstractClientPlayer abstractClientPlayerEntity,
                              float f, float g, PoseStack matrixStack,
                              MultiBufferSource vertexConsumerProvider,
                              int i, CallbackInfo ci)
    {
        final RenderPlayerEvent renderPlayerEvent =
                new RenderPlayerEvent(abstractClientPlayerEntity);
        EventBus.INSTANCE.dispatch(renderPlayerEvent);
        yaw = abstractClientPlayerEntity.getYRot();
        prevYaw = abstractClientPlayerEntity.yRotO;
        bodyYaw = abstractClientPlayerEntity.yBodyRot;
        prevBodyYaw = abstractClientPlayerEntity.yBodyRotO;
        headYaw = abstractClientPlayerEntity.yHeadRot;
        prevHeadYaw = abstractClientPlayerEntity.yHeadRotO;
        pitch = abstractClientPlayerEntity.getXRot();
        prevPitch = abstractClientPlayerEntity.xRotO;
        prevSneaking = abstractClientPlayerEntity.isShiftKeyDown();
        if (renderPlayerEvent.isCanceled())
        {
            abstractClientPlayerEntity.setYRot(renderPlayerEvent.getYaw());
            abstractClientPlayerEntity.yRotO = renderPlayerEvent.getYaw();
            abstractClientPlayerEntity.setYBodyRot(renderPlayerEvent.getYaw());
            abstractClientPlayerEntity.yBodyRotO = renderPlayerEvent.getYaw();
            abstractClientPlayerEntity.setYHeadRot(renderPlayerEvent.getYaw());
            abstractClientPlayerEntity.yHeadRotO = renderPlayerEvent.getYaw();
            abstractClientPlayerEntity.setXRot(renderPlayerEvent.getPitch());
            abstractClientPlayerEntity.xRotO = renderPlayerEvent.getPitch();
            // abstractClientPlayerEntity.setSneaking(renderPlayerEvent.isSneaking());
        }
    }

    /**
     * @param abstractClientPlayerEntity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     * @param ci
     */
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/render /VertexConsumerProvider;I)V", at = @At(value = "TAIL"))
    private void onRenderTail(AbstractClientPlayer abstractClientPlayerEntity,
                              float f, float g, PoseStack matrixStack,
                              MultiBufferSource vertexConsumerProvider,
                              int i, CallbackInfo ci)
    {
        abstractClientPlayerEntity.setYRot(yaw);
        abstractClientPlayerEntity.yRotO = prevYaw;
        abstractClientPlayerEntity.setYBodyRot(bodyYaw);
        abstractClientPlayerEntity.yBodyRotO = prevBodyYaw;
        abstractClientPlayerEntity.setYHeadRot(headYaw);
        abstractClientPlayerEntity.yHeadRotO = prevHeadYaw;
        abstractClientPlayerEntity.setXRot(pitch);
        abstractClientPlayerEntity.xRotO = prevPitch;
        // abstractClientPlayerEntity.setSneaking(prevSneaking);
    }

    @Redirect(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean hookSetupTransforms(AbstractClientPlayer instance)
    {
        ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
        EventBus.INSTANCE.dispatch(elytraTransformEvent);
        if (elytraTransformEvent.isCanceled())
        {
            return false;
        }
        return instance.isFallFlying();
    }
}
