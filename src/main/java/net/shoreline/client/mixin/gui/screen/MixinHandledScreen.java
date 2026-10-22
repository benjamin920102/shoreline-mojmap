package net.shoreline.client.mixin.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
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

import static net.minecraft.client.gui.screens.Screen.hasShiftDown;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen
{

    @Shadow
    @Nullable
    protected abstract Slot findSlot(double xPosition, double yPosition);

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    private boolean doubleclick;

    @Shadow
    protected abstract void slotClicked(Slot slot, int invSlot, int clickData, ClickType actionType);


    @Inject(method = "renderTooltip", at = @At(value = "HEAD"), cancellable = true)
    private void hookDrawMouseoverTooltip(GuiGraphics context, int x, int y, CallbackInfo ci)
    {
        if (hoveredSlot == null)
        {
            return;
        }
        RenderTooltipEvent renderTooltipEvent =
                new RenderTooltipEvent(context, hoveredSlot.getItem(), x, y);
        EventBus.INSTANCE.dispatch(renderTooltipEvent);
        if (renderTooltipEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("TAIL"))
    private void hookMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir)
    {
        MouseDraggedEvent mouseDraggedEvent = new MouseDraggedEvent();
        EventBus.INSTANCE.dispatch(mouseDraggedEvent);
        if (button != GLFW_MOUSE_BUTTON_LEFT
                || doubleclick
                || !mouseDraggedEvent.isCanceled())
        {
            return;
        }
        Slot slot = findSlot(mouseX, mouseY);
        if (slot != null && slot.hasItem() && hasShiftDown())
        {
            slotClicked(slot, slot.index, button, ClickType.QUICK_MOVE);
        }
    }
}
