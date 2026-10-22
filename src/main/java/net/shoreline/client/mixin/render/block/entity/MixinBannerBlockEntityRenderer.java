package net.shoreline.client.mixin.render.block.entity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;

@Mixin(BannerRenderer.class)
public class MixinBannerBlockEntityRenderer
{
    @Redirect(method = "renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;Z)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static VertexConsumer hookRenderCanvas(Material instance, MultiBufferSource vertexConsumers, Function<ResourceLocation, RenderType> layerFactory, boolean bl)
    {
        return instance.buffer(vertexConsumers, RenderLayersClient.ENTITY_SOLID);
    }
}
