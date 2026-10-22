package net.shoreline.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shoreline.client.impl.event.*;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient implements IMinecraftClient
{
    //
    @Shadow
    public ClientLevel level;
    //
    @Shadow
    public LocalPlayer player;
    //
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    //
    @Shadow
    protected int missTime;
    @Unique
    private boolean leftClick;
    // https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/mixin/MinecraftClientMixin.java#L54
    @Unique
    private boolean rightClick;
    @Unique
    private boolean doAttackCalled;
    @Unique
    private boolean doItemUseCalled;

    /**
     *
     */
    @Shadow
    protected abstract void startUseItem();

    /**
     * @return
     */
    @Shadow
    protected abstract boolean startAttack();

    @Shadow
    @Final
    private Window window;

    /**
     *
     */
    @Override
    public void leftClick()
    {
        leftClick = true;
    }

    /**
     *
     */
    @Override
    public void rightClick()
    {
        rightClick = true;
    }

    /**
     * @param ci
     */
    @Inject(method = "run", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Minecraft;runTick(Z)V", shift = At.Shift.BEFORE))
    private void hookRun(CallbackInfo ci)
    {
        final RunTickEvent runTickEvent = new RunTickEvent();
        EventBus.INSTANCE.dispatch(runTickEvent);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;"))
    private void hookTick(CallbackInfo ci)
    {
        ClientTickEvent tickEvent = new ClientTickEvent();
        EventBus.INSTANCE.dispatch(tickEvent);
    }

    /**
     * @param loadingContext
     * @param cir
     */
    @Inject(method = "buildInitialScreens", at = @At(value = "RETURN"))
    private void hookOnInitFinished(Minecraft.GameLoadCookie loadingContext, CallbackInfoReturnable<Runnable> cir)
    {
        FinishLoadingEvent finishLoadingEvent = new FinishLoadingEvent();
        EventBus.INSTANCE.dispatch(finishLoadingEvent);
        // Managers.CAPES.init();
    }

    /**
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hookTickPre(CallbackInfo ci)
    {
        doAttackCalled = false;
        doItemUseCalled = false;
        if (player != null && level != null)
        {
            TickEvent tickPreEvent = new TickEvent();
            tickPreEvent.setStage(StageEvent.EventStage.PRE);
            EventBus.INSTANCE.dispatch(tickPreEvent);
        }
        if (gameMode == null)
        {
            return;
        }
        if (leftClick && !doAttackCalled)
        {
            startAttack();
        }
        if (rightClick && !doItemUseCalled)
        {
            startUseItem();
        }
        leftClick = false;
        rightClick = false;
    }

    @Unique
    private final List<Integer> deadList = new ArrayList<>();

    /**
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void hookTickPost(CallbackInfo ci)
    {
        if (player != null && level != null)
        {
            TickEvent tickPostEvent = new TickEvent();
            tickPostEvent.setStage(StageEvent.EventStage.POST);
            EventBus.INSTANCE.dispatch(tickPostEvent);
            for (Entity entity : level.entitiesForRendering())
            {
                if (entity instanceof LivingEntity e)
                {
                    if (e.isDeadOrDying() && !deadList.contains(e.getId()))
                    {
                        EntityDeathEvent entityDeathEvent = new EntityDeathEvent(e);
                        EventBus.INSTANCE.dispatch(entityDeathEvent);
                        deadList.add(e.getId());
                    }
                    else if (!e.isDeadOrDying())
                    {
                        deadList.remove((Integer) e.getId());
                    }
                }
            }
        }
    }

    /**
     * @param screen
     * @param ci
     */
    @Inject(method = "setScreen", at = @At(value = "TAIL"))
    private void hookSetScreen(Screen screen, CallbackInfo ci)
    {
        ScreenOpenEvent screenOpenEvent = new ScreenOpenEvent(screen);
        EventBus.INSTANCE.dispatch(screenOpenEvent);
    }

    /**
     * @param ci
     */
    @Inject(method = "startUseItem", at = @At(value = "HEAD"), cancellable = true)
    private void hookDoItemUse(CallbackInfo ci)
    {
        doItemUseCalled = true;
        ItemUseEvent itemUseEvent = new ItemUseEvent();
        EventBus.INSTANCE.dispatch(itemUseEvent);
        if (itemUseEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param cir
     */
    @Inject(method = "startAttack", at = @At(value = "HEAD"))
    private void hookDoAttack(CallbackInfoReturnable<Boolean> cir)
    {
        doAttackCalled = true;
        AttackCooldownEvent attackCooldownEvent = new AttackCooldownEvent();
        EventBus.INSTANCE.dispatch(attackCooldownEvent);
        if (attackCooldownEvent.isCanceled())
        {
            missTime = 0;
        }
    }

    /**
     * @param instance
     * @return
     */
    @Redirect(method = "continueAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean hookIsUsingItem(LocalPlayer instance)
    {
        ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
        EventBus.INSTANCE.dispatch(itemMultitaskEvent);
        return !itemMultitaskEvent.isCanceled() && instance.isUsingItem();
    }

    /**
     * @param instance
     * @return
     */
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean hookIsBreakingBlock(MultiPlayerGameMode instance)
    {
        ItemMultitaskEvent itemMultitaskEvent = new ItemMultitaskEvent();
        EventBus.INSTANCE.dispatch(itemMultitaskEvent);
        return !itemMultitaskEvent.isCanceled() && instance.isDestroying();
    }

    /**
     * @param cir
     */
    @Inject(method = "getFramerateLimit", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetFramerateLimit(CallbackInfoReturnable<Integer> cir)
    {
        FramerateLimitEvent framerateLimitEvent = new FramerateLimitEvent();
        EventBus.INSTANCE.dispatch(framerateLimitEvent);
        if (framerateLimitEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(framerateLimitEvent.getFramerateLimit());
        }
    }

    /**
     * @param entity
     * @param cir
     */
    @Inject(method = "shouldEntityAppearGlowing", at = @At(value = "HEAD"), cancellable = true)
    private void hookHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        EntityOutlineEvent entityOutlineEvent = new EntityOutlineEvent(entity);
        EventBus.INSTANCE.dispatch(entityOutlineEvent);
        if (entityOutlineEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "RETURN"))
    private void hookReloadResources(boolean force, Minecraft.GameLoadCookie loadingContext, CallbackInfoReturnable<CompletableFuture<Void>> cir)
    {
        RefreshPacksEvent refreshPacksEvent = new RefreshPacksEvent();
        EventBus.INSTANCE.dispatch(refreshPacksEvent);
    }

    @Inject(method = "resizeDisplay", at = @At(value = "TAIL"))
    private void hookOnResolutionChanged(CallbackInfo ci)
    {
        ResolutionEvent resolutionEvent = new ResolutionEvent(window);
        EventBus.INSTANCE.dispatch(resolutionEvent);
    }
}
