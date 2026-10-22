package net.shoreline.client.mixin.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.impl.event.render.block.RenderBlockEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderManager
{
    // This bs is incompatible with sodium
    @Inject(method = "renderBatched", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderBlock(BlockState state, BlockPos pos, BlockAndTintGetter world,
                                 PoseStack matrices, VertexConsumer vertexConsumer,
                                 boolean cull, RandomSource random, CallbackInfo ci)
    {
        RenderBlockEvent renderBlockEvent =
                new RenderBlockEvent(state, pos);
        EventBus.INSTANCE.dispatch(renderBlockEvent);
        if (renderBlockEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
