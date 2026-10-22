package net.shoreline.client.impl.event.render.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;

@Cancelable
public class RenderEntityEvent<T extends LivingEntity> extends Event
{
    public final LivingEntityRenderer<T, EntityModel<T>> renderer;
    public final LivingEntity entity;

    public final float f;
    public final float g;
    public final PoseStack matrixStack;
    public final MultiBufferSource vertexConsumerProvider;
    public final int i;
    public final RenderType layer;
    public final EntityModel model;
    //
    public final List<RenderLayer<T, EntityModel<T>>> features;

    /**
     * @param entity
     * @param f
     * @param g
     * @param matrixStack
     * @param vertexConsumerProvider
     * @param i
     * @param model
     * @param features
     */
    public RenderEntityEvent(LivingEntityRenderer<T, EntityModel<T>> renderer, LivingEntity entity,
                             float f, float g, PoseStack matrixStack,
                             MultiBufferSource vertexConsumerProvider,
                             int i, EntityModel model, RenderType layer,
                             List<RenderLayer<T, EntityModel<T>>> features)
    {
        this.renderer = renderer;
        this.entity = entity;
        this.f = f;
        this.g = g;
        this.matrixStack = matrixStack;
        this.vertexConsumerProvider = vertexConsumerProvider;
        this.i = i;
        this.model = model;
        this.layer = layer;
        this.features = features;
    }
}
