package net.shoreline.client.mixin.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.shoreline.client.impl.event.render.block.RenderTileEntityEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(EnchantTableRenderer.class)
public class MixinEnchantingTableBlockEntityRenderer
{
    @Shadow
    @Final
    private BookModel bookModel;

    /**
     * DO NOT CHANGE TO REDIRECT
     */
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at =
    @At(value = "INVOKE", target = "Lnet/minecraft/client/model/BookModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            shift = At.Shift.BEFORE), cancellable = true)
    private void hookRender(EnchantingTableBlockEntity enchantingTableBlockEntity,
                            float f, PoseStack matrixStack,
                            MultiBufferSource vertexConsumerProvider,
                            int i, int j, CallbackInfo ci)
    {
        RenderTileEntityEvent.EnchantingTableBook renderTileEntityEvent =
                new RenderTileEntityEvent.EnchantingTableBook();
        EventBus.INSTANCE.dispatch(renderTileEntityEvent);
        if (renderTileEntityEvent.isCanceled())
        {
            ci.cancel();
            matrixStack.popPose();
        }
    }
}
