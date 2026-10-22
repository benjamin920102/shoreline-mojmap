package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Gavin
 * @since 1.0
 */
@Mixin(ClientboundSetEntityMotionPacket.class)
public interface AccessorEntityVelocityUpdateS2CPacket
{
    @Accessor("xa")
    @Mutable
    void setVelocityX(int velocityX);

    @Accessor("ya")
    @Mutable
    void setVelocityY(int velocityY);

    @Accessor("za")
    @Mutable
    void setVelocityZ(int velocityZ);
}
