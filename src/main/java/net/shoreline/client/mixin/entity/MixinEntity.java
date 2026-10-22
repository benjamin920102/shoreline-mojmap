package net.shoreline.client.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.camera.EntityCameraPositionEvent;
import net.shoreline.client.impl.event.entity.EntityRotationVectorEvent;
import net.shoreline.client.impl.event.entity.LookDirectionEvent;
import net.shoreline.client.impl.event.entity.RenderFireEntityEvent;
import net.shoreline.client.impl.event.entity.SetBBEvent;
import net.shoreline.client.impl.event.entity.SlowMovementEvent;
import net.shoreline.client.impl.event.entity.UpdateVelocityEvent;
import net.shoreline.client.impl.event.entity.VelocityEvent;
import net.shoreline.client.impl.event.entity.VelocityMultiplierEvent;
import net.shoreline.client.impl.event.entity.decoration.TeamColorEvent;
import net.shoreline.client.impl.event.entity.player.PushEntityEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinEntity implements Globals {
   @Shadow
   public boolean hasImpulse;
   @Shadow
   private Vec3 deltaMovement;
   @Shadow
   protected Vec3 stuckSpeedMultiplier;
   @Shadow
   public float fallDistance;

   @Shadow
   private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
      return null;
   }

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract void setDeltaMovement(Vec3 var1);

   @Shadow
   public abstract boolean isSprinting();

   @Inject(
      method = {"getViewVector(F)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookGetCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3> info) {
      EntityRotationVectorEvent event = new EntityRotationVectorEvent(tickDelta, (Entity)(Object)this, (Vec3)info.getReturnValue());
      EventBus.INSTANCE.dispatch(event);
      info.setReturnValue(event.getPosition());
   }

   @Inject(
      method = {"collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")}
   )
   public void hookMove(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
      if ((Object)this != mc.player) {
      }
   }

   @Inject(
      method = {"makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSlowMovement(BlockState state, Vec3 multiplier, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
         EventBus.INSTANCE.dispatch(slowMovementEvent);
         if (slowMovementEvent.isCanceled()) {
            ci.cancel();
            this.fallDistance = 0.0F;
            this.stuckSpeedMultiplier = multiplier.scale(slowMovementEvent.getMultiplier());
         }
      }
   }

   @Redirect(
      method = {"getBlockSpeedFactor()F"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/ block/Block;"
      )
   )
   private Block hookGetVelocityMultiplier(BlockState instance) {
      if ((Object)this != mc.player) {
         return instance.getBlock();
      } else {
         VelocityMultiplierEvent velocityMultiplierEvent = new VelocityMultiplierEvent(instance);
         EventBus.INSTANCE.dispatch(velocityMultiplierEvent);
         return velocityMultiplierEvent.isCanceled() ? Blocks.DIRT : instance.getBlock();
      }
   }

   @Inject(
      method = {"moveRelative(FLnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookUpdateVelocity(float speed, Vec3 movementInput, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(
            movementInput, speed, mc.player.getYRot(), getInputVector(movementInput, speed, mc.player.getYRot())
         );
         EventBus.INSTANCE.dispatch(updateVelocityEvent);
         if (updateVelocityEvent.isCanceled()) {
            ci.cancel();
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(updateVelocityEvent.getVelocity()));
         }
      }
   }

   @Inject(
      method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
      PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity)(Object)this, entity);
      EventBus.INSTANCE.dispatch(pushEntityEvent);
      if (pushEntityEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getTeamColor()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
      TeamColorEvent teamColorEvent = new TeamColorEvent((Entity)(Object)this);
      EventBus.INSTANCE.dispatch(teamColorEvent);
      if (teamColorEvent.isCanceled()) {
         cir.setReturnValue(teamColorEvent.getColor());
         cir.cancel();
      }
   }

   @Inject(
      method = {"turn(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         LookDirectionEvent lookDirectionEvent = new LookDirectionEvent((Entity)(Object)this, cursorDeltaX, cursorDeltaY);
         EventBus.INSTANCE.dispatch(lookDirectionEvent);
         if (lookDirectionEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookCameraPositionVec(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
      EntityCameraPositionEvent cameraPositionEvent = new EntityCameraPositionEvent((Vec3)cir.getReturnValue(), (Entity)(Object)this, tickDelta);
      EventBus.INSTANCE.dispatch(cameraPositionEvent);
      cir.setReturnValue(cameraPositionEvent.getPosition());
   }

   @Inject(
      method = {"setBoundingBox(Lnet/minecraft/world/phys/AABB;)V"},
      at = {@At("HEAD")}
   )
   private void hookSetBoundingBox(AABB boundingBox, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SetBBEvent setBBEvent = new SetBBEvent(boundingBox);
         EventBus.INSTANCE.dispatch(setBBEvent);
      }
   }

   @Inject(
      method = {"displayFireAnimation()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDoesRenderOnFire(CallbackInfoReturnable<Boolean> cir) {
      RenderFireEntityEvent renderFireEntityEvent = new RenderFireEntityEvent();
      EventBus.INSTANCE.dispatch(renderFireEntityEvent);
      if (renderFireEntityEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSetVelocity(Vec3 velocity, CallbackInfo ci) {
      VelocityEvent velocityEvent = new VelocityEvent(velocity);
      EventBus.INSTANCE.dispatch(velocityEvent);
      if (velocityEvent.isCanceled()) {
         ci.cancel();
         this.deltaMovement = velocityEvent.getVelocity();
      }
   }
}
