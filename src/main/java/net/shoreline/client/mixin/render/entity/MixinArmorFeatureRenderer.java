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

/**
 * @author linus
 * @see HumanoidArmorLayer
 * @since 1.0
 */
@Mixin(HumanoidArmorLayer.class)
public class MixinArmorFeatureRenderer
{
    /**
     * @param matrices
     * @param vertexConsumers
     * @param entity
     * @param armorSlot
     * @param light
     * @param model
     * @param ci
     */
    @Inject(method = "renderArmorPiece", at = @At(value = "HEAD"), cancellable = true)
    private void hookRenderArmor(PoseStack matrices,
                                 MultiBufferSource vertexConsumers,
                                 LivingEntity entity, EquipmentSlot armorSlot,
                                 int light, HumanoidModel<?> model, CallbackInfo ci)
    {
        RenderArmorEvent renderArmorEvent = new RenderArmorEvent(entity);
        EventBus.INSTANCE.dispatch(renderArmorEvent);
        if (renderArmorEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
