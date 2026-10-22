package net.shoreline.client.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.camera.EntityCameraPositionEvent;
import net.shoreline.client.impl.event.entity.*;
import net.shoreline.client.impl.event.entity.decoration.TeamColorEvent;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @see Entity
 * @since 1.0
 */
@Mixin(Entity.class)
public abstract class MixinEntity implements Globals
{

    /**
     * @param movementInput
     * @param speed
     * @param yaw
     * @return
     */
    @Shadow
    private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw)
    {
        return null;
    }

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 deltaMovement);

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public boolean hasImpulse;

    @Shadow private Vec3 deltaMovement;

    @Shadow protected Vec3 stuckSpeedMultiplier;

    @Shadow public float fallDistance;

    /**
     * @param tickDelta
     * @param info
     * @author xgraza
     */
    @Inject(method = "getViewVector", at = @At("RETURN"), cancellable = true)
    public void hookGetCameraPosVec(final float tickDelta, final CallbackInfoReturnable<Vec3> info)
    {
        final EntityRotationVectorEvent event = new EntityRotationVectorEvent(
                tickDelta, (Entity) (Object) this, info.getReturnValue());
        EventBus.INSTANCE.dispatch(event);
        info.setReturnValue(event.getPosition());
    }

    /**
     * @param movement
     * @param cir
     */
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At(value = "HEAD"))
    public void hookMove(Vec3 movement, CallbackInfoReturnable<Vec3> cir)
    {
        if ((Object) this != mc.player)
        {
        }
        // StepEvent stepEvent = new StepEvent(cir.getReturnValue().y - movement.y);
        // EventBus.INSTANCE.dispatch(stepEvent);
    }

    /**
     * @param state
     * @param multiplier
     * @param ci
     */
    @Inject(method = "makeStuckInBlock", at = @At(value = "HEAD"), cancellable = true)
    private void hookSlowMovement(BlockState state, Vec3 multiplier, CallbackInfo ci)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
        EventBus.INSTANCE.dispatch(slowMovementEvent);
        if (slowMovementEvent.isCanceled())
        {
            ci.cancel();
            this.fallDistance = 0.0f;
            this.stuckSpeedMultiplier = multiplier.scale(slowMovementEvent.getMultiplier());
        }
    }

    /**
     * @param instance
     * @return
     */
    @Redirect(method = "getBlockSpeedFactor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/ block/Block;"))
    private Block hookGetVelocityMultiplier(BlockState instance)
    {
        if ((Object) this != mc.player)
        {
            return instance.getBlock();
        }
        VelocityMultiplierEvent velocityMultiplierEvent =
                new VelocityMultiplierEvent(instance);
        EventBus.INSTANCE.dispatch(velocityMultiplierEvent);
        if (velocityMultiplierEvent.isCanceled())
        {
            return Blocks.DIRT;
        }
        return instance.getBlock();
    }

    /**
     * @param speed
     * @param movementInput
     * @param ci
     */
    @Inject(method = "moveRelative", at = @At(value = "HEAD"), cancellable = true)
    private void hookUpdateVelocity(float speed, Vec3 movementInput, CallbackInfo ci)
    {
        if ((Object) this == mc.player)
        {
            UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(movementInput, speed, mc.player.getYRot(), getInputVector(movementInput, speed, mc.player.getYRot()));
            EventBus.INSTANCE.dispatch(updateVelocityEvent);
            if (updateVelocityEvent.isCanceled())
            {
                ci.cancel();
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(updateVelocityEvent.getVelocity()));
            }
        }
    }

    /**
     * @param entity
     * @param ci
     */
    @Inject(method = "push", at = @At(value = "HEAD"), cancellable = true)
    private void hookPushAwayFrom(Entity entity, CallbackInfo ci)
    {
        PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity) (Object) this, entity);
        EventBus.INSTANCE.dispatch(pushEntityEvent);
        if (pushEntityEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param cir
     */
    @Inject(method = "getTeamColor", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookGetTeamColorValue(CallbackInfoReturnable<Integer> cir)
    {
        TeamColorEvent teamColorEvent =
                new TeamColorEvent((Entity) (Object) this);
        EventBus.INSTANCE.dispatch(teamColorEvent);
        if (teamColorEvent.isCanceled())
        {
            cir.setReturnValue(teamColorEvent.getColor());
            cir.cancel();
        }
    }

    /**
     * @param cursorDeltaX
     * @param cursorDeltaY
     * @param ci
     */
    @Inject(method = "turn", at = @At(value = "HEAD"), cancellable = true)
    private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci)
    {
        if ((Object) this == mc.player)
        {
            LookDirectionEvent lookDirectionEvent = new LookDirectionEvent(
                    (Entity) (Object) this, cursorDeltaX, cursorDeltaY);
            EventBus.INSTANCE.dispatch(lookDirectionEvent);
            if (lookDirectionEvent.isCanceled())
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getEyePosition", at = @At("RETURN"), cancellable = true)
    public void hookCameraPositionVec(float tickDelta, CallbackInfoReturnable<Vec3> cir)
    {
        EntityCameraPositionEvent cameraPositionEvent = new EntityCameraPositionEvent(cir.getReturnValue(), (Entity) (Object) this, tickDelta);
        EventBus.INSTANCE.dispatch(cameraPositionEvent);
        cir.setReturnValue(cameraPositionEvent.getPosition());
    }

    @Inject(method = "setBoundingBox", at = @At(value = "HEAD"))
    private void hookSetBoundingBox(AABB boundingBox, CallbackInfo ci)
    {
        if ((Object) this == mc.player)
        {
            SetBBEvent setBBEvent = new SetBBEvent(boundingBox);
            EventBus.INSTANCE.dispatch(setBBEvent);
        }
    }

    @Inject(method = "displayFireAnimation", at = @At(value = "HEAD"), cancellable = true)
    private void hookDoesRenderOnFire(CallbackInfoReturnable<Boolean> cir)
    {
        RenderFireEntityEvent renderFireEntityEvent = new RenderFireEntityEvent();
        EventBus.INSTANCE.dispatch(renderFireEntityEvent);
        if (renderFireEntityEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookSetVelocity(Vec3 deltaMovement, CallbackInfo ci)
    {
        VelocityEvent velocityEvent = new VelocityEvent(deltaMovement);
        EventBus.INSTANCE.dispatch(velocityEvent);
        if (velocityEvent.isCanceled())
        {
            ci.cancel();
            this.deltaMovement = velocityEvent.getVelocity();
        }
    }
}
