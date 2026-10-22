package net.shoreline.client.api.render.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

public class StaticBipedEntityModel<T extends AbstractClientPlayer> extends PlayerModel<T> {
   private final T player;
   private float limbSwing;
   private float limbSwingAmount;
   private float yaw;
   private float bodyYaw;
   private float yawHead;
   private float pitch;
   private double x;
   private double y;
   private double z;

   public StaticBipedEntityModel(T player, boolean thinArms, float tickDelta) {
      super(LayerDefinitions.createRoots().get(thinArms ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER).bakeRoot(), thinArms);
      this.player = player;
      this.limbSwing = player.walkAnimation.position(tickDelta);
      this.limbSwingAmount = player.walkAnimation.speed(tickDelta);
      this.yaw = player.getYRot();
      this.bodyYaw = player.getVisualRotationYInDegrees();
      this.yawHead = player.getYHeadRot();
      this.pitch = player.getXRot();
      this.crouching = player.isShiftKeyDown();
      this.rightArmPose = PlayerRenderer.getArmPose(player, player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
      this.leftArmPose = PlayerRenderer.getArmPose(player, player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
      this.attackTime = player.attackAnim;
      this.x = player.getX();
      this.y = player.getY();
      this.z = player.getZ();
      this.prepareMobModel(player, this.limbSwing, this.limbSwingAmount, tickDelta);
      this.setupAnim(player, this.limbSwing, this.limbSwingAmount, player.tickCount + tickDelta, this.yaw, this.pitch);
   }

   public AbstractClientPlayer getPlayer() {
      return this.player;
   }

   public float getLimbSwing() {
      return this.limbSwing;
   }

   public float getLimbSwingAmount() {
      return this.limbSwingAmount;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getBodyYaw() {
      return this.bodyYaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }
}
