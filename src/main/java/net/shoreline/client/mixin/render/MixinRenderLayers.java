package net.shoreline.client.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public abstract class MixinRenderLayers
{
    @Shadow
    public static RenderType getChunkRenderType(BlockState state)
    {
        return null;
    }

    @Inject(method = "getRenderType", at = @At(value = "HEAD"), cancellable = true)
    private static void hookGetEntityBlockLayer(BlockState state, boolean direct, CallbackInfoReturnable<RenderType> cir)
    {
//        if (!(state.getBlock() instanceof AbstractBannerBlock || state.getBlock() instanceof BedBlock || state.isOf(Blocks.CONDUIT)
//                || state.isOf(Blocks.CHEST) || state.isOf(Blocks.ENDER_CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.DECORATED_POT) || state.getBlock() instanceof ShulkerBoxBlock))
//        {
//            return;
//        }
    }

    @Inject(method = "getRenderType", at = @At(value = "HEAD"), cancellable = true)
    private static void hookGetItemLayer(ItemStack stack, boolean direct, CallbackInfoReturnable<RenderType> cir)
    {
        cir.cancel();
        Item item = stack.getItem();
        if (item instanceof BlockItem)
        {
            Block block = ((BlockItem) item).getBlock();
            RenderType renderLayer = getChunkRenderType(block.defaultBlockState());
            if (renderLayer == RenderType.translucent())
            {
                if (!Minecraft.useShaderTransparency())
                {
                    cir.setReturnValue(RenderLayersClient.ENTITY_TRANSLUCENT_CULL.apply(TextureAtlas.LOCATION_BLOCKS));
                }
                else
                {
                    cir.setReturnValue(direct ? RenderLayersClient.ENTITY_TRANSLUCENT_CULL.apply(TextureAtlas.LOCATION_BLOCKS) : RenderLayersClient.ITEM_ENTITY_TRANSLUCENT_CULL_2.apply(TextureAtlas.LOCATION_BLOCKS));
                }
            }
            else
            {
                cir.setReturnValue(RenderLayersClient.ENTITY_CUTOUT.apply(TextureAtlas.LOCATION_BLOCKS));
            }
        }
        else
        {
            cir.setReturnValue(direct ? RenderLayersClient.ENTITY_TRANSLUCENT_CULL.apply(TextureAtlas.LOCATION_BLOCKS) : RenderLayersClient.ITEM_ENTITY_TRANSLUCENT_CULL_2.apply(TextureAtlas.LOCATION_BLOCKS));
        }
    }
}
