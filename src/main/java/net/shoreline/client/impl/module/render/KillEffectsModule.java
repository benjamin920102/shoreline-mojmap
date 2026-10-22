package net.shoreline.client.impl.module.render;

import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.HashMap;
import java.util.Map;

public class KillEffectsModule extends ToggleModule
{

    Config<KillEffect> killEffectConfig = register(new EnumConfig<>("Effect", "The kill effect to apply", KillEffect.LIGHTNING, KillEffect.values()));
    Config<Integer> strikesConfig = register(new NumberConfig<>("Strikes", "The number of lightning strikes", 1, 1, 5, () -> killEffectConfig.getValue() == KillEffect.LIGHTNING));

    private final Map<Entity, Long> lastAttackedEntities = new HashMap<>();

    public KillEffectsModule()
    {
        super("KillEffects", "Adds effects to player deaths", ModuleCategory.RENDER);
    }

    @EventListener
    public void onEntityDeath(EntityDeathEvent event)
    {
        if (event.getEntity() == mc.player || !(event.getEntity() instanceof Player player) || !wasLastAttackedByPlayer(player))
        {
            return;
        }
        switch (killEffectConfig.getValue())
        {
            case LIGHTNING ->
            {
                for (int i = 0; i < strikesConfig.getValue(); i++)
                {
                    LightningBolt lightningEntity = new LightningBolt(EntityType.LIGHTNING_BOLT, mc.level);
                    lightningEntity.setPosRaw(player.getX(), player.getY(), player.getZ());
                    mc.level.addEntity(lightningEntity);
                }
            }
            case FIREWORK ->
            {
                fireworkExplode(player.getX(), player.getY(), player.getZ(), 0.5, 4);
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }
        if (event.getPacket() instanceof ClientboundDamageEventPacket packet && packet.sourceCauseId() == mc.player.getId())
        {
            lastAttackedEntities.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 5000);
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity == null)
            {
                return;
            }
            lastAttackedEntities.put(entity, System.currentTimeMillis());
        }
    }

    private void fireworkExplode(double x, double y, double z, double size, int amount)
    {
        double d = x;
        double e = y;
        double f = z;
        for (int i = -amount; i <= amount; ++i)
        {
            for (int j = -amount; j <= amount; ++j)
            {
                for (int k = -amount; k <= amount; ++k)
                {
                    double g = (double) j + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double h = (double) i + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double l = (double) k + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double m = Math.sqrt(g * g + h * h + l * l) / size + RANDOM.nextGaussian() * 0.05;
                    addExplosionParticle(d, e, f, g / m, h / m, l / m);
                    if (i == -amount || i == amount || j == -amount || j == amount) continue;
                    k += amount * 2 - 1;
                }
            }
        }
    }

    private void addExplosionParticle(double x, double y, double z, double velocityX, double velocityY, double velocityZ)
    {
        if (mc.particleEngine == null || mc.level == null)
        {
            return;
        }
        FireworkParticles.SparkParticle explosion = (FireworkParticles.SparkParticle) mc.particleEngine.createParticle(ParticleTypes.FIREWORK, x, y, z, velocityX, velocityY, velocityZ);
        if (explosion == null)
        {
            return;
        }
        explosion.setTrail(false);
        explosion.setTwinkle(false);
        explosion.setColor(ColorsModule.getInstance().getRGB());
    }

    private boolean wasLastAttackedByPlayer(Entity entity)
    {
        Long lastAttackedTime = lastAttackedEntities.get(entity);
        return lastAttackedTime != null && (System.currentTimeMillis() - lastAttackedTime) < 5000;
    }

    private enum KillEffect
    {
        LIGHTNING,
        FIREWORK
    }
}