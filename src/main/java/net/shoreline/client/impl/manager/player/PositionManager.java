package net.shoreline.client.impl.manager.player;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

/**
 * @author linus
 * @since 1.0
 */
public class PositionManager implements Globals
{
    //
    private double x, y, z;
    private BlockPos blockPos;
    //
    private boolean sneaking, sprinting;
    //
    private boolean onGround;

    /**
     *
     */
    public PositionManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    /**
     * @param vec3d
     */
    public void setPosition(Vec3 vec3d)
    {
        setPosition(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch)
    {
        setPositionClient(x, y, z);
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, isOnGround()));
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public void setPosition(double x, double y, double z)
    {
        setPositionClient(x, y, z);
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.Pos(x, y, z, isOnGround()));
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public void setPositionClient(double x, double y, double z)
    {
        if (mc.player.isHandsBusy())
        {
            mc.player.getVehicle().setPos(x, y, z);
            return;
        }
        mc.player.setPos(x, y, z);
    }

    /**
     * @param x
     * @param z
     */
    public void setPositionXZ(double x, double z)
    {
        setPosition(x, y, z);
    }

    /**
     * @param y
     */
    public void setPositionY(double y)
    {
        setPosition(x, y, z);
    }

    /**
     * @return
     */
    public Vec3 getPos()
    {
        return new Vec3(getX(), getY(), getZ());
    }

    /**
     * @return
     */
    public Vec3 getEyePos()
    {
        return getPos().add(0.0, mc.player.getEyeHeight(), 0.0);
    }

    /**
     * @param tickDelta
     * @return
     */
    public final Vec3 getCameraPosVec(float tickDelta)
    {
        double d = Mth.lerp(tickDelta, mc.player.xo, getX());
        double e = Mth.lerp(tickDelta, mc.player.yo, getY())
                + (double) mc.player.getEyeHeight();
        double f = Mth.lerp(tickDelta, mc.player.zo, getZ());
        return new Vec3(d, e, f);
    }

    /**
     * @param entity
     * @return
     */
    public double squaredDistanceTo(Entity entity)
    {
        float f = (float) (getX() - entity.getX());
        float g = (float) (getY() - entity.getY());
        float h = (float) (getZ() - entity.getZ());
        return Mth.lengthSquared(f, g, h);
    }

    /**
     * @param entity
     * @return
     */
    public double squaredReachDistanceTo(Entity entity)
    {
        Vec3 cam = getCameraPosVec(1.0f);
        float f = (float) (cam.x() - entity.getX());
        float g = (float) (cam.y() - entity.getY());
        float h = (float) (cam.z() - entity.getZ());
        return Mth.lengthSquared(f, g, h);
    }

    /**
     * @param event
     */
    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (mc.player != null && mc.level != null)
        {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket packet)
            {
                onGround = packet.isOnGround();
                if (packet.hasPosition())
                {
                    x = packet.getX(x);
                    y = packet.getY(y);
                    z = packet.getZ(z);
                    blockPos = BlockPos.containing(x, y, z);
                }
            }
            else if (event.getPacket() instanceof ServerboundPlayerCommandPacket packet)
            {
                switch (packet.getAction())
                {
                    case START_SPRINTING -> sprinting = true;
                    case STOP_SPRINTING -> sprinting = false;
                    case PRESS_SHIFT_KEY -> sneaking = true;
                    case RELEASE_SHIFT_KEY -> sneaking = false;
                }
            }
        }
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getZ()
    {
        return z;
    }

    /**
     * @return
     */
    public BlockPos getBlockPos()
    {
        return blockPos;
    }

    /**
     * @return
     */
    public boolean isSneaking()
    {
        return sneaking;
    }

    /**
     * @return
     */
    public boolean isSprinting()
    {
        return sprinting;
    }

    /**
     * @return
     */
    public boolean isOnGround()
    {
        return onGround;
    }
}
