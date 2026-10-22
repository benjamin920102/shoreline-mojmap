package net.shoreline.client.mixin.render.block.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ShulkerBoxRenderer.class})
public class MixinShulkerBoxBlockEntityRenderer {
   @Redirect(
      method = {"render(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/resources/model/Material;buffer(Lnet/minecraft/client/renderer/MultiBufferSource;Ljava/util/function/Function;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
      )
   )
   private VertexConsumer hookGetVertexConsumers(Material instance, MultiBufferSource vertexConsumers, Function<ResourceLocation, RenderType> layerFactory) {
      return instance.buffer(vertexConsumers, RenderLayersClient.ENTITY_CUTOUT_NO_CULL);
   }
}
