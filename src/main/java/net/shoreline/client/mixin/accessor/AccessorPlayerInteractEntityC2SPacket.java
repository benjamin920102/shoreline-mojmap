package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface AccessorPlayerInteractEntityC2SPacket
{
    @Accessor("entityId")
    @Mutable
    void hookSetEntityId(int entityId);
}
