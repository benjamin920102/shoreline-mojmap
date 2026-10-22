package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.shoreline.client.impl.event.render.entity.RenderArmorEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidArmorLayer.class})
public class MixinArmorFeatureRenderer {
   @Inject(
      method = {"renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderArmor(
      PoseStack matrices, MultiBufferSource vertexConsumers, LivingEntity entity, EquipmentSlot armorSlot, int light, HumanoidModel<?> model, CallbackInfo ci
   ) {
      RenderArmorEvent renderArmorEvent = new RenderArmorEvent(entity);
      EventBus.INSTANCE.dispatch(renderArmorEvent);
      if (renderArmorEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
