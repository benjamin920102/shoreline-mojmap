package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.shoreline.client.impl.event.gui.RenderTooltipEvent;
import net.shoreline.client.impl.event.gui.screen.MouseDraggedEvent;
import net.shoreline.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractContainerScreen.class})
public abstract class MixinHandledScreen {
   @Shadow
   @Nullable
   protected Slot hoveredSlot;
   @Shadow
   private boolean doubleclick;

   @Shadow
   @Nullable
   protected abstract Slot findSlot(double var1, double var3);

   @Shadow
   protected abstract void slotClicked(Slot var1, int var2, int var3, ClickType var4);

   @Inject(
      method = {"renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDrawMouseoverTooltip(GuiGraphics context, int x, int y, CallbackInfo ci) {
      if (this.hoveredSlot != null) {
         RenderTooltipEvent renderTooltipEvent = new RenderTooltipEvent(context, this.hoveredSlot.getItem(), x, y);
         EventBus.INSTANCE.dispatch(renderTooltipEvent);
         if (renderTooltipEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"mouseDragged(DDIDD)Z"},
      at = {@At("TAIL")}
   )
   private void hookMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
      MouseDraggedEvent mouseDraggedEvent = new MouseDraggedEvent();
      EventBus.INSTANCE.dispatch(mouseDraggedEvent);
      if (button == 0 && !this.doubleclick && mouseDraggedEvent.isCanceled()) {
         Slot slot = this.findSlot(mouseX, mouseY);
         if (slot != null && slot.hasItem() && Screen.hasShiftDown()) {
            this.slotClicked(slot, slot.index, button, ClickType.QUICK_MOVE);
         }
      }
   }
}
