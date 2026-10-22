package net.shoreline.client.mixin.render.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.impl.event.render.entity.RenderEntityEvent;
import net.shoreline.client.impl.event.render.entity.RenderEntityInvisibleEvent;
import net.shoreline.client.impl.event.render.entity.RenderThroughWallsEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   @Shadow
   protected M model;
   @Shadow
   @Final
   protected List<RenderLayer<T, M>> layers;

   @Shadow
   protected abstract RenderType getRenderType(T var1, boolean var2, boolean var3, boolean var4);

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRender(LivingEntity livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
      RenderEntityEvent renderEntityEvent = new RenderEntityEvent<>(
         (LivingEntityRenderer<T, EntityModel<T>>)(Object)this,
         livingEntity,
         f,
         g,
         matrixStack,
         vertexConsumerProvider,
         i,
         this.model,
         this.getRenderType((T)livingEntity, false, false, false),
         (List)this.layers
      );
      EventBus.INSTANCE.dispatch(renderEntityEvent);
      if (renderEntityEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
      )
   )
   private boolean redirectRender$isInvisibleTo(LivingEntity entity, Player player) {
      RenderEntityInvisibleEvent event = new RenderEntityInvisibleEvent(entity);
      EventBus.INSTANCE.dispatch(event);
      return event.isCanceled() ? false : entity.isInvisibleTo(player);
   }

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRender$1(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
      RenderThroughWallsEvent renderThroughWallsEvent = new RenderThroughWallsEvent(livingEntity);
      EventBus.INSTANCE.dispatch(renderThroughWallsEvent);
      if (renderThroughWallsEvent.isCanceled()) {
         RenderSystem.disableDepthTest();
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("TAIL")}
   )
   private void hookRender$2(T livingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
      RenderThroughWallsEvent renderThroughWallsEvent = new RenderThroughWallsEvent(livingEntity);
      EventBus.INSTANCE.dispatch(renderThroughWallsEvent);
      if (renderThroughWallsEvent.isCanceled()) {
         RenderSystem.enableDepthTest();
      }
   }
}
