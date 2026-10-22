package net.shoreline.client.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LevelRenderer.class})
public interface AccessorWorldRenderer {
   @Accessor("cullingFrustum")
   Frustum getFrustum();

   @Accessor("destroyingBlocks")
   Int2ObjectMap<BlockDestructionProgress> getBlockBreakingProgressions();

   @Accessor("renderBuffers")
   RenderBuffers hookGetBufferBuilders();

   @Accessor("globalBlockEntities")
   Set<BlockEntity> hookGetNoCullingBlockEntities();
}
