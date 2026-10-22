package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.shoreline.client.impl.event.render.entity.RenderItemEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer
{
    /**
     * @param itemEntity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     * @param ci
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookRender(ItemEntity itemEntity, float f, float g,
                            PoseStack matrixStack,
                            MultiBufferSource vertexConsumerProvider,
                            int i, CallbackInfo ci)
    {
        RenderItemEvent renderItemEvent = new RenderItemEvent(itemEntity);
        EventBus.INSTANCE.dispatch(renderItemEvent);
        if (renderItemEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
