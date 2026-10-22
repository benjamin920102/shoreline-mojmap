package net.shoreline.client.mixin.accessor;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface AccessorLimbAnimator
{

    @Accessor("position")
    void hookSetPos(float pos);

    @Accessor("speed")
    void hookSetSpeed(float speed);
}
