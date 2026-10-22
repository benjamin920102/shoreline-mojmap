package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.shoreline.client.impl.event.render.entity.RenderWitherSkullEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(WitherSkullRenderer.class)
public class MixinWitherSkullEntityRenderer
{
    /**
     * @param witherSkullEntity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/WitherSkull;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookRender(WitherSkull witherSkullEntity, float f,
                            float g, PoseStack matrixStack,
                            MultiBufferSource vertexConsumerProvider,
                            int i, CallbackInfo ci)
    {
        RenderWitherSkullEvent renderWitherSkullEvent =
                new RenderWitherSkullEvent();
        EventBus.INSTANCE.dispatch(renderWitherSkullEvent);
        if (renderWitherSkullEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
