package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderBuffers.class})
public interface AccessorBufferBuilderStorage {
   @Accessor("bufferSource")
   @Mutable
   void hookSetEntityVertexConsumers(BufferSource var1);
}
