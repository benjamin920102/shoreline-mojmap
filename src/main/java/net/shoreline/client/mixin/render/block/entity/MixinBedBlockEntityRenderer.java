package net.shoreline.client.mixin.render.block.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BedRenderer.class})
public class MixinBedBlockEntityRenderer {
   @Redirect(
      method = {"renderPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/core/Direction;Lnet/minecraft/client/resources/model/Material;IIZ)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
      )
   )
   private VertexConsumer hookRenderPart(Material instance, MultiBufferSource vertexConsumers, Function<ResourceLocation, RenderType> layerFactory) {
      return instance.buffer(vertexConsumers, RenderLayersClient.ENTITY_SOLID);
   }
}
