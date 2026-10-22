package net.shoreline.client.mixin.accessor;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface AccessorAbstractBlock
{
    /**
     * @param slipperiness
     */
    @Accessor("friction")
    @Mutable
    void setSlipperiness(float slipperiness);
}
