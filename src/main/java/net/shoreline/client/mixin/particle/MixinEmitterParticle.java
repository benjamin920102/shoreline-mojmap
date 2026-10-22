package net.shoreline.client.mixin.particle;

import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.event.particle.EmitParticleEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackingEmitter.class)
public abstract class MixinEmitterParticle extends MixinParticle implements Globals
{
    @Shadow
    @Final
    private ParticleOptions particleType;

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    private int life;

    @Mutable
    @Shadow
    @Final
    private int lifeTime;

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci)
    {
        EmitParticleEvent emitParticleEvent = new EmitParticleEvent(particleType, 16, 30);
        EventBus.INSTANCE.dispatch(emitParticleEvent);
        if (emitParticleEvent.isCanceled())
        {
            ci.cancel();
            lifeTime = emitParticleEvent.getParticleTime();
            for (int i = 0; i < emitParticleEvent.getParticleCount(); ++i)
            {
                double f;
                double e;
                double d = RANDOM.nextFloat() * 2.0f - 1.0f;
                if (d * d + (e = (double)(RANDOM.nextFloat() * 2.0f - 1.0f)) * e + (f = (double)(RANDOM.nextFloat() * 2.0f - 1.0f)) * f > 1.0) continue;
                double g = this.entity.getX(d / 4.0);
                double h = this.entity.getY(0.5 + e / 4.0);
                double j = this.entity.getZ(f / 4.0);
                mc.level.addParticle(this.particleType, false, g, h, j, d, e + 0.2, f);
            }
            ++this.life;
            if (this.life >= this.lifeTime)
            {
                this.markDead();
            }
        }
    }
}
