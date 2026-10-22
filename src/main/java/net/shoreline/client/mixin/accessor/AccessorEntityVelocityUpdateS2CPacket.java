package net.shoreline.client.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundSetEntityMotionPacket.class})
public interface AccessorEntityVelocityUpdateS2CPacket {
   @Accessor("xa")
   @Mutable
   void setVelocityX(int var1);

   @Accessor("ya")
   @Mutable
   void setVelocityY(int var1);

   @Accessor("za")
   @Mutable
   void setVelocityZ(int var1);
}
