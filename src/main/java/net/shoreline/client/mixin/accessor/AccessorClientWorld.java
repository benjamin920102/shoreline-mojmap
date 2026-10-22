package net.shoreline.client.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ClientLevel.class)
public interface AccessorClientWorld
{
    /**
     * @param x
     * @param y
     * @param z
     * @param event
     * @param category
     * @param volume
     * @param pitch
     * @param useDistance
     * @param seed
     */
    @Invoker("playSound")
    void hookPlaySound(double x, double y, double z, SoundEvent event,
                       SoundSource category, float volume, float pitch,
                       boolean useDistance, long seed);

    /**
     * @return
     */
    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler hookGetPendingUpdateManager();
}
