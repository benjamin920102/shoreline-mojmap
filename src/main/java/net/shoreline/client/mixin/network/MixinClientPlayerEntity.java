package net.shoreline.client.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.entity.SwingEvent;
import net.shoreline.client.impl.event.entity.player.PlayerMoveEvent;
import net.shoreline.client.impl.event.network.*;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @see LocalPlayer
 * @since 1.0
 */
@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer implements Globals, IClientPlayerEntity
{
    //
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
    //
    @Shadow
    @Final
    protected Minecraft minecraft;
    // Last tick values
    @Shadow
    private boolean wasShiftKeyDown;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;
    @Shadow
    private boolean lastOnGround;
    //
    @Shadow
    private int positionReminder;
    @Shadow
    private boolean autoJumpEnabled;
    //
    @Unique
    private boolean ticking;

    /**
     *
     */
    public MixinClientPlayerEntity()
    {
        // Treating this class as ClientPlayerEntity with mc.player info works
        // Need a better solution
        super(Minecraft.getInstance().level,
                Minecraft.getInstance().player.getGameProfile());
    }

    //
    @Shadow
    protected abstract void sendIsSprintingIfNeeded();

    //
    @Shadow
    public abstract boolean isShiftKeyDown();

    @Shadow
    protected abstract boolean isControlledCamera();

    //
    @Shadow
    protected abstract void updateAutoJump(float dx, float dz);

    //
    @Shadow
    public abstract void tick();

    //
    @Shadow
    protected abstract void sendPosition();

    /**
     * @param ci
     */
    @Inject(method = "sendPosition", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMovementPackets(CallbackInfo ci)
    {
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent();
        playerUpdateEvent.setStage(StageEvent.EventStage.PRE);
        EventBus.INSTANCE.dispatch(playerUpdateEvent);
        // Rotation spoof
        MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(mc.player.getX(), mc.player.getY(),
                mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround());
        EventBus.INSTANCE.dispatch(movementPacketsEvent);
        double x = movementPacketsEvent.getX();
        double y = movementPacketsEvent.getY();
        double z = movementPacketsEvent.getZ();
        float yaw = movementPacketsEvent.getYaw();
        float pitch = movementPacketsEvent.getPitch();
        boolean ground = movementPacketsEvent.getOnGround();

        EncodeYawEvent encodeYawEvent = new EncodeYawEvent();
        EventBus.INSTANCE.dispatch(encodeYawEvent);
        if (encodeYawEvent.isCanceled())
        {
            // yaw overflow, yes grim is retarded
            yaw += 36000000.0f;
        }

        if (movementPacketsEvent.isCanceled() || encodeYawEvent.isCanceled())
        {
            ci.cancel();
            sendIsSprintingIfNeeded();
            boolean bl = isShiftKeyDown();
            if (bl != wasShiftKeyDown)
            {
                ServerboundPlayerCommandPacket.Action mode = bl ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY :
                        ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
                connection.send(new ServerboundPlayerCommandPacket(this, mode));
                wasShiftKeyDown = bl;
            }
            if (isControlledCamera())
            {
                double d = x - xLast;
                double e = y - yLast1;
                double f = z - zLast;
                double g = yaw - yRotLast;
                double h = pitch - xRotLast;
                ++positionReminder;
                boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || positionReminder >= 20;
                boolean bl3 = g != 0.0 || h != 0.0;
                if (isPassenger())
                {
                    Vec3 vec3d = getDeltaMovement();
                    connection.send(new ServerboundMovePlayerPacket.PosRot(vec3d.x, -999.0, vec3d.z, getYRot(), getXRot(), ground));
                    bl2 = false;
                }
                else if (bl2 && bl3)
                {
                    connection.send(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, ground));
                }
                else if (bl2)
                {
                    connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, ground));
                }
                else if (bl3)
                {
                    connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, ground));
                }
                else if (lastOnGround != onGround())
                {
                    connection.send(new ServerboundMovePlayerPacket.StatusOnly(ground));
                }
                if (bl2)
                {
                    xLast = x;
                    yLast1 = y;
                    zLast = z;
                    positionReminder = 0;
                }
                if (bl3)
                {
                    yRotLast = yaw;
                    xRotLast = pitch;
                }
                lastOnGround = ground;
                autoJumpEnabled = minecraft.options.autoJump().get();
            }
        }
        playerUpdateEvent.setStage(StageEvent.EventStage.POST);
        EventBus.INSTANCE.dispatch(playerUpdateEvent);
    }

    /**
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.BEFORE, ordinal = 0))
    private void hookTickPre(CallbackInfo ci)
    {
        PlayerTickEvent playerTickEvent = new PlayerTickEvent();
        EventBus.INSTANCE.dispatch(playerTickEvent);
    }

    /**
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", ordinal = 0, shift = At.Shift.AFTER))
    private void hookTick(CallbackInfo ci)
    {
        if (ticking)
        {
            return;
        }
        TickMovementEvent tickMovementEvent = new TickMovementEvent();
        EventBus.INSTANCE.dispatch(tickMovementEvent);
        if (tickMovementEvent.isCanceled())
        {
            for (int i = 0; i < tickMovementEvent.getIterations(); i++)
            {
                ticking = true;
                tick();
                ticking = false;
                sendPosition();
            }
        }
    }

    /**
     * @param ci
     */
    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void hookTickMovementPost(CallbackInfo ci)
    {
        MovementSlowdownEvent movementUpdateEvent =
                new MovementSlowdownEvent(input);
        EventBus.INSTANCE.dispatch(movementUpdateEvent);
    }

    /**
     * @param movementType
     * @param movement
     * @param ci
     */
    @Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
    private void hookMove(MoverType movementType, Vec3 movement,
                          CallbackInfo ci)
    {
        final PlayerMoveEvent playerMoveEvent =
                new PlayerMoveEvent(movementType, movement);
        EventBus.INSTANCE.dispatch(playerMoveEvent);
        if (playerMoveEvent.isCanceled())
        {
            ci.cancel();
            double d = getX();
            double e = getZ();
            super.move(movementType, playerMoveEvent.getMovement());
            updateAutoJump((float) (getX() - d), (float) (getZ() - e));
        }
    }

    /**
     * @param x
     * @param z
     * @param ci
     */
    @Inject(method = "moveTowardsClosestSpace", at = @At(value = "HEAD"),
            cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci)
    {
        PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
        EventBus.INSTANCE.dispatch(pushOutOfBlocksEvent);
        if (pushOutOfBlocksEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    /**
     * @param hand
     * @param ci
     */
    @Inject(method = "startUsingItem", at = @At(value = "HEAD"))
    private void hookSetCurrentHand(InteractionHand hand, CallbackInfo ci)
    {
        SetCurrentHandEvent setCurrentHandEvent = new SetCurrentHandEvent(hand);
        EventBus.INSTANCE.dispatch(setCurrentHandEvent);
    }

    /**
     * @param instance
     * @param b
     */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V",
            ordinal = 3))
    private void hookSetSprinting(LocalPlayer instance, boolean b)
    {
        final SprintCancelEvent sprintEvent = new SprintCancelEvent();
        EventBus.INSTANCE.dispatch(sprintEvent);
        if (sprintEvent.isCanceled())
        {
            instance.setSprinting(true);
        }
        else
        {
            instance.setSprinting(b);
        }
    }

    /**
     * @param instance
     * @return
     */
    @Redirect(method = "handleConfusionTransitionEffect", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private Screen hookCurrentScreen(Minecraft instance)
    {
        //
        return null;
    }

    /**
     * @param cir
     */
    @Inject(method = "getJumpRidingScale", at = @At(value = "HEAD"),
            cancellable = true)
    private void hookGetMountJumpStrength(CallbackInfoReturnable<Float> cir)
    {
        MountJumpStrengthEvent mountJumpStrengthEvent =
                new MountJumpStrengthEvent();
        EventBus.INSTANCE.dispatch(mountJumpStrengthEvent);
        if (mountJumpStrengthEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(mountJumpStrengthEvent.getJumpStrength());
        }
    }

    /**
     * @param hand
     * @param ci
     */
    @Inject(method = "swing", at = @At(value = "RETURN"))
    private void hookSwingHand(InteractionHand hand, CallbackInfo ci)
    {
        SwingEvent swingEvent = new SwingEvent(hand);
        EventBus.INSTANCE.dispatch(swingEvent);
    }

    @Inject(method = "removeVehicle", at = @At(value = "HEAD"), cancellable = true)
    private void hookDismountVehicle(CallbackInfo ci)
    {
        DismountVehicleEvent dismountVehicleEvent = new DismountVehicleEvent();
        EventBus.INSTANCE.dispatch(dismountVehicleEvent);
        if (dismountVehicleEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Override
    public float getLastSpoofedYaw()
    {
        return yRotLast;
    }

    @Override
    public float getLastSpoofedPitch()
    {
        return xRotLast;
    }
}
