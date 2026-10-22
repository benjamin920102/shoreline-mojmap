package net.shoreline.client.mixin.accessor;

import java.util.Optional;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerboundSetBeaconPacket.class})
public interface AccessorUpdateBeaconC2SPacket {
   @Accessor("primary")
   @Mutable
   void setPrimaryEffect(Optional<MobEffect> var1);

   @Accessor("secondary")
   @Mutable
   void setSecondaryEffect(Optional<MobEffect> var1);
}
