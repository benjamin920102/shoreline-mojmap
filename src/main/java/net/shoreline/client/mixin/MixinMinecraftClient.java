package net.shoreline.client.mixin;

import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft.GameLoadCookie;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shoreline.client.impl.event.AttackCooldownEvent;
import net.shoreline.client.impl.event.ClientTickEvent;
import net.shoreline.client.impl.event.EntityOutlineEvent;
import net.shoreline.client.impl.event.FinishLoadingEvent;
import net.shoreline.client.impl.event.FramerateLimitEvent;
import net.shoreline.client.impl.event.ItemMultitaskEvent;
import net.shoreline.client.impl.event.ItemUseEvent;
import net.shoreline.client.impl.event.ResolutionEvent;
import net.shoreline.client.impl.event.RunTickEvent;
import net.shoreline.client.impl.event.ScreenOpenEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.gui.screen.pack.RefreshPacksEvent;
import net.shoreline.client.impl.imixin.IMinecraftClient;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.event.StageEvent;
import org.jetbrains.annotations.Nullable;
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

@Mixin({Minecraft.class})
public abstract class MixinMinecraftClient implements IMinecraftClient {
   @Shadow
   public ClientLevel level;
   @Shadow
   public LocalPlayer player;
   @Shadow
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Shadow
   protected int missTime;
   @Unique
   private boolean leftClick;
   @Unique
   private boolean rightClick;
   @Unique
   private boolean doAttackCalled;
   @Unique
   private boolean doItemUseCalled;
   @Shadow
   @Final
   private Window window;
   @Unique
   private final List<Integer> deadList = new ArrayList<>();

   @Shadow
   protected abstract void startUseItem();

   @Shadow
   protected abstract boolean startAttack();

   @Override
   public void leftClick() {
      this.leftClick = true;
   }

   @Override
   public void rightClick() {
      this.rightClick = true;
   }

   @Inject(
      method = {"run()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;runTick(Z)V",
         shift = Shift.BEFORE
      )}
   )
   private void hookRun(CallbackInfo ci) {
      RunTickEvent runTickEvent = new RunTickEvent();
      EventBus.INSTANCE.dispatch(runTickEvent);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;"
      )}
   )
   private void hookTick(CallbackInfo ci) {
      ClientTickEvent tickEvent = new ClientTickEvent();
      EventBus.INSTANCE.dispatch(tickEvent);
   }

   @Inject(
      method = {"buildInitialScreens(Lnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/lang/Runnable;"},
      at = {@At("RETURN")}
   )
   private void hookOnInitFinished(GameLoadCookie loadingContext, CallbackInfoReturnable<Runnable> cir) {
      FinishLoadingEvent finishLoadingEvent = new FinishLoadingEvent();
      EventBus.INSTANCE.dispatch(finishLoadingEvent);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   private void hookTickPre(CallbackInfo ci) {
      this.doAttackCalled = false;
      this.doItemUseCalled = false;
      if (this.player != null && this.level != null) {
         TickEvent tickPreEvent = new TickEvent();
         tickPreEvent.setStage(StageEvent.EventStage.PRE);
         EventBus.INSTANCE.dispatch(tickPreEvent);
      }

      if (this.gameMode != null) {
         if (this.leftClick && !this.doAttackCalled) {
            this.startAttack();
         }

         if (this.rightClick && !this.doItemUseCalled) {
            this.startUseItem();
         }

         this.leftClick = false;
         this.rightClick = false;
      }
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("TAIL")}
   )
   private void hookTickPost(CallbackInfo ci) {
      if (this.player != null && this.level != null) {
         TickEvent tickPostEvent = new TickEvent();
         tickPostEvent.setStage(StageEvent.EventStage.POST);
         EventBus.INSTANCE.dispatch(tickPostEvent);

         for (Entity entity : this.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity e) {
               if (e.isDeadOrDying() && !this.deadList.contains(e.getId())) {
                  EntityDeathEvent entityDeathEvent = new EntityDeathEvent(e);
                  EventBus.INSTANCE.dispatch(entityDeathEvent);
                  this.deadList.add(e.getId());
               } else if (!e.isDeadOrDying()) {
                  this.deadList.remove(Integer.valueOf(e.getId()));
               }
            }
         }
      }
   }

   @Inject(
      method = {"setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"},
      at = {@At("TAIL")}
   )
   private void hookSetScreen(Screen screen, CallbackInfo ci) {
      ScreenOpenEvent screenOpenEvent = new ScreenOpenEvent(screen);
      EventBus.INSTANCE.dispatch(screenOpenEvent);
   }

   @Inject(
      method = {"startUseItem()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDoItemUse(CallbackInfo ci) {
      this.doItemUseCalled = true;
      ItemUseEvent itemUseEvent = new ItemUseEvent();
      EventBus.INSTANCE.dispatch(itemUseEvent);
      if (itemUseEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"startAttack()Z"},
      at = {@At("HEAD")}
   )
   private void hookDoAttack(CallbackInfoReturnable<Boolean> cir) {
      this.doAttackCalled = true;
      AttackCooldownEvent attackCooldownEvent = new AttackCooldownEvent();
      EventBus.INSTANCE.dispatch(attackCooldownEvent);
      if (attackCooldownEvent.isCanceled()) {
         this.missTime = 0;
      }
   }

   @Redirect(
      method = {"continueAttack(Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
      )
   )
   private boolean hookIsUsingItem(LocalPlayer instance) {
      ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
      EventBus.INSTANCE.dispatch(itemMultitaskEvent);
      return !itemMultitaskEvent.isCanceled() && instance.isUsingItem();
   }

   @Redirect(
      method = {"startUseItem()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"
      )
   )
   private boolean hookIsBreakingBlock(MultiPlayerGameMode instance) {
      ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
      EventBus.INSTANCE.dispatch(itemMultitaskEvent);
      return !itemMultitaskEvent.isCanceled() && instance.isDestroying();
   }

   @Inject(
      method = {"getFramerateLimit()I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
      FramerateLimitEvent framerateLimitEvent = new FramerateLimitEvent();
      EventBus.INSTANCE.dispatch(framerateLimitEvent);
      if (framerateLimitEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(framerateLimitEvent.getFramerateLimit());
      }
   }

   @Inject(
      method = {"shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
      EventBus.INSTANCE.dispatch(entityOutlineEvent);
      if (entityOutlineEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;"},
      at = {@At("RETURN")}
   )
   private void hookReloadResources(boolean force, GameLoadCookie loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
      RefreshPacksEvent refreshPacksEvent = new RefreshPacksEvent();
      EventBus.INSTANCE.dispatch(refreshPacksEvent);
   }

   @Inject(
      method = {"resizeDisplay()V"},
      at = {@At("TAIL")}
   )
   private void hookOnResolutionChanged(CallbackInfo ci) {
      ResolutionEvent resolutionEvent = new ResolutionEvent(this.window);
      EventBus.INSTANCE.dispatch(resolutionEvent);
   }
}
