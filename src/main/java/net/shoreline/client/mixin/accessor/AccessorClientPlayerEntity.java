package net.shoreline.client.mixin.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LocalPlayer.class})
public interface AccessorClientPlayerEntity {
   @Accessor("xLast")
   double getLastX();

   @Accessor("yLast1")
   double getLastBaseY();

   @Accessor("zLast")
   double getLastZ();

   @Accessor("positionReminder")
   void hookSetTicksSinceLastPositionPacketSent(int var1);
}
