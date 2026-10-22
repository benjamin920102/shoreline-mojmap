package net.shoreline.client.mixin.gui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.shoreline.client.impl.event.gui.hud.RenderOverlayEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer
{
    /**
     * @param client
     * @param matrices
     * @param ci
     */
    @Inject(method = "renderFire", at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookRenderFireOverlay(Minecraft client, PoseStack matrices, CallbackInfo ci)
    {
        RenderOverlayEvent.Fire renderOverlayEvent =
                new RenderOverlayEvent.Fire(null);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param client
     * @param matrices
     * @param ci
     */
    @Inject(method = "renderWater", at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookRenderUnderwaterOverlay(Minecraft client, PoseStack matrices,
                                                    CallbackInfo ci)
    {
        RenderOverlayEvent.Water renderOverlayEvent =
                new RenderOverlayEvent.Water(null);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param sprite
     * @param matrices
     * @param ci
     */
    @Inject(method = "renderTex", at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookRenderFireOverlay(TextureAtlasSprite sprite, PoseStack matrices, CallbackInfo ci)
    {
        RenderOverlayEvent.Block renderOverlayEvent =
                new RenderOverlayEvent.Block(null);
        EventBus.INSTANCE.dispatch(renderOverlayEvent);
        if (renderOverlayEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
