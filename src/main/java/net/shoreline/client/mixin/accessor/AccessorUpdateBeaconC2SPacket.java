package net.shoreline.client.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;

/**
 *
 */
@Mixin(ServerboundSetBeaconPacket.class)
public interface AccessorUpdateBeaconC2SPacket
{
    /**
     * @param primaryEffectId
     */
    @Accessor("primary")
    @Mutable
    void setPrimaryEffect(Optional<MobEffect> primaryEffectId);

    /**
     * @param secondaryEffectId
     */
    @Accessor("secondary")
    @Mutable
    void setSecondaryEffect(Optional<MobEffect> secondaryEffectId);
}
