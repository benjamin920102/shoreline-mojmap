package net.shoreline.client.mixin.render;

import net.minecraft.client.DeltaTracker.Timer;
import net.shoreline.client.impl.event.render.TickCounterEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Timer.class})
public class MixinRenderTickCounter {
   @Shadow
   private float deltaTicks;
   @Shadow
   private float deltaTickResidual;
   @Shadow
   private long lastMs;
   @Final
   @Shadow
   private float msPerTick;

   @Inject(
      method = {"advanceGameTime(J)I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TickCounterEvent tickCounterEvent = new TickCounterEvent();
      EventBus.INSTANCE.dispatch(tickCounterEvent);
      if (tickCounterEvent.isCanceled()) {
         this.deltaTicks = (float)(timeMillis - this.lastMs) / this.msPerTick * tickCounterEvent.getTicks();
         this.lastMs = timeMillis;
         this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
         int i = (int)(Object)this.deltaTickResidual;
         this.deltaTickResidual -= i;
         cir.setReturnValue(i);
      }
   }
}
