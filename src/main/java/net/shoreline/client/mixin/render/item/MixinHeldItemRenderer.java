package net.shoreline.client.mixin.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.shoreline.client.impl.event.render.item.*;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinHeldItemRenderer implements Globals
{

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ItemStack mainHandItem;
    
    @Shadow
    private ItemStack offHandItem;

    @Shadow
    public float mainHandHeight;

    @Shadow
    public float offHandHeight;

    @Shadow
    public float oMainHandHeight;

    @Shadow
    public float oOffHandHeight;

    /**
     * @param matrices
     * @param vertexConsumers
     * @param light
     * @param arm
     * @param ci
     */
    @Inject(method = "renderPlayerArm", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderArm(PoseStack matrices, MultiBufferSource vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci)
    {
        PlayerRenderer playerEntityRenderer = (PlayerRenderer) entityRenderDispatcher.getRenderer(minecraft.player);
        RenderArmEvent renderArmEvent = new RenderArmEvent(matrices, vertexConsumers, light, equipProgress, swingProgress, arm, playerEntityRenderer);
        EventBus.INSTANCE.dispatch(renderArmEvent);
        if (renderArmEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderFirstPersonItem$2(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci)
    {
        RenderFirstPersonEvent.Head renderFirstPersonEvent = new RenderFirstPersonEvent.Head(hand, item, equipProgress, matrices);
        EventBus.INSTANCE.dispatch(renderFirstPersonEvent);
        if (renderFirstPersonEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param player
     * @param tickDelta
     * @param pitch
     * @param hand
     * @param swingProgress
     * @param item
     * @param equipProgress
     * @param matrices
     * @param vertexConsumers
     * @param light
     * @param ci
     */
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void hookRenderFirstPersonItem(AbstractClientPlayer player, float tickDelta,
                                           float pitch, InteractionHand hand, float swingProgress,
                                           ItemStack item, float equipProgress, PoseStack matrices,
                                           MultiBufferSource vertexConsumers, int light, CallbackInfo ci)
    {
        RenderFirstPersonEvent renderFirstPersonEvent = new RenderFirstPersonEvent(hand, item, equipProgress, matrices);
        EventBus.INSTANCE.dispatch(renderFirstPersonEvent);
    }

    @Inject(method = "applyEatTransform", at = @At(value = "HEAD"), cancellable = true)
    private void hookApplyEatOrDrinkTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, Player player, CallbackInfo ci)
    {
        ci.cancel();
        float h;
        float f = (float)this.minecraft.player.getUseItemRemainingTicks() - tickDelta + 1.0f;
        float g = f / (float)stack.getUseDuration(mc.player);
        if (g < 0.8f)
        {
            h = Mth.abs(Mth.cos(f / 4.0f * (float)Math.PI) * 0.1f);
            EatTransformationEvent eatTransformationEvent = new EatTransformationEvent();
            EventBus.INSTANCE.dispatch(eatTransformationEvent);
            matrices.translate(0.0f, eatTransformationEvent.isCanceled() ? h * eatTransformationEvent.getFactor() : h, 0.0f);
        }
        h = 1.0f - (float)Math.pow(g, 27.0);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6f * (float)i, h * -0.5f, h * 0.0f);
        matrices.mulPose(Axis.YP.rotationDegrees((float)i * h * 90.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(h * 10.0f));
        matrices.mulPose(Axis.ZP.rotationDegrees((float)i * h * 30.0f));
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2), index = 0)
    private float hookEquipProgressMainhand(float value)
    {
        RenderSwingAnimationEvent renderSwingAnimation = new RenderSwingAnimationEvent();
        EventBus.INSTANCE.dispatch(renderSwingAnimation);
        float f = mc.player.getAttackStrengthScale(1.0f);
        float modified = renderSwingAnimation.isCanceled() ? 1.0f : f * f * f;
        return (ItemStack.matches(mainHandItem, mc.player.getMainHandItem()) ? modified : 0.0f) - mainHandHeight;
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void hookUpdateHeldItems(CallbackInfo ci)
    {
        ItemStack itemStack = mc.player.getMainHandItem();
        ItemStack itemStack2 = mc.player.getOffhandItem();
        UpdateHeldItemsEvent updateHeldItemsEvent = new UpdateHeldItemsEvent();
        EventBus.INSTANCE.dispatch(updateHeldItemsEvent);
        if (updateHeldItemsEvent.isCanceled())
        {
            ci.cancel();
            mainHandHeight = 1.0f;
            offHandHeight = 1.0f;
            oMainHandHeight = 1.0f;
            oOffHandHeight = 1.0f;
            mainHandItem = itemStack;
            offHandItem = itemStack2;
        }
    }
}
