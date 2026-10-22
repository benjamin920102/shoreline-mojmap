package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.event.render.entity.RenderLabelEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer
{

    /**
     * @param entity
     * @param text
     * @param matrices
     * @param vertexConsumers
     * @param light
     * @param ci
     */
    @Inject(method = "renderNameTag", at = @At(value = "HEAD"), cancellable = true)
    public void hookRenderLabelIfPresent(Entity entity, Component text, PoseStack matrices, MultiBufferSource vertexConsumers, int light, float tickDelta, CallbackInfo ci)
    {
        RenderLabelEvent renderLabelEvent = new RenderLabelEvent(entity);
        EventBus.INSTANCE.dispatch(renderLabelEvent);
        if (renderLabelEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
