package net.shoreline.client.mixin.render.block.entity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;

@Mixin(BedRenderer.class)
public class MixinBedBlockEntityRenderer
{
    @Redirect(method = "renderPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer hookRenderPart(Material instance, MultiBufferSource vertexConsumers, Function<ResourceLocation, RenderType> layerFactory)
    {
        return instance.buffer(vertexConsumers, RenderLayersClient.ENTITY_SOLID);
    }
}
