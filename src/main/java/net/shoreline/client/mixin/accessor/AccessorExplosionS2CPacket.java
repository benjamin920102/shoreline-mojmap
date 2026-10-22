package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundExplodePacket.class})
public interface AccessorExplosionS2CPacket {
   @Accessor("knockbackX")
   @Mutable
   void setPlayerVelocityX(float var1);

   @Accessor("knockbackY")
   @Mutable
   void setPlayerVelocityY(float var1);

   @Accessor("knockbackZ")
   @Mutable
   void setPlayerVelocityZ(float var1);
}
