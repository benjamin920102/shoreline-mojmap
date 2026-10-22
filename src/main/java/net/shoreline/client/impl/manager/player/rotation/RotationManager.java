package net.shoreline.client.impl.manager.player.rotation;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.impl.event.entity.UpdateVelocityEvent;
import net.shoreline.client.impl.event.entity.player.PlayerJumpEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardTickEvent;
import net.shoreline.client.impl.event.network.MovementPacketsEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.render.entity.RenderPlayerEvent;
import net.shoreline.client.impl.imixin.IClientPlayerEntity;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.client.RotationsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

/**
 * @author linus, bon55
 * @since 1.0
 */
public class RotationManager implements Globals
{
    private final List<Rotation> requests = new CopyOnWriteArrayList<>();
    // Relevant rotation values
    private float serverYaw, serverPitch, lastServerYaw, lastServerPitch, prevJumpYaw, prevYaw, prevPitch;
    boolean rotate;

    // The current in use rotation
    private Rotation rotation;
    private int rotateTicks;

    // Sprint jump fix for webs
    private boolean webJumpFix;
    private boolean preJumpFix;

    /**
     *
     */
    public RotationManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation())
        {
            float packetYaw = packet.getYRot(0.0f);
            float packetPitch = packet.getXRot(0.0f);
            serverYaw = packetYaw;
            serverPitch = packetPitch;
        }
    }

    @EventListener(priority = MIN_VALUE)
    public void onUpdate(PlayerTickEvent event)
    {
        webJumpFix = PlayerUtil.inWeb(1.0);

        if (requests.isEmpty())
        {
            rotation = null;
            return;
        }
        Rotation request = getRotationRequest();
        if (request == null)
        {
            if (isDoneRotating())
            {
                rotation = null;
                return;
            }
        }
        else
        {
            rotation = request;
        }
        // fixes flags for aim % 360
        // GCD implementation maybe?
        if (rotation == null)
        {
            return;
        }
        rotateTicks = 0;
        rotate = true;
    }

    @EventListener
    public void onMovementPackets(MovementPacketsEvent event)
    {
        if (rotation != null)
        {

            if (rotate)
            {
                removeRotation(rotation);
                event.cancel();
                event.setYaw(rotation.getYaw());
                event.setPitch(rotation.getPitch());
                rotate = false;
            }

            if (rotation.isSnap())
            {
                rotation = null;
            }
        }
    }

//    @EventListener
//    public void onEntityRotationVector(final EntityRotationVectorEvent event) {
//        if (event.getEntity() instanceof ClientPlayerEntity && rotation != null) {
//            final float rotX = MathHelper.lerp(event.getTickDelta(), serverPitch, lastServerPitch);
//            final float rotY = MathHelper.lerp(event.getTickDelta(), serverYaw, lastServerYaw);
//            event.setPosition(RotationUtil.getRotationVector(rotX, rotY));
//        }
//    }

    @EventListener
    public void onPlayerUpdate(final PlayerUpdateEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.POST)
        {
            lastServerYaw = ((IClientPlayerEntity) mc.player).getLastSpoofedYaw();
            lastServerPitch = ((IClientPlayerEntity) mc.player).getLastSpoofedPitch();
        }
    }

    @EventListener
    public void onKeyboardTick(KeyboardTickEvent event)
    {
        if (rotation != null && mc.player != null
                && RotationsModule.getInstance().getMovementFix())
        {
            float forward = mc.player.input.forwardImpulse;
            float sideways = mc.player.input.leftImpulse;
            float delta = (mc.player.getYRot() - rotation.getYaw()) * Mth.DEG_TO_RAD;
            float cos = Mth.cos(delta);
            float sin = Mth.sin(delta);
            mc.player.input.leftImpulse = Math.round(sideways * cos - forward * sin);
            mc.player.input.forwardImpulse = Math.round(forward * cos + sideways * sin);
        }
    }

    @EventListener
    public void onUpdateVelocity(UpdateVelocityEvent event)
    {
        if (rotation != null && RotationsModule.getInstance().getMovementFix())
        {
            event.cancel();
            event.setVelocity(movementInputToVelocity(rotation.getYaw(), event.getMovementInput(), event.getSpeed()));
        }
    }

    @EventListener
    public void onPlayerJump(PlayerJumpEvent event)
    {
        if (rotation != null && RotationsModule.getInstance().getMovementFix())
        {
            if (event.getStage() == StageEvent.EventStage.PRE)
            {
                prevJumpYaw = mc.player.getYRot();
                mc.player.setYRot(rotation.getYaw());
                if (AnticheatModule.getInstance().getWebJumpFix() && webJumpFix)
                {
                    preJumpFix = mc.player.isSprinting();
                    mc.player.setSprinting(false);
                }
            }
            else
            {
                mc.player.setYRot(prevJumpYaw);
                if (webJumpFix)
                {
                    mc.player.setSprinting(preJumpFix);
                }
            }
        }
    }

    @EventListener
    public void onRenderPlayer(RenderPlayerEvent event)
    {
        if (event.getEntity() == mc.player && rotation != null)
        {
            // Match packet server rotations
            event.setYaw(Interpolation.interpolateFloat(prevYaw, getServerYaw(), mc.getTimer().getGameTimeDeltaPartialTick(true)));
            event.setPitch(Interpolation.interpolateFloat(prevPitch, getServerPitch(), mc.getTimer().getGameTimeDeltaPartialTick(true)));
            prevYaw = event.getYaw();
            prevPitch = event.getPitch();
            event.cancel();
        }
    }

    /**
     * @param rotation
     */
    public void setRotation(Rotation rotation)
    {

        if (RotationsModule.getInstance().getMouseSensFix())
        {
            double fix = Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
            rotation.setYaw((float) (rotation.getYaw() - (rotation.getYaw() - serverYaw) % fix));
            rotation.setPitch((float) (rotation.getPitch() - (rotation.getPitch() - serverPitch) % fix));
        }
        if (rotation.getPriority() == MAX_VALUE)
        {
            this.rotation = rotation;
        }

        Rotation request = requests.stream().filter(r -> rotation.getPriority() == r.getPriority()).findFirst().orElse(null);
        if (request == null)
        {
            requests.add(rotation);
        }
        else
        {
            // r.setPriority();
            request.setYaw(rotation.getYaw());
            request.setPitch(rotation.getPitch());
        }
    }

    /**
     * @param yaw
     * @param pitch
     */
    public void setRotationClient(float yaw, float pitch)
    {
        if (mc.player == null)
        {
            return;
        }
        mc.player.setYRot(yaw);
        mc.player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
    }

    public void setRotationSilent(float yaw, float pitch)
    {
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
    }

    // This is only required by grim because of rotation movement checks
    public void setRotationSilentSync()
    {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
        // Managers.NETWORK.sendSequencedPacket((s) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, s));
    }

    /**
     * @param request
     */
    public boolean removeRotation(Rotation request)
    {
        return requests.remove(request);
    }

    public boolean isRotationBlocked(int priority)
    {
        return rotation != null && priority < rotation.getPriority();
    }

    /**
     * @return
     */
    public boolean isDoneRotating()
    {
        return rotateTicks > RotationsModule.getInstance().getPreserveTicks();
    }

    public boolean isRotating()
    {
        return rotation != null;
    }

    public float getRotationYaw()
    {
        return rotation.getYaw();
    }

    public float getRotationPitch()
    {
        return rotation.getPitch();
    }

    /**
     * @return
     */
    public float getServerYaw()
    {
        return serverYaw;
    }

    /**
     * @return
     */
    public float getWrappedYaw()
    {
        return Mth.wrapDegrees(serverYaw);
    }

    /**
     * @return
     */
    public float getServerPitch()
    {
        return serverPitch;
    }

    //
    private Vec3 movementInputToVelocity(float yaw, Vec3 movementInput, float speed)
    {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7)
        {
            return Vec3.ZERO;
        }
        Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
        float f = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float g = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(vec3d.x * (double) g - vec3d.z * (double) f, vec3d.y, vec3d.z * (double) g + vec3d.x * (double) f);
    }

    private Rotation getRotationRequest()
    {
        Rotation rotationRequest = null;
        int priority = 0;
        for (Rotation request : requests)
        {
            if (request.getPriority() > priority)
            {
                rotationRequest = request;
                priority = request.getPriority();
            }
        }
        return rotationRequest;
    }
}