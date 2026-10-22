package net.shoreline.client.mixin.entity.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.entity.player.*;
import net.shoreline.client.impl.event.network.ReachEvent;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity implements Globals
{
    /**
     * @param entityType
     * @param world
     */
    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world)
    {
        super(entityType, world);
    }

    @Shadow
    public abstract void travel(Vec3 movementInput);

    /**
     * @param movementInput
     * @param ci
     */
    @Inject(method = "travel", at = @At(value = "HEAD"), cancellable = true)
    private void hookTravelHead(Vec3 movementInput, CallbackInfo ci)
    {
        TravelEvent travelEvent = new TravelEvent(movementInput);
        EventBus.INSTANCE.dispatch(travelEvent);
        if (travelEvent.isCanceled())
        {
            move(MoverType.SELF, getDeltaMovement());
            ci.cancel();
        }
    }

    /**
     * @param cir
     */
    @Inject(method = "isPushedByFluid", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookIsPushedByFluids(CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        PushFluidsEvent pushFluidsEvent = new PushFluidsEvent();
        EventBus.INSTANCE.dispatch(pushFluidsEvent);
        if (pushFluidsEvent.isCanceled())
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    /**
     * @param ci
     */
    @Inject(method = "jumpFromGround", at = @At(value = "HEAD"), cancellable = true)
    private void hookJumpPre(CallbackInfo ci)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
        playerJumpEvent.setStage(StageEvent.EventStage.PRE);
        EventBus.INSTANCE.dispatch(playerJumpEvent);
        if (playerJumpEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param ci
     */
    @Inject(method = "jumpFromGround", at = @At(value = "RETURN"), cancellable = true)
    private void hookJumpPost(CallbackInfo ci)
    {
        if ((Object) this != mc.player)
        {
            return;
        }
        PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
        playerJumpEvent.setStage(StageEvent.EventStage.POST);
        EventBus.INSTANCE.dispatch(playerJumpEvent);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hookAttack(Player playerEntity, Vec3 movementInput)
    {
        if (playerEntity instanceof LocalPlayer)
        {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            EventBus.INSTANCE.dispatch(sprintResetEvent);
            if (!sprintResetEvent.isCanceled())
            {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void hookAttack$1(Player instance, boolean b)
    {
        if (instance instanceof LocalPlayer)
        {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            EventBus.INSTANCE.dispatch(sprintResetEvent);
            if (!sprintResetEvent.isCanceled())
            {
                mc.player.setSprinting(false);
            }
        }
    }

    @Inject(method = "isStayingOnGroundSurface", at = @At(value = "HEAD"), cancellable = true)
    private void hookClipAtLedge(CallbackInfoReturnable<Boolean> cir)
    {
        LedgeClipEvent ledgeClipEvent = new LedgeClipEvent();
        EventBus.INSTANCE.dispatch(ledgeClipEvent);
        if (ledgeClipEvent.isCanceled())
        {
            cir.setReturnValue(ledgeClipEvent.isClipped());
        }
    }

    @Redirect(method = "updatePlayerPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isFallFlying()Z"))
    private boolean hookUpdatePose(Player instance)
    {
        ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
        EventBus.INSTANCE.dispatch(elytraTransformEvent);
        if (elytraTransformEvent.isCanceled())
        {
            return false;
        }
        return instance.isFallFlying();
    }

    @Inject(method = "blockInteractionRange", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetBlockInteractionRange(CallbackInfoReturnable<Double> cir)
    {
        final ReachEvent.Block reachEvent = new ReachEvent.Block();
        EventBus.INSTANCE.dispatch(reachEvent);
        if (reachEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(reachEvent.getReach());
        }
    }

    @Inject(method = "entityInteractionRange", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetEntityInteractionRange(CallbackInfoReturnable<Double> cir)
    {
        final ReachEvent.Entity reachEvent = new ReachEvent.Entity();
        EventBus.INSTANCE.dispatch(reachEvent);
        if (reachEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(reachEvent.getReach());
        }
    }
}
