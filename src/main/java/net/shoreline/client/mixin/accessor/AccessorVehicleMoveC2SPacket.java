package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMoveVehiclePacket.class)
public interface AccessorVehicleMoveC2SPacket
{
    @Accessor("y")
    void hookSetY(double y);
}
