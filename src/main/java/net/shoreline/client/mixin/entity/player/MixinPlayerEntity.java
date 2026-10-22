package net.shoreline.client.mixin.entity.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.entity.player.LedgeClipEvent;
import net.shoreline.client.impl.event.entity.player.PlayerJumpEvent;
import net.shoreline.client.impl.event.entity.player.PushFluidsEvent;
import net.shoreline.client.impl.event.entity.player.SprintResetEvent;
import net.shoreline.client.impl.event.entity.player.TravelEvent;
import net.shoreline.client.impl.event.network.ReachEvent;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class MixinPlayerEntity extends LivingEntity implements Globals {
   protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
      super(entityType, world);
   }

   @Shadow
   @Override
   public abstract void travel(Vec3 var1);

   @Inject(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTravelHead(Vec3 movementInput, CallbackInfo ci) {
      TravelEvent travelEvent = new TravelEvent(movementInput);
      EventBus.INSTANCE.dispatch(travelEvent);
      if (travelEvent.isCanceled()) {
         this.move(MoverType.SELF, this.getDeltaMovement());
         ci.cancel();
      }
   }

   @Inject(
      method = {"isPushedByFluid()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         PushFluidsEvent pushFluidsEvent = new PushFluidsEvent();
         EventBus.INSTANCE.dispatch(pushFluidsEvent);
         if (pushFluidsEvent.isCanceled()) {
            cir.setReturnValue(false);
            cir.cancel();
         }
      }
   }

   @Inject(
      method = {"jumpFromGround()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookJumpPre(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
         playerJumpEvent.setStage(StageEvent.EventStage.PRE);
         EventBus.INSTANCE.dispatch(playerJumpEvent);
         if (playerJumpEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"jumpFromGround()V"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookJumpPost(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         PlayerJumpEvent playerJumpEvent = new PlayerJumpEvent();
         playerJumpEvent.setStage(StageEvent.EventStage.POST);
         EventBus.INSTANCE.dispatch(playerJumpEvent);
      }
   }

   @Redirect(
      method = {"attack(Lnet/minecraft/world/entity/Entity;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )
   )
   private void hookAttack(Player playerEntity, Vec3 movementInput) {
      if (playerEntity instanceof LocalPlayer) {
         SprintResetEvent sprintResetEvent = new SprintResetEvent();
         EventBus.INSTANCE.dispatch(sprintResetEvent);
         if (!sprintResetEvent.isCanceled()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         }
      }
   }

   @Redirect(
      method = {"attack(Lnet/minecraft/world/entity/Entity;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"
      )
   )
   private void hookAttack$1(Player instance, boolean b) {
      if (instance instanceof LocalPlayer) {
         SprintResetEvent sprintResetEvent = new SprintResetEvent();
         EventBus.INSTANCE.dispatch(sprintResetEvent);
         if (!sprintResetEvent.isCanceled()) {
            mc.player.setSprinting(false);
         }
      }
   }

   @Inject(
      method = {"isStayingOnGroundSurface()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
      LedgeClipEvent ledgeClipEvent = new LedgeClipEvent();
      EventBus.INSTANCE.dispatch(ledgeClipEvent);
      if (ledgeClipEvent.isCanceled()) {
         cir.setReturnValue(ledgeClipEvent.isClipped());
      }
   }

   @Redirect(
      method = {"updatePlayerPose()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;isFallFlying()Z"
      )
   )
   private boolean hookUpdatePose(Player instance) {
      ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
      EventBus.INSTANCE.dispatch(elytraTransformEvent);
      return elytraTransformEvent.isCanceled() ? false : instance.isFallFlying();
   }

   @Inject(
      method = {"blockInteractionRange()D"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
      ReachEvent.Block reachEvent = new ReachEvent.Block();
      EventBus.INSTANCE.dispatch(reachEvent);
      if (reachEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(reachEvent.getReach());
      }
   }

   @Inject(
      method = {"entityInteractionRange()D"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
      ReachEvent.Entity reachEvent = new ReachEvent.Entity();
      EventBus.INSTANCE.dispatch(reachEvent);
      if (reachEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(reachEvent.getReach());
      }
   }
}
