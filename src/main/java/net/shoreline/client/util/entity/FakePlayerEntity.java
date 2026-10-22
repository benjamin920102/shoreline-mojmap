package net.shoreline.client.util.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.util.Globals;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linus
 * @since 1.0
 */
public class FakePlayerEntity extends RemotePlayer implements Globals
{
    //
    public static final AtomicInteger CURRENT_ID = new AtomicInteger(1000000);
    //
    private final Player player;

    /**
     * @param player
     * @param name
     */
    public FakePlayerEntity(Player player, String name)
    {
        super(Minecraft.getInstance().level,
                new GameProfile(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), name));
        this.player = player;
        copyPosition(player);
        yRotO = getYRot();
        xRotO = getXRot();
        yHeadRot = player.yHeadRot;
        yHeadRotO = yHeadRot;
        yBodyRot = player.yBodyRot;
        yBodyRotO = yBodyRot;
        walkAnimation.position = player.walkAnimation.position();
        walkAnimation.setSpeed(player.walkAnimation.speed());
        Byte playerModel = player.getEntityData()
                .get(Player.DATA_PLAYER_MODE_CUSTOMISATION);
        entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
        getAttributes().assignAllValues(player.getAttributes());
        setShiftKeyDown(player.isShiftKeyDown());
        setSwimming(player.isSwimming());
        setPose(player.getPose());
        setHealth(player.getHealth());
        setAbsorptionAmount(player.getAbsorptionAmount());
        // setBoundingBox(player.getBoundingBox());
        getInventory().replaceWith(player.getInventory());
        setId(CURRENT_ID.incrementAndGet());
        this.tickCount = 100;
    }

    /**
     * @param player
     */
    public FakePlayerEntity(Player player)
    {
        this(player, player.getName().getString());
    }

    /**
     *
     */
    public void spawnPlayer()
    {
        if (mc.level != null)
        {
            unsetRemoved();
            mc.level.addEntity(this);
        }
    }

    /**
     *
     */
    public void despawnPlayer()
    {
        if (mc.level != null)
        {
            mc.level.removeEntity(getId(), RemovalReason.DISCARDED);
            setRemoved(RemovalReason.DISCARDED);
        }
    }

    /**
     * @return
     */
    @Override
    public boolean isDeadOrDying()
    {
        return false;
    }

    /**
     * @return
     */
    public Player getPlayer()
    {
        return player;
    }
}
