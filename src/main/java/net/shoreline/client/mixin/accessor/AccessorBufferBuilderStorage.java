package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderBuffers.class)
public interface AccessorBufferBuilderStorage
{
    /**
     * @param vertexConsumerProvider
     */
    @Accessor("bufferSource")
    @Mutable
    void hookSetEntityVertexConsumers(MultiBufferSource.BufferSource vertexConsumerProvider);
}
