package net.shoreline.client.impl.event.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderCrystalEvent extends Event {
   public final EndCrystal endCrystalEntity;
   public final float f;
   public final float g;
   public final PoseStack matrixStack;
   public final int i;
   public final ModelPart core;
   public final ModelPart frame;
   public float spin = 1.0F;
   public float scale = 1.0F;
   public boolean bounce = true;

   public RenderCrystalEvent(EndCrystal endCrystalEntity, float f, float g, PoseStack matrixStack, int i, ModelPart core, ModelPart frame) {
      this.endCrystalEntity = endCrystalEntity;
      this.f = f;
      this.g = g;
      this.matrixStack = matrixStack;
      this.i = i;
      this.core = core;
      this.frame = frame;
   }

   public float getSpin() {
      return this.spin;
   }

   public float getScale() {
      return this.scale;
   }

   public boolean getBounce() {
      return this.bounce;
   }

   public void setSpin(float spin) {
      this.spin = spin;
   }

   public void setScale(float scale) {
      this.scale = scale;
   }

   public void setBounce(boolean bounce) {
      this.bounce = bounce;
   }
}
