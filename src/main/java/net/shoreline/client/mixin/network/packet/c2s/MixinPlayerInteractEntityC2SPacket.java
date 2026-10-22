package net.shoreline.client.mixin.network.packet.c2s;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.shoreline.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.network.InteractType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ServerboundInteractPacket.class})
public abstract class MixinPlayerInteractEntityC2SPacket implements IPlayerInteractEntityC2SPacket, Globals {
   @Shadow
   @Final
   private int entityId;

   @Shadow
   public abstract void write(FriendlyByteBuf var1);

   @Override
   public Entity getEntity() {
      return mc.level == null ? null : mc.level.getEntity(this.entityId);
   }

   @Override
   public InteractType getType() {
      FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
      this.write(packetBuf);
      packetBuf.readVarInt();
      return packetBuf.readEnum(InteractType.class);
   }
}
