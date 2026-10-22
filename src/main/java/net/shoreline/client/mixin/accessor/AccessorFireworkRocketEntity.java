package net.shoreline.client.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireworkRocketEntity.class)
public interface AccessorFireworkRocketEntity
{
    /**
     * @return
     */
    @Accessor("attachedToEntity")
    LivingEntity hookGetShooter();

    /**
     * @return
     */
    @Invoker("isAttachedToEntity")
    boolean hookWasShotByEntity();

    @Invoker("explode")
    void hookExplodeAndRemove();
}
