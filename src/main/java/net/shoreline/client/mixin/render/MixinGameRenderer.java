package net.shoreline.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.shoreline.client.api.render.satin.ReloadableShaderEffectManager;
import net.shoreline.client.impl.event.network.ReachEvent;
import net.shoreline.client.impl.event.render.*;
import net.shoreline.client.impl.event.world.UpdateCrosshairTargetEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author linus
 * @see GameRenderer
 * @since 1.0
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer implements Globals
{
    //
    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    private float oldFov;

    @Shadow
    private float fov;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void hookInit(Minecraft minecraft, ItemInHandRenderer heldItemRenderer, ResourceManager resourceManager, RenderBuffers buffers, CallbackInfo ci)
    {
        LightmapInitEvent lightmapInitEvent = new LightmapInitEvent();
        EventBus.INSTANCE.dispatch(lightmapInitEvent);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=hand"}), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void hookRenderWorld(DeltaTracker tickCounter, CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2, @Local(ordinal = 1) float tickDelta, @Local PoseStack matrixStack)
    {
        RenderWorldEvent.Game renderWorldEvent = new RenderWorldEvent.Game(matrixStack, tickDelta);
        EventBus.INSTANCE.dispatch(renderWorldEvent);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    public void hookRenderWorld$2(DeltaTracker tickCounter, CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2, @Local(ordinal = 1) float tickDelta, @Local PoseStack matrixStack)
    {
        RenderWorldEvent.Hand reloadShaderEvent = new RenderWorldEvent.Hand(matrixStack, tickDelta);
        EventBus.INSTANCE.dispatch(reloadShaderEvent);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"))
    private void hookRenderWorld$3(DeltaTracker tickCounter, CallbackInfo ci)
    {
        LightmapUpdateEvent lightmapUpdateEvent = new LightmapUpdateEvent(tickCounter.getGameTimeDeltaPartialTick(true));
        EventBus.INSTANCE.dispatch(lightmapUpdateEvent);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float hookLerpNausea(float delta, float start, float end)
    {
        RenderNauseaEvent renderNauseaEvent = new RenderNauseaEvent();
        EventBus.INSTANCE.dispatch(renderNauseaEvent);
        if (renderNauseaEvent.isCanceled())
        {
            return 0.0f;
        }
        return Mth.lerp(delta, start, end);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;tick()V"))
    private void hookTick(CallbackInfo ci)
    {
        LightmapTickEvent lightmapTickEvent = new LightmapTickEvent();
        EventBus.INSTANCE.dispatch(lightmapTickEvent);
    }

    @Inject(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void hookUpdateTargetedEntity$1(final float tickDelta, final CallbackInfo info)
    {
        UpdateCrosshairTargetEvent event = new UpdateCrosshairTargetEvent(tickDelta, minecraft.getCameraEntity());
        EventBus.INSTANCE.dispatch(event);
    }

    /**
     * @param matrices
     * @param tickDelta
     * @param ci
     */
    @Inject(method = "bobHurt", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookTiltViewWhenHurt(PoseStack matrices, float tickDelta,
                                      CallbackInfo ci)
    {
        HurtCamEvent hurtCamEvent = new HurtCamEvent();
        EventBus.INSTANCE.dispatch(hurtCamEvent);
        if (hurtCamEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param floatingItem
     * @param ci
     */
    @Inject(method = "displayItemActivation", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookShowFloatingItem(ItemStack floatingItem, CallbackInfo ci)
    {
        RenderFloatingItemEvent renderFloatingItemEvent =
                new RenderFloatingItemEvent(floatingItem);
        EventBus.INSTANCE.dispatch(renderFloatingItemEvent);
        if (renderFloatingItemEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param distortionStrength
     * @param ci
     */
    @Inject(method = "renderConfusionOverlay", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderNausea(GuiGraphics context, float distortionStrength, CallbackInfo ci)
    {
        RenderNauseaEvent renderNauseaEvent = new RenderNauseaEvent();
        EventBus.INSTANCE.dispatch(renderNauseaEvent);
        if (renderNauseaEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param cir
     */
    @Inject(method = "shouldRenderBlockOutline", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir)
    {
        RenderBlockOutlineEvent renderBlockOutlineEvent =
                new RenderBlockOutlineEvent();
        EventBus.INSTANCE.dispatch(renderBlockOutlineEvent);
        if (renderBlockOutlineEvent.isCanceled())
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    /**
     * @param tickDelta
     * @param info
     */
    @Inject(method = "pick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"), cancellable = true)
    private void hookUpdateTargetedEntity$2(float tickDelta, CallbackInfo info)
    {
        TargetEntityEvent targetEntityEvent = new TargetEntityEvent();
        EventBus.INSTANCE.dispatch(targetEntityEvent);
        if (targetEntityEvent.isCanceled() && minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK)
        {
            minecraft.getProfiler().pop();
            info.cancel();
        }
    }

    /**
     * @return
     */
    @Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;entityInteractionRange()D"))
    private double updateTargetedEntityModifySquaredMaxReach(LocalPlayer instance)
    {
        ReachEvent.Entity reachEvent = new ReachEvent.Entity();
        EventBus.INSTANCE.dispatch(reachEvent);
        return reachEvent.isCanceled() ? reachEvent.getReach() : instance.entityInteractionRange();
    }

    /**
     * @return
     */
    @Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;blockInteractionRange()D"))
    private double updateTargetedEntityModifySquaredMaxReach$1(LocalPlayer instance)
    {
        ReachEvent.Block reachEvent = new ReachEvent.Block();
        EventBus.INSTANCE.dispatch(reachEvent);
        return reachEvent.isCanceled() ? reachEvent.getReach() : instance.blockInteractionRange();
    }

    /**
     * @param matrices
     * @param tickDelta
     * @param ci
     */
    @Inject(method = "bobView", at = @At(value = "HEAD"), cancellable = true)
    private void hookBobView(PoseStack matrices, float tickDelta, CallbackInfo ci)
    {
        BobViewEvent bobViewEvent = new BobViewEvent(matrices, tickDelta);
        EventBus.INSTANCE.dispatch(bobViewEvent);
        if (bobViewEvent.isCanceled())
        {
            ci.cancel();
            matrices.translate(0.0f, bobViewEvent.getY(), 0.0f);
        }
    }

    /**
     * @param camera
     * @param tickDelta
     * @param changingFov
     * @param cir
     */
    @Inject(method = "getFov", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir)
    {
        FovEvent fovEvent = new FovEvent();
        EventBus.INSTANCE.dispatch(fovEvent);
        if (fovEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(fovEvent.getFov() * (double) Mth.lerp(tickDelta, oldFov, fov));
        }
    }

    /**
     * @param factory
     * @param ci
     */
    @Inject(method = "reloadShaders", at = @At(value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private void initPrograms(ResourceProvider factory, CallbackInfo ci)
    {
        LoadProgramsEvent loadProgramsEvent = new LoadProgramsEvent();
        EventBus.INSTANCE.dispatch(loadProgramsEvent);
    }

    @Inject(method = "reloadShaders", at = @At(value = "RETURN"))
    private void loadSatinPrograms(ResourceProvider factory, CallbackInfo ci)
    {
        ReloadableShaderEffectManager.INSTANCE.reload(factory);
    }
}
