package net.shoreline.client.mixin.render.entity;

import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.impl.event.render.entity.RenderItemFrameEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ItemFrameRenderer.class})
public class MixinItemFrameEntityRenderer {
   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"
      )
   )
   private boolean redirectIsEmpty(ItemStack itemStack) {
      RenderItemFrameEvent renderItemFrameEvent = new RenderItemFrameEvent();
      EventBus.INSTANCE.dispatch(renderItemFrameEvent);
      return renderItemFrameEvent.isCanceled() ? true : itemStack.isEmpty();
   }
}
