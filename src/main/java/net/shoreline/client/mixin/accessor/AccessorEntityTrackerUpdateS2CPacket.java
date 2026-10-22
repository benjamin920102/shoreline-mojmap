package net.shoreline.client.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(ClientboundSetEntityDataPacket.class)
public interface AccessorEntityTrackerUpdateS2CPacket
{
    @Accessor("packedItems")
    @Mutable
    void hookSetTrackedValues(List<SynchedEntityData.DataValue<?>> trackedValues);
}
