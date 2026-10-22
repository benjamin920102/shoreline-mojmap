package net.shoreline.client.mixin.entity;

import baritone.api.BaritoneAPI;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.ShorelineMod;
import net.shoreline.client.impl.event.entity.ConsumeItemEvent;
import net.shoreline.client.impl.event.entity.EntityTravelEvent;
import net.shoreline.client.impl.event.entity.JumpDelayEvent;
import net.shoreline.client.impl.event.entity.JumpRotationEvent;
import net.shoreline.client.impl.event.entity.LevitationEvent;
import net.shoreline.client.impl.event.entity.PlayerClimbEvent;
import net.shoreline.client.impl.event.entity.PlayerDamageEvent;
import net.shoreline.client.impl.event.entity.StatusEffectEvent;
import net.shoreline.client.impl.event.entity.StepEvent;
import net.shoreline.client.impl.event.entity.SwingSpeedEvent;
import net.shoreline.client.impl.event.entity.UpdateServerPositionEvent;
import net.shoreline.client.impl.event.render.entity.ElytraTransformEvent;
import net.shoreline.client.impl.module.movement.ElytraFlyModule;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity extends MixinEntity implements Globals {
   @Shadow
   protected ItemStack useItem;
   @Shadow
   private int noJumpDelay;
   @Unique
   private boolean prevFlying;

   @Shadow
   public abstract float getViewYRot(float var1);

   @Shadow
   protected abstract float getJumpPower();

   @Shadow
   public abstract boolean hasEffect(Holder<MobEffect> var1);

   @Inject(
      method = {"getCurrentSwingDuration()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
      SwingSpeedEvent swingSpeedEvent = new SwingSpeedEvent();
      EventBus.INSTANCE.dispatch(swingSpeedEvent);
      if (swingSpeedEvent.isCanceled()) {
         if (swingSpeedEvent.getSelfOnly() && (Object)this != mc.player) {
            return;
         }

         cir.cancel();
         cir.setReturnValue(swingSpeedEvent.getSwingSpeed());
      }
   }

   @ModifyExpressionValue(
      method = {"jumpFromGround()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
      )}
   )
   private float hookJump$getYaw(float original) {
      if ((Object)this != mc.player) {
         return original;
      } else {
         JumpRotationEvent jumpRotationEvent = new JumpRotationEvent(original);
         EventBus.INSTANCE.dispatch(jumpRotationEvent);
         return jumpRotationEvent.isCanceled() ? jumpRotationEvent.getYaw() : original;
      }
   }

   @Redirect(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"
      )
   )
   private boolean hookHasStatusEffect(LivingEntity instance, Holder<MobEffect> effect) {
      if (instance.equals(mc.player) && effect == MobEffects.LEVITATION) {
         LevitationEvent levitationEvent = new LevitationEvent();
         EventBus.INSTANCE.dispatch(levitationEvent);
         return !levitationEvent.isCanceled() && this.hasEffect(effect);
      } else {
         return this.hasEffect(effect);
      }
   }

   @Inject(
      method = {"completeUsingItem()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
         shift = Shift.AFTER
      )}
   )
   private void hookConsumeItem(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         ConsumeItemEvent consumeItemEvent = new ConsumeItemEvent(this.useItem);
         EventBus.INSTANCE.dispatch(consumeItemEvent);
      }
   }

   @Inject(
      method = {"aiStep()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTickMovement(CallbackInfo ci) {
      JumpDelayEvent jumpDelayEvent = new JumpDelayEvent();
      EventBus.INSTANCE.dispatch(jumpDelayEvent);
      if (jumpDelayEvent.isCanceled()) {
         this.noJumpDelay = 0;
      }
   }

   @Inject(
      method = {"addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"},
      at = {@At("HEAD")}
   )
   private void hookAddStatusEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         StatusEffectEvent.Add statusEffectEvent = new StatusEffectEvent.Add(effect);
         EventBus.INSTANCE.dispatch(statusEffectEvent);
      }
   }

   @Inject(
      method = {"forceAddEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")}
   )
   private void hookAddStatusEffect$1(MobEffectInstance effect, Entity source, CallbackInfo ci) {
      if ((Object)this == mc.player) {
         StatusEffectEvent.Add statusEffectEvent = new StatusEffectEvent.Add(effect);
         EventBus.INSTANCE.dispatch(statusEffectEvent);
      }
   }

   @Inject(
      method = {"removeEffectNoUpdate(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"},
      at = {@At("HEAD")}
   )
   private void hookRemoveStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
      if ((Object)this == mc.player) {
         StatusEffectEvent.Remove statusEffectEvent = new StatusEffectEvent.Remove(effect);
         EventBus.INSTANCE.dispatch(statusEffectEvent);
      }
   }

   @Inject(
      method = {"isFallFlying()Z"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void hookIsFallFlying(CallbackInfoReturnable<Boolean> cir) {
      if (!ShorelineMod.isBaritonePresent() || !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
         boolean flying = (Boolean)cir.getReturnValue();
         boolean stoppedFlying = this.prevFlying && !flying;
         if (ElytraFlyModule.getInstance().isEnabled() && ElytraFlyModule.getInstance().isBounce() && stoppedFlying) {
            mc.player.startFallFlying();
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
            cir.setReturnValue(true);
         }

         this.prevFlying = flying;
      }
   }

   @Inject(
      method = {"actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"},
      at = {@At("HEAD")}
   )
   private void hookDamage(DamageSource source, float amount, CallbackInfo ci) {
      if (mc.player != null) {
         if (source.getEntity() != null && source.getEntity().getName().getString().equalsIgnoreCase(mc.player.getName().getString())) {
            PlayerDamageEvent playerDamageEvent = new PlayerDamageEvent((LivingEntity)(Object)this);
            EventBus.INSTANCE.dispatch(playerDamageEvent);
         }
      }
   }

   @Inject(
      method = {"lerpTo(DDDFFI)V"},
      at = {@At("HEAD")}
   )
   private void hookUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
      UpdateServerPositionEvent updateServerPositionEvent = new UpdateServerPositionEvent((LivingEntity)(Object)this, x, y, z, yaw, pitch);
      EventBus.INSTANCE.dispatch(updateServerPositionEvent);
   }

   @Inject(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTravelPre(Vec3 movementInput, CallbackInfo ci) {
      EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity)(Object)this, true);
      EventBus.INSTANCE.dispatch(entityTravelEvent);
      if (entityTravelEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("RETURN")}
   )
   private void hookTravelPost(Vec3 movementInput, CallbackInfo ci) {
      EntityTravelEvent entityTravelEvent = new EntityTravelEvent((LivingEntity)(Object)this, false);
      EventBus.INSTANCE.dispatch(entityTravelEvent);
   }

   @Redirect(
      method = {"tick()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"
      )
   )
   private boolean hookTick(LivingEntity instance) {
      ElytraTransformEvent elytraTransformEvent = new ElytraTransformEvent(instance);
      EventBus.INSTANCE.dispatch(elytraTransformEvent);
      return elytraTransformEvent.isCanceled() ? false : instance.isFallFlying();
   }

   @Inject(
      method = {"maxUpStep()F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetStepHeight(CallbackInfoReturnable<Float> cir) {
      StepEvent stepEvent = new StepEvent((LivingEntity)(Object)this, cir.getReturnValueF());
      EventBus.INSTANCE.dispatch(stepEvent);
      if (stepEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(stepEvent.getStepHeight());
      }
   }

   @Inject(
      method = {"onClimbable()Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsClimbing(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player) {
         PlayerClimbEvent playerClimbEvent = new PlayerClimbEvent();
         EventBus.INSTANCE.dispatch(playerClimbEvent);
         if (playerClimbEvent.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(false);
         }
      }
   }
}
