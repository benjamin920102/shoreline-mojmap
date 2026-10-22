package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundPlayerPositionPacket.class})
public interface AccessorPlayerPositionLookS2CPacket {
   @Accessor("yRot")
   @Mutable
   void setYaw(float var1);

   @Accessor("xRot")
   @Mutable
   void setPitch(float var1);
}
