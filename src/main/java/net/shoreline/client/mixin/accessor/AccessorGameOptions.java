package net.shoreline.client.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;

@Mixin(Options.class)
public interface AccessorGameOptions
{
    /**
     * @return
     */
    @Accessor("modelParts")
    @Mutable
    Set<PlayerModelPart> getPlayerModelParts();
}
