package net.shoreline.client.mixin.particle;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.impl.event.particle.BlockBreakParticleEvent;
import net.shoreline.client.impl.event.particle.ParticleEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ParticleEngine.class)
public class MixinParticleManager
{
    @Shadow
    @Final
    private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

    /**
     * @param parameters
     * @param x
     * @param y
     * @param z
     * @param velocityX
     * @param velocityY
     * @param velocityZ
     * @param cir
     */
    @Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At(value =
            "HEAD"), cancellable = true)
    private void hookAddParticle(ParticleOptions parameters, double x,
                                 double y, double z, double velocityX,
                                 double velocityY, double velocityZ,
                                 CallbackInfoReturnable<Particle> cir)
    {
        ParticleEvent particleEvent = new ParticleEvent(parameters);
        EventBus.INSTANCE.dispatch(particleEvent);
        if (particleEvent.isCanceled())
        {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    /**
     * @param entity
     * @param parameters
     * @param maxAge
     * @param ci
     */
    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookAddEmitter(Entity entity, ParticleOptions parameters, int maxAge, CallbackInfo ci)
    {
        ParticleEvent.Emitter particleEvent =
                new ParticleEvent.Emitter(parameters);
        EventBus.INSTANCE.dispatch(particleEvent);
        if (particleEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        BlockBreakParticleEvent blockBreakParticleEvent = new BlockBreakParticleEvent();
        EventBus.INSTANCE.dispatch(blockBreakParticleEvent);
        if (blockBreakParticleEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci)
    {
        BlockBreakParticleEvent blockBreakParticleEvent = new BlockBreakParticleEvent();
        EventBus.INSTANCE.dispatch(blockBreakParticleEvent);
        if (blockBreakParticleEvent.isCanceled())
        {
            ci.cancel();
        }
    }
}
