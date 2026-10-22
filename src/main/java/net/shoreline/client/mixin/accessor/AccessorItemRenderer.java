package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ItemRenderer.class})
public interface AccessorItemRenderer {
   @Accessor("blockEntityRenderer")
   BlockEntityWithoutLevelRenderer hookGetBuiltinModelItemRenderer();

   @Accessor("itemColors")
   ItemColors hookGetItemColors();

   @Invoker("renderModelLists")
   void hookRenderBakedItemModel(BakedModel var1, ItemStack var2, int var3, int var4, PoseStack var5, VertexConsumer var6);
}
