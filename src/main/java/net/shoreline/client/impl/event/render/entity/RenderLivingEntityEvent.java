package net.shoreline.client.impl.event.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderLivingEntityEvent extends Event {
   private final LivingEntity entity;
   private final EntityModel<?> model;
   private final PoseStack matrices;
   private final VertexConsumer vertexConsumer;
   private final int light;
   private final int overlay;
   private final float red;
   private final float green;
   private final float blue;
   private final float alpha;

   public RenderLivingEntityEvent(
      LivingEntity entity,
      EntityModel<?> model,
      PoseStack matrices,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      this.entity = entity;
      this.model = model;
      this.matrices = matrices;
      this.vertexConsumer = vertexConsumer;
      this.light = light;
      this.overlay = overlay;
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.alpha = alpha;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public EntityModel<?> getModel() {
      return this.model;
   }

   public PoseStack getMatrices() {
      return this.matrices;
   }

   public VertexConsumer getVertexConsumerProvider() {
      return this.vertexConsumer;
   }

   public int getLight() {
      return this.light;
   }

   public int getOverlay() {
      return this.overlay;
   }

   public float getRed() {
      return this.red;
   }

   public float getGreen() {
      return this.green;
   }

   public float getBlue() {
      return this.blue;
   }

   public float getAlpha() {
      return this.alpha;
   }
}
