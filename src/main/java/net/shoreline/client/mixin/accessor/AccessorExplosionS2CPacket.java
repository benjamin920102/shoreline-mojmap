package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author linus
 * @see AccessorExplosionS2CPacket
 * @since 1.0
 */
@Mixin(ClientboundExplodePacket.class)
public interface AccessorExplosionS2CPacket
{
    /**
     * @param playerVelocityX
     */
    @Accessor("knockbackX")
    @Mutable
    void setPlayerVelocityX(float playerVelocityX);

    /**
     * @param playerVelocityY
     */
    @Accessor("knockbackY")
    @Mutable
    void setPlayerVelocityY(float playerVelocityY);

    /**
     * @param playerVelocityZ
     */
    @Accessor("knockbackZ")
    @Mutable
    void setPlayerVelocityZ(float playerVelocityZ);
}
