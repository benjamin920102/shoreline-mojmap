package net.shoreline.client.mixin.network.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.shoreline.client.impl.event.network.server.FinishMiningServerEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayerGameMode.class})
public class MixinServerPlayerInteractionManager {
   @Inject(
      method = {"destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookFinishMining(BlockPos pos, int sequence, String reason, CallbackInfo ci) {
      FinishMiningServerEvent finishMiningServerEvent = new FinishMiningServerEvent();
      EventBus.INSTANCE.dispatch(finishMiningServerEvent);
      if (finishMiningServerEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
