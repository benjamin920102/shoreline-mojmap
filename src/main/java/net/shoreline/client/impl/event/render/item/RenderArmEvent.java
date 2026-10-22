package net.shoreline.client.impl.event.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.shoreline.eventbus.annotation.Cancelable;
import net.shoreline.eventbus.event.Event;

@Cancelable
public class RenderArmEvent extends Event {
   public final PoseStack matrices;
   public final MultiBufferSource vertexConsumers;
   public final int light;
   public final HumanoidArm arm;
   public final float equipProgress;
   public final float swingProgress;
   public final PlayerRenderer playerEntityRenderer;

   public RenderArmEvent(
      PoseStack matrices,
      MultiBufferSource vertexConsumers,
      int light,
      float equipProgress,
      float swingProgress,
      HumanoidArm arm,
      PlayerRenderer playerEntityRenderer
   ) {
      this.matrices = matrices;
      this.vertexConsumers = vertexConsumers;
      this.light = light;
      this.equipProgress = equipProgress;
      this.swingProgress = swingProgress;
      this.arm = arm;
      this.playerEntityRenderer = playerEntityRenderer;
   }
}
