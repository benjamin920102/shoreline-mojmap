package net.shoreline.client.impl.manager.player;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class PositionManager implements Globals {
   private double x;
   private double y;
   private double z;
   private BlockPos blockPos;
   private boolean sneaking;
   private boolean sprinting;
   private boolean onGround;

   public PositionManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public void setPosition(Vec3 vec3d) {
      this.setPosition(vec3d.x(), vec3d.y(), vec3d.z());
   }

   public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
      this.setPositionClient(x, y, z);
      Managers.NETWORK.sendPacket(new PosRot(x, y, z, yaw, pitch, this.isOnGround()));
   }

   public void setPosition(double x, double y, double z) {
      this.setPositionClient(x, y, z);
      Managers.NETWORK.sendPacket(new Pos(x, y, z, this.isOnGround()));
   }

   public void setPositionClient(double x, double y, double z) {
      if (mc.player.isHandsBusy()) {
         mc.player.getVehicle().setPos(x, y, z);
      } else {
         mc.player.setPos(x, y, z);
      }
   }

   public void setPositionXZ(double x, double z) {
      this.setPosition(x, this.y, z);
   }

   public void setPositionY(double y) {
      this.setPosition(this.x, y, this.z);
   }

   public Vec3 getPos() {
      return new Vec3(this.getX(), this.getY(), this.getZ());
   }

   public Vec3 getEyePos() {
      return this.getPos().add(0.0, mc.player.getEyeHeight(), 0.0);
   }

   public final Vec3 getCameraPosVec(float tickDelta) {
      double d = Mth.lerp((double)tickDelta, mc.player.xo, this.getX());
      double e = Mth.lerp((double)tickDelta, mc.player.yo, this.getY()) + mc.player.getEyeHeight();
      double f = Mth.lerp((double)tickDelta, mc.player.zo, this.getZ());
      return new Vec3(d, e, f);
   }

   public double squaredDistanceTo(Entity entity) {
      float f = (float)(this.getX() - entity.getX());
      float g = (float)(this.getY() - entity.getY());
      float h = (float)(this.getZ() - entity.getZ());
      return Mth.lengthSquared((double)f, (double)g, (double)h);
   }

   public double squaredReachDistanceTo(Entity entity) {
      Vec3 cam = this.getCameraPosVec(1.0F);
      float f = (float)(cam.x() - entity.getX());
      float g = (float)(cam.y() - entity.getY());
      float h = (float)(cam.z() - entity.getZ());
      return Mth.lengthSquared((double)f, (double)g, (double)h);
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
            this.onGround = packet.isOnGround();
            if (packet.hasPosition()) {
               this.x = packet.getX(this.x);
               this.y = packet.getY(this.y);
               this.z = packet.getZ(this.z);
               this.blockPos = BlockPos.containing(this.x, this.y, this.z);
            }
         } else if (event.getPacket() instanceof ServerboundPlayerCommandPacket packetx) {
            switch (packetx.getAction()) {
               case START_SPRINTING:
                  this.sprinting = true;
                  break;
               case STOP_SPRINTING:
                  this.sprinting = false;
                  break;
               case PRESS_SHIFT_KEY:
                  this.sneaking = true;
                  break;
               case RELEASE_SHIFT_KEY:
                  this.sneaking = false;
            }
         }
      }
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

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public boolean isOnGround() {
      return this.onGround;
   }
}
