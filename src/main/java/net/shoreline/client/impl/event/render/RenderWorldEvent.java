package net.shoreline.client.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.shoreline.eventbus.event.Event;

/**
 * @author linus
 * @since 1.0
 */
public class RenderWorldEvent extends Event
{
    //
    private final PoseStack matrices;
    private final float tickDelta;

    /**
     * @param matrices
     */
    public RenderWorldEvent(PoseStack matrices, float tickDelta)
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

    public static class Game extends RenderWorldEvent
    {

        /**
         * @param matrices
         * @param tickDelta
         */
        public Game(PoseStack matrices, float tickDelta)
        {
            super(matrices, tickDelta);
        }
    }

    public static class Hand extends RenderWorldEvent
    {
        /**
         * @param matrices
         * @param tickDelta
         */
        public Hand(PoseStack matrices, float tickDelta)
        {
            super(matrices, tickDelta);
        }
    }
}
