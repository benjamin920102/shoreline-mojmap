package net.shoreline.client.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.entity.SwingEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.network.DismountVehicleEvent;
import net.shoreline.client.impl.event.network.EncodeYawEvent;
import net.shoreline.client.impl.event.network.MountJumpStrengthEvent;
import net.shoreline.client.impl.event.network.MovementPacketsEvent;
import net.shoreline.client.impl.event.network.MovementSlowdownEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.network.PushOutOfBlocksEvent;
import net.shoreline.client.impl.event.network.SetCurrentHandEvent;
import net.shoreline.client.impl.event.network.SprintCancelEvent;
import net.shoreline.client.impl.event.network.TickMovementEvent;
import net.shoreline.client.impl.imixin.IClientPlayerEntity;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LocalPlayer.class})
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer implements Globals, IClientPlayerEntity {
   @Shadow
   @Final
   public ClientPacketListener connection;
   @Shadow
   public double xLast;
   @Shadow
   public double yLast1;
   @Shadow
   public double zLast;
   @Shadow
   public Input input;
   @Shadow
   @Final
   protected Minecraft minecraft;
   @Shadow
   private boolean wasShiftKeyDown;
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private int positionReminder;
   @Shadow
   private boolean autoJumpEnabled;
   @Unique
   private boolean ticking;

   public MixinClientPlayerEntity() {
      super(Minecraft.getInstance().level, Minecraft.getInstance().player.getGameProfile());
   }

   @Shadow
   protected abstract void sendIsSprintingIfNeeded();

   @Shadow
   @Override
   public abstract boolean isShiftKeyDown();

   @Shadow
   protected abstract boolean isControlledCamera();

   @Shadow
   protected abstract void updateAutoJump(float var1, float var2);

   @Shadow
   @Override
   public abstract void tick();

   @Shadow
   protected abstract void sendPosition();

   @Inject(
      method = {"sendPosition()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSendMovementPackets(CallbackInfo ci) {
      PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
      playerUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(playerUpdateEvent);
      MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(
         mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()
      );
      EventBus.INSTANCE.dispatch(movementPacketsEvent);
      double x = movementPacketsEvent.getX();
      double y = movementPacketsEvent.getY();
      double z = movementPacketsEvent.getZ();
      float yaw = movementPacketsEvent.getYaw();
      float pitch = movementPacketsEvent.getPitch();
      boolean ground = movementPacketsEvent.getOnGround();
      EncodeYawEvent encodeYawEvent = new EncodeYawEvent();
      EventBus.INSTANCE.dispatch(encodeYawEvent);
      if (encodeYawEvent.isCanceled()) {
         yaw += 3.6E7F;
      }

      if (movementPacketsEvent.isCanceled() || encodeYawEvent.isCanceled()) {
         ci.cancel();
         this.sendIsSprintingIfNeeded();
         boolean bl = this.isShiftKeyDown();
         if (bl != this.wasShiftKeyDown) {
            Action mode = bl ? Action.PRESS_SHIFT_KEY : Action.RELEASE_SHIFT_KEY;
            this.connection.send(new ServerboundPlayerCommandPacket(this, mode));
            this.wasShiftKeyDown = bl;
         }

         if (this.isControlledCamera()) {
            double d = x - this.xLast;
            double e = y - this.yLast1;
            double f = z - this.zLast;
            double g = yaw - this.yRotLast;
            double h = pitch - this.xRotLast;
            this.positionReminder++;
            boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
            boolean bl3 = g != 0.0 || h != 0.0;
            if (this.isPassenger()) {
               Vec3 vec3d = this.getDeltaMovement();
               this.connection.send(new PosRot(vec3d.x, -999.0, vec3d.z, this.getYRot(), this.getXRot(), ground));
               bl2 = false;
            } else if (bl2 && bl3) {
               this.connection.send(new PosRot(x, y, z, yaw, pitch, ground));
            } else if (bl2) {
               this.connection.send(new Pos(x, y, z, ground));
            } else if (bl3) {
               this.connection.send(new Rot(yaw, pitch, ground));
            } else if (this.lastOnGround != this.onGround()) {
               this.connection.send(new StatusOnly(ground));
            }

            if (bl2) {
               this.xLast = x;
               this.yLast1 = y;
               this.zLast = z;
               this.positionReminder = 0;
            }

            if (bl3) {
               this.yRotLast = yaw;
               this.xRotLast = pitch;
            }

            this.lastOnGround = ground;
            this.autoJumpEnabled = this.minecraft.options.autoJump().get();
         }
      }

      playerUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(playerUpdateEvent);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
         shift = Shift.BEFORE,
         ordinal = 0
      )}
   )
   private void hookTickPre(CallbackInfo ci) {
      PlayerTickEvent playerTickEvent = new PlayerTickEvent();
      EventBus.INSTANCE.dispatch(playerTickEvent);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void hookTick(CallbackInfo ci) {
      if (!this.ticking) {
         TickMovementEvent tickMovementEvent = new TickMovementEvent();
         EventBus.INSTANCE.dispatch(tickMovementEvent);
         if (tickMovementEvent.isCanceled()) {
            for (int i = 0; i < tickMovementEvent.getIterations(); i++) {
               this.ticking = true;
               this.tick();
               this.ticking = false;
               this.sendPosition();
            }
         }
      }
   }

   @Inject(
      method = {"aiStep()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
         shift = Shift.AFTER
      )}
   )
   private void hookTickMovementPost(CallbackInfo ci) {
      MovementSlowdownEvent movementUpdateEvent = new MovementSlowdownEvent(this.input);
      EventBus.INSTANCE.dispatch(movementUpdateEvent);
   }

   @Inject(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookMove(MoverType movementType, Vec3 movement, CallbackInfo ci) {
      PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(movementType, movement);
      EventBus.INSTANCE.dispatch(playerMoveEvent);
      if (playerMoveEvent.isCanceled()) {
         ci.cancel();
         double d = this.getX();
         double e = this.getZ();
         super.move(movementType, playerMoveEvent.getMovement());
         this.updateAutoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      }
   }

   @Inject(
      method = {"moveTowardsClosestSpace(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
      PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
      EventBus.INSTANCE.dispatch(pushOutOfBlocksEvent);
      if (pushOutOfBlocksEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"startUsingItem(Lnet/minecraft/world/InteractionHand;)V"},
      at = {@At("HEAD")}
   )
   private void hookSetCurrentHand(InteractionHand hand, CallbackInfo ci) {
      SetCurrentHandEvent setCurrentHandEvent = new SetCurrentHandEvent(hand);
      EventBus.INSTANCE.dispatch(setCurrentHandEvent);
   }

   @Redirect(
      method = {"aiStep()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V",
         ordinal = 3
      )
   )
   private void hookSetSprinting(LocalPlayer instance, boolean b) {
      SprintCancelEvent sprintEvent = new SprintCancelEvent();
      EventBus.INSTANCE.dispatch(sprintEvent);
      if (sprintEvent.isCanceled()) {
         instance.setSprinting(true);
      } else {
         instance.setSprinting(b);
      }
   }

   @Redirect(
      method = {"handleConfusionTransitionEffect(Z)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"
      )
   )
   private Screen hookCurrentScreen(Minecraft instance) {
      return null;
   }

   @Inject(
      method = {"getJumpRidingScale()F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetMountJumpStrength(CallbackInfoReturnable<Float> cir) {
      MountJumpStrengthEvent mountJumpStrengthEvent = new MountJumpStrengthEvent();
      EventBus.INSTANCE.dispatch(mountJumpStrengthEvent);
      if (mountJumpStrengthEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(mountJumpStrengthEvent.getJumpStrength());
      }
   }

   @Inject(
      method = {"swing(Lnet/minecraft/world/InteractionHand;)V"},
      at = {@At("RETURN")}
   )
   private void hookSwingHand(InteractionHand hand, CallbackInfo ci) {
      SwingEvent swingEvent = new SwingEvent(hand);
      EventBus.INSTANCE.dispatch(swingEvent);
   }

   @Inject(
      method = {"removeVehicle()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDismountVehicle(CallbackInfo ci) {
      DismountVehicleEvent dismountVehicleEvent = new DismountVehicleEvent();
      EventBus.INSTANCE.dispatch(dismountVehicleEvent);
      if (dismountVehicleEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Override
   public float getLastSpoofedYaw() {
      return this.yRotLast;
   }

   @Override
   public float getLastSpoofedPitch() {
      return this.xRotLast;
   }
}
