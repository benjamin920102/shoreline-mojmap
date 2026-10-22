package net.shoreline.client.mixin.entity.projectile;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.entity.FireworkTickEvent;
import net.shoreline.client.impl.event.entity.projectile.FireworkVelocityEvent;
import net.shoreline.client.impl.event.entity.projectile.RemoveFireworkEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity implements Globals
{
    //
    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void hookTick(CallbackInfo ci)
    {
        FireworkTickEvent event = new FireworkTickEvent((FireworkRocketEntity) (Object) this);
        EventBus.INSTANCE.dispatch(event);
        if (event.isCanceled())
        {
            ci.cancel();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hookSetVelocity(LivingEntity entity, Vec3 velocity)
    {
        if (entity instanceof LocalPlayer)
        {
            FireworkVelocityEvent fireworkVelocityEvent = new FireworkVelocityEvent();
            EventBus.INSTANCE.dispatch(fireworkVelocityEvent);
            if (!fireworkVelocityEvent.isCanceled())
            {
                entity.setDeltaMovement(velocity);
            }
        }
    }

    /**
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FireworkRocketEntity;updateRotation()V", shift = At.Shift.AFTER), cancellable = true)
    private void hookTickPre(CallbackInfo ci)
    {
        if (life <= lifetime)
        {
            return;
        }
        FireworkRocketEntity rocketEntity = ((FireworkRocketEntity) (Object) this);
        RemoveFireworkEvent removeFireworkEvent = new RemoveFireworkEvent(rocketEntity);
        EventBus.INSTANCE.dispatch(removeFireworkEvent);
        if (removeFireworkEvent.isCanceled())
        {
            ci.cancel();
            if (life == 0 && !rocketEntity.isSilent())
            {
                mc.level.playSound(null, rocketEntity.getX(), rocketEntity.getY(), rocketEntity.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0f, 1.0f);
            }
            ++life;
            if (mc.level.isClientSide && life % 2 < 2)
            {
                mc.level.addParticle(ParticleTypes.FIREWORK, rocketEntity.getX(), rocketEntity.getY(), rocketEntity.getZ(), mc.level.random.nextGaussian() * 0.05, -rocketEntity.getDeltaMovement().y * 0.5, mc.level.random.nextGaussian() * 0.05);
            }
        }
    }
}
