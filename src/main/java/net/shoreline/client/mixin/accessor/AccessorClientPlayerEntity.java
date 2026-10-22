package net.shoreline.client.mixin.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author linus
 * @see LocalPlayer
 * @since 1.0
 */
@Mixin(LocalPlayer.class)
public interface AccessorClientPlayerEntity
{
    /**
     * @return
     */
    @Accessor("xLast")
    double getLastX();

    /**
     * @return
     */
    @Accessor("yLast1")
    double getLastBaseY();

    /**
     * @return
     */
    @Accessor("zLast")
    double getLastZ();

    @Accessor("positionReminder")
    void hookSetTicksSinceLastPositionPacketSent(int ticksSinceLastPositionPacketSent);
}
