package net.shoreline.client.mixin.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.impl.event.gui.hud.RenderCrosshairEvent;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @see Gui
 * @since 1.0
 */
@Mixin(Gui.class)
public class MixinInGameHud implements Globals
{
    @Shadow
    @Final
    private static ResourceLocation PUMPKIN_BLUR_LOCATION;
    //
    @Shadow
    @Final
    private static ResourceLocation POWDER_SNOW_OUTLINE_LOCATION;

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void hookRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci)
    {
        RenderOverlayEvent.Post renderOverlayEvent =
                new RenderOverlayEvent.Post(context, tickCounter.getGameTimeDeltaPartialTick(true));
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
    }

//    @Redirect(method = "renderItemHotbar", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I"))
//    private int hookRenderHotbar$selectedSlot(PlayerInventory instance) {
//        return Managers.INVENTORY.getServerSlot();
//    }

    /**
     * @param context
     * @param ci
     */
    @Inject(method = "renderEffects", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookRenderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci)
    {
        RenderOverlayEvent.StatusEffect renderOverlayEvent =
                new RenderOverlayEvent.StatusEffect(context);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param context
     * @param nauseaStrength
     * @param ci
     */
    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(GuiGraphics context,
                                         float nauseaStrength,
                                         CallbackInfo ci)
    {
        RenderOverlayEvent.Portal renderOverlayEvent = new RenderOverlayEvent.Portal(context);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param context
     * @param scale
     * @param ci
     */
    @Inject(method = "renderSpyglassOverlay", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookRenderSpyglassOverlay(GuiGraphics context, float scale,
                                           CallbackInfo ci)
    {
        RenderOverlayEvent.Spyglass renderOverlayEvent =
                new RenderOverlayEvent.Spyglass(context);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param context
     * @param texture
     * @param opacity
     * @param ci
     */
    @Inject(method = "renderTextureOverlay", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderOverlay(GuiGraphics context, ResourceLocation texture,
                                   float opacity, CallbackInfo ci)
    {
        if (texture.getPath().equals(PUMPKIN_BLUR_LOCATION.getPath()))
        {
            RenderOverlayEvent.Pumpkin renderOverlayEvent =
                    new RenderOverlayEvent.Pumpkin(context);
            EventBus.INSTANCE.dispatch(renderOverlayEvent);
            if (renderOverlayEvent.isCanceled())
            {
                ci.cancel();
            }
        }
        else if (texture.getPath().equals(POWDER_SNOW_OUTLINE_LOCATION.getPath()))
        {
            RenderOverlayEvent.Frostbite renderOverlayEvent =
                    new RenderOverlayEvent.Frostbite(context);
            EventBus.INSTANCE.dispatch(renderOverlayEvent);
            if (renderOverlayEvent.isCanceled())
            {
                ci.cancel();
            }
        }
    }

    /**
     * @param instance
     * @param text
     * @param x
     * @param y
     * @param color
     * @return
     */
    @Redirect(method = "renderSelectedItemName", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)I"))
    private int hookRenderHeldItemTooltip(GuiGraphics instance, Font textRenderer, Component text, int x, int y, int width, int color)
    {
        RenderOverlayEvent.ItemName renderOverlayEvent =
                new RenderOverlayEvent.ItemName(instance);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            if (renderOverlayEvent.isUpdateXY())
            {
                return instance.drawString(mc.font, text,
                        renderOverlayEvent.getX(), renderOverlayEvent.getY(), color, true);
            }
            return 0;
        }
        return instance.drawString(mc.font, text, x, y, color, true);
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "TAIL"))
    private void hookRenderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci)
    {
        RenderOverlayEvent.Hotbar hotbar = new RenderOverlayEvent.Hotbar(context);
        EventBus.INSTANCE.dispatch(hotbar);
    }

    @Inject(method = "renderCrosshair", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci)
    {
        RenderCrosshairEvent renderCrosshairEvent = new RenderCrosshairEvent(context);
        EventBus.INSTANCE.dispatch(renderCrosshairEvent);
        if (renderCrosshairEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
