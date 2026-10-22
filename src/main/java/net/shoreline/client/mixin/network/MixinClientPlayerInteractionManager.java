package net.shoreline.client.mixin.network;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.client.impl.event.network.AttackBlockEvent;
import net.shoreline.client.impl.event.network.BreakBlockEvent;
import net.shoreline.client.impl.event.network.InteractBlockEvent;
import net.shoreline.client.impl.event.network.InteractBorderEvent;
import net.shoreline.client.impl.event.network.ItemDesyncEvent;
import net.shoreline.client.impl.event.network.PacketSneakingEvent;
import net.shoreline.client.impl.event.network.StrafeFixEvent;
import net.shoreline.client.impl.event.network.SyncSelectedSlotEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class MixinClientPlayerInteractionManager implements Globals {
   @Shadow
   private GameType localPlayerMode;

   @Shadow
   protected abstract void ensureHasSentCarriedItem();

   @Shadow
   protected abstract void startPrediction(ClientLevel var1, PredictiveAction var2);

   @Inject(
      method = {"startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      BlockState state = mc.level.getBlockState(pos);
      AttackBlockEvent attackBlockEvent = new AttackBlockEvent(pos, state, direction);
      EventBus.INSTANCE.dispatch(attackBlockEvent);
      if (attackBlockEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      InteractBlockEvent interactBlockEvent = new InteractBlockEvent(player, hand, hitResult);
      EventBus.INSTANCE.dispatch(interactBlockEvent);
      if (interactBlockEvent.isCanceled()) {
         cir.setReturnValue(InteractionResult.SUCCESS);
         cir.cancel();
      }
   }

   @Redirect(
      method = {"useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z"
      )
   )
   private boolean hookInteractBlock$2(WorldBorder worldBorder, BlockPos pos) {
      InteractBorderEvent interactBorderEvent = new InteractBorderEvent();
      EventBus.INSTANCE.dispatch(interactBorderEvent);
      return interactBorderEvent.isCanceled() ? true : worldBorder.isWithinBounds(pos);
   }

   @Inject(
      method = {"destroyBlock(Lnet/minecraft/core/BlockPos;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      BreakBlockEvent breakBlockEvent = new BreakBlockEvent(pos);
      EventBus.INSTANCE.dispatch(breakBlockEvent);
      if (breakBlockEvent.isCanceled()) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }

   @Inject(
      method = {"useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookInteractItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      StrafeFixEvent strafeFixEvent = new StrafeFixEvent();
      EventBus.INSTANCE.dispatch(strafeFixEvent);
      if (strafeFixEvent.isCanceled()) {
         cir.cancel();
         if (this.localPlayerMode == GameType.SPECTATOR) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
         }

         this.ensureHasSentCarriedItem();
         MutableObject<InteractionResult> mutableObject = new MutableObject();
         this.startPrediction(
            mc.level,
            sequence -> {
               ServerboundUseItemPacket playerInteractItemC2SPacket = new ServerboundUseItemPacket(
                  hand,
                  sequence,
                  Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : player.getYRot(),
                  Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationPitch() : player.getXRot()
               );
               ItemStack itemStack = player.getItemInHand(hand);
               if (player.getCooldowns().isOnCooldown(itemStack.getItem())) {
                  mutableObject.setValue(InteractionResult.PASS);
                  return playerInteractItemC2SPacket;
               } else {
                  InteractionResultHolder<ItemStack> typedActionResult = itemStack.use(mc.level, player, hand);
                  ItemStack itemStack2 = typedActionResult.getObject();
                  if (itemStack2 != itemStack) {
                     player.setItemInHand(hand, itemStack2);
                  }

                  mutableObject.setValue(typedActionResult.getResult());
                  return playerInteractItemC2SPacket;
               }
            }
         );
         cir.setReturnValue((InteractionResult)mutableObject.getValue());
      }
   }

   @Redirect(
      method = {"performUseItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"
      )
   )
   private boolean hookRedirectInteractBlockInternal$shouldCancelInteraction(LocalPlayer player) {
      PacketSneakingEvent packetSneakingEvent = new PacketSneakingEvent();
      EventBus.INSTANCE.dispatch(packetSneakingEvent);
      return player.isShiftKeyDown() || packetSneakingEvent.isCanceled();
   }

   @Redirect(
      method = {"performUseItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
      )
   )
   private ItemStack hookRedirectInteractBlockInternal$getStackInHand(LocalPlayer entity, InteractionHand hand) {
      if (hand.equals(InteractionHand.OFF_HAND)) {
         return entity.getItemInHand(hand);
      } else {
         ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
         EventBus.INSTANCE.dispatch(itemDesyncEvent);
         return itemDesyncEvent.isCanceled() ? itemDesyncEvent.getServerItem() : entity.getItemInHand(InteractionHand.MAIN_HAND);
      }
   }

   @Redirect(
      method = {"performUseItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z",
         ordinal = 0
      )
   )
   private boolean hookRedirectInteractBlockInternal$getMainHandStack(ItemStack instance) {
      ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
      EventBus.INSTANCE.dispatch(itemDesyncEvent);
      return itemDesyncEvent.isCanceled() ? itemDesyncEvent.getServerItem().isEmpty() : instance.isEmpty();
   }

   @Inject(
      method = {"ensureHasSentCarriedItem()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSyncSelectedSlot(CallbackInfo ci) {
      SyncSelectedSlotEvent syncSelectedSlotEvent = new SyncSelectedSlotEvent();
      EventBus.INSTANCE.dispatch(syncSelectedSlotEvent);
      if (syncSelectedSlotEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
