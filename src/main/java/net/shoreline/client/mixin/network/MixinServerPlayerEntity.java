package net.shoreline.client.mixin.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public class MixinServerPlayerEntity {
   @Inject(
      method = {"triggerDimensionChangeTriggers(Lnet/minecraft/server/level/ServerLevel;)V"},
      at = {@At("HEAD")}
   )
   private void hookMoveToWorld(ServerLevel origin, CallbackInfo ci) {
      LoadWorldEvent loadWorldEvent = new LoadWorldEvent();
      EventBus.INSTANCE.dispatch(loadWorldEvent);
   }
}
