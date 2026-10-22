package net.shoreline.client.mixin.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import net.minecraft.network.PacketDecoder;
import net.shoreline.client.impl.event.network.DecodePacketEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PacketDecoder.class})
public class MixinDecoderHandler {
   @Inject(
      method = {"decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/ConnectionProtocol;id()Ljava/lang/String;",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void hookDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
      DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
      EventBus.INSTANCE.dispatch(decodePacketEvent);
      if (decodePacketEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
