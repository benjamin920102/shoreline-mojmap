package net.shoreline.client.impl.event.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.shoreline.eventbus.event.Event;

public class RenderEntityWorldEvent extends Event
{
    public final Entity entity;
    public final double cameraX;
    public final double cameraY;
    public final double cameraZ;
    public final float yaw;
    public final float tickDelta;
    public final PoseStack matrices;
    public final MultiBufferSource vertexConsumers;

    public RenderEntityWorldEvent(Entity entity, double cameraX, double cameraY, double cameraZ, float yaw,
                                  float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers)
    {
        this.entity = entity;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.yaw = yaw;
        this.tickDelta = tickDelta;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
    }
}
