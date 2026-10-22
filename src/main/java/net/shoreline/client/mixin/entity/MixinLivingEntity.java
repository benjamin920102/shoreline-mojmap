package net.shoreline.client.mixin.entity;

import baritone.api.BaritoneAPI;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.impl.event.entity.*;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.module.movement.ElytraFlyModule;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity implements Globals
{
    //
    @Shadow
    protected ItemStack useItem;

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    @Shadow
    protected abstract float getJumpPower();

    @Shadow
    private int noJumpDelay;

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> par1);

    @Unique
    private boolean prevFlying;

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void hookGetHandSwingDuration(CallbackInfoReturnable<Integer> cir)
    {
        SwingSpeedEvent swingSpeedEvent = new SwingSpeedEvent();
        EventBus.INSTANCE.dispatch(swingSpeedEvent);
        if (swingSpeedEvent.isCanceled())
        {
            if (swingSpeedEvent.getSelfOnly() && ((Object) this != mc.player))
            {
                return;
            }
            cir.cancel();
            cir.setReturnValue(swingSpeedEvent.getSwingSpeed());
        }
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float hookJump$getYaw(float original)
    {
        if ((Object) this != mc.player)
        {
            return original;
        }

        JumpRotationEvent jumpRotationEvent = new JumpRotationEvent(original);
        EventBus.INSTANCE.dispatch(jumpRotationEvent);
        if (jumpRotationEvent.isCanceled())
        {
            return jumpRotationEvent.getYaw();
        }

        return original;
    }

    /**
     * @param instance
     * @param effect
     * @return
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean hookHasStatusEffect(LivingEntity instance, Holder<MobEffect> effect)
    {
        if (instance.equals(mc.player) && effect == MobEffects.LEVITATION)
        {
            LevitationEvent levitationEvent = new LevitationEvent();
            EventBus.INSTANCE.dispatch(levitationEvent);
            return !levitationEvent.isCanceled() && hasEffect(effect);
        }
        return hasEffect(effect);
    }

    /**
     * @param ci
     */
    @Inject(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER))
    private void hookConsumeItem(CallbackInfo ci)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        ConsumeItemEvent consumeItemEvent = new ConsumeItemEvent(useItem);
        EventBus.INSTANCE.dispatch(consumeItemEvent);
    }

    @Inject(method = "aiStep", at = @At(value = "HEAD"), cancellable = true)
    private void hookTickMovement(CallbackInfo ci)
    {
        JumpDelayEvent jumpDelayEvent = new JumpDelayEvent();
        EventBus.INSTANCE.dispatch(jumpDelayEvent);
        if (jumpDelayEvent.isCanceled())
        {
            noJumpDelay = 0;
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "HEAD"))
    private void hookAddStatusEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        StatusEffectEvent.Add statusEffectEvent = new StatusEffectEvent.Add(effect);
        EventBus.INSTANCE.dispatch(statusEffectEvent);
    }

    @Inject(method = "forceAddEffect", at = @At(value = "HEAD"))
    private void hookAddStatusEffect$1(MobEffectInstance effect, Entity source, CallbackInfo ci)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        StatusEffectEvent.Add statusEffectEvent = new StatusEffectEvent.Add(effect);
        EventBus.INSTANCE.dispatch(statusEffectEvent);
    }

    @Inject(method = "removeEffectNoUpdate", at = @At(value = "HEAD"))
    private void hookRemoveStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        StatusEffectEvent.Remove statusEffectEvent = new StatusEffectEvent.Remove(effect);
        EventBus.INSTANCE.dispatch(statusEffectEvent);
    }

    @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
    public void hookIsFallFlying(CallbackInfoReturnable<Boolean> cir)
    {
        if (ShorelineMod.isBaritonePresent() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
        {
            return;
        }
        boolean flying = cir.getReturnValue();
        boolean stoppedFlying = prevFlying && !flying;
        if (ElytraFlyModule.getInstance().isEnabled() && ElytraFlyModule.getInstance().isBounce() && stoppedFlying)
        {
            mc.player.startFallFlying();
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            cir.setReturnValue(true);
        }
        prevFlying = flying;
    }

    @Inject(method = "actuallyHurt", at = @At(value = "HEAD"))
    private void hookDamage(DamageSource source, float amount, CallbackInfo ci)
    {
        // Idk this doesnt work with direct checks??
        if (mc.player == null)
        {
            return;
        }

        if (source.getEntity() != null && source.getEntity().getName().getString().equalsIgnoreCase(mc.player.getName().getString()))
        {
            PlayerDamageEvent playerDamageEvent = new PlayerDamageEvent((LivingEntity) (Object) this);
            EventBus.INSTANCE.dispatch(playerDamageEvent);
        }
    }

    @Inject(method = "lerpTo", at = @At(value = "HEAD"))
    private void hookUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci)
    {
        UpdateServerPositionEvent updateServerPositionEvent = new UpdateServerPositionEvent((LivingEntity) (Object) this, x, y, z, yaw, pitch);
        EventBus.INSTANCE.dispatch(updateServerPositionEvent);
    }

    @Inject(method = "travel", at = @At(value = "HEAD"), cancellable = true)
    private void hookTravelPre(Vec3 movementInput, CallbackInfo ci)
    {
        EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity) (Object) this, true);
        EventBus.INSTANCE.dispatch(entityTravelEvent);
        if (entityTravelEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At(value = "RETURN"))
    private void hookTravelPost(Vec3 movementInput, CallbackInfo ci)
    {
        EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity) (Object) this, false);
        EventBus.INSTANCE.dispatch(entityTravelEvent);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private boolean hookTick(LivingEntity instance)
    {
        ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
        EventBus.INSTANCE.dispatch(elytraTransformEvent);
        if (elytraTransformEvent.isCanceled())
        {
            return false;
        }
        return instance.isFallFlying();
    }

    @Inject(method = "maxUpStep", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetStepHeight(CallbackInfoReturnable<Float> cir)
    {
        StepEvent stepEvent = new StepEvent((LivingEntity) (Object) this, cir.getReturnValueF());
        EventBus.INSTANCE.dispatch(stepEvent);
        if (stepEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(stepEvent.getStepHeight());
        }
    }

    @Inject(method = "onClimbable", at = @At(value = "HEAD"), cancellable = true)
    private void hookIsClimbing(CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this != mc.player)
        {
            return;
        }

        PlayerClimbEvent playerClimbEvent = new PlayerClimbEvent();
        EventBus.INSTANCE.dispatch(playerClimbEvent);
        if (playerClimbEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }
}