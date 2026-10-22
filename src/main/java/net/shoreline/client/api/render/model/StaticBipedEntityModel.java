package net.shoreline.client.api.render.model;

import static net.minecraft.client.renderer.entity.player.PlayerRenderer.getArmPose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

public class StaticBipedEntityModel<T extends AbstractClientPlayer> extends PlayerModel<T>
{
    private final T player;
    private float limbSwing;
    private float limbSwingAmount;
    private float yaw;
    private float bodyYaw;
    private float yawHead;
    private float pitch;
    private double x, y, z;

    public StaticBipedEntityModel(T player, boolean thinArms, float tickDelta)
    {
        super(LayerDefinitions.createRoots().get(thinArms ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER).bakeRoot(), thinArms);
        this.player = player;
        this.limbSwing = player.walkAnimation.position(tickDelta);
        this.limbSwingAmount = player.walkAnimation.speed(tickDelta);
        this.yaw = player.getYRot();
        this.bodyYaw = player.getVisualRotationYInDegrees();
        this.yawHead = player.getYHeadRot();
        this.pitch = player.getXRot();
        this.crouching = player.isShiftKeyDown();
        this.rightArmPose = getArmPose(player, player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        this.leftArmPose = getArmPose(player, player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        this.attackTime = player.attackAnim;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        prepareMobModel(player, limbSwing, limbSwingAmount, tickDelta);
        setupAnim(player, limbSwing, limbSwingAmount, player.tickCount + tickDelta, yaw, pitch);
    }

    public AbstractClientPlayer getPlayer()
    {
        return player;
    }

    public float getLimbSwing()
    {
        return limbSwing;
    }

    public float getLimbSwingAmount()
    {
        return limbSwingAmount;
    }

    public float getYaw()
    {
        return yaw;
    }

    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    public float getBodyYaw()
    {
        return bodyYaw;
    }

    public float getPitch()
    {
        return pitch;
    }

    public void setPitch(float pitch)
    {
        this.pitch = pitch;
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
}
