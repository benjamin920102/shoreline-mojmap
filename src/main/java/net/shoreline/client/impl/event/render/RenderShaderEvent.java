package net.shoreline.client.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderShaderEvent extends Event
{
    private final PoseStack matrices;
    private final float tickDelta;

    /**
     * @param matrices
     */
    public RenderShaderEvent(PoseStack matrices, float tickDelta)
    {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
    }

    /**
     * @return
     */
    public PoseStack getMatrices()
    {
        return matrices;
    }

    /**
     * @return
     */
    public float getTickDelta()
    {
        return tickDelta;
    }

    public static class BlockEntities extends RenderShaderEvent
    {
        /**
         * @param matrices
         * @param tickDelta
         */
        public BlockEntities(PoseStack matrices, float tickDelta)
        {
            super(matrices, tickDelta);
        }
    }
}