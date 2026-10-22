package net.shoreline.client.impl.manager.player.rotation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
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

public class RotationManager implements Globals {
   private final List<Rotation> requests = new CopyOnWriteArrayList<>();
   private float serverYaw;
   private float serverPitch;
   private float lastServerYaw;
   private float lastServerPitch;
   private float prevJumpYaw;
   private float prevYaw;
   private float prevPitch;
   boolean rotate;
   private Rotation rotation;
   private int rotateTicks;
   private boolean webJumpFix;
   private boolean preJumpFix;

   public RotationManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
            float packetYaw = packet.getYRot(0.0F);
            float packetPitch = packet.getXRot(0.0F);
            this.serverYaw = packetYaw;
            this.serverPitch = packetPitch;
         }
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onUpdate(PlayerTickEvent event) {
      this.webJumpFix = PlayerUtil.inWeb(1.0);
      if (this.requests.isEmpty()) {
         this.rotation = null;
      } else {
         Rotation request = this.getRotationRequest();
         if (request == null) {
            if (this.isDoneRotating()) {
               this.rotation = null;
               return;
            }
         } else {
            this.rotation = request;
         }

         if (this.rotation != null) {
            this.rotateTicks = 0;
            this.rotate = true;
         }
      }
   }

   @EventListener
   public void onMovementPackets(MovementPacketsEvent event) {
      if (this.rotation != null) {
         if (this.rotate) {
            this.removeRotation(this.rotation);
            event.cancel();
            event.setYaw(this.rotation.getYaw());
            event.setPitch(this.rotation.getPitch());
            this.rotate = false;
         }

         if (this.rotation.isSnap()) {
            this.rotation = null;
         }
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         this.lastServerYaw = ((IClientPlayerEntity)mc.player).getLastSpoofedYaw();
         this.lastServerPitch = ((IClientPlayerEntity)mc.player).getLastSpoofedPitch();
      }
   }

   @EventListener
   public void onKeyboardTick(KeyboardTickEvent event) {
      if (this.rotation != null && mc.player != null && RotationsModule.getInstance().getMovementFix()) {
         float forward = mc.player.input.forwardImpulse;
         float sideways = mc.player.input.leftImpulse;
         float delta = (mc.player.getYRot() - this.rotation.getYaw()) * (float) (Math.PI / 180.0);
         float cos = Mth.cos(delta);
         float sin = Mth.sin(delta);
         mc.player.input.leftImpulse = Math.round(sideways * cos - forward * sin);
         mc.player.input.forwardImpulse = Math.round(forward * cos + sideways * sin);
      }
   }

   @EventListener
   public void onUpdateVelocity(UpdateVelocityEvent event) {
      if (this.rotation != null && RotationsModule.getInstance().getMovementFix()) {
         event.cancel();
         event.setVelocity(this.movementInputToVelocity(this.rotation.getYaw(), event.getMovementInput(), event.getSpeed()));
      }
   }

   @EventListener
   public void onPlayerJump(PlayerJumpEvent event) {
      if (this.rotation != null && RotationsModule.getInstance().getMovementFix()) {
         if (event.getStage() == StageEvent.EventStage.PRE) {
            this.prevJumpYaw = mc.player.getYRot();
            mc.player.setYRot(this.rotation.getYaw());
            if (AnticheatModule.getInstance().getWebJumpFix() && this.webJumpFix) {
               this.preJumpFix = mc.player.isSprinting();
               mc.player.setSprinting(false);
            }
         } else {
            mc.player.setYRot(this.prevJumpYaw);
            if (this.webJumpFix) {
               mc.player.setSprinting(this.preJumpFix);
            }
         }
      }
   }

   @EventListener
   public void onRenderPlayer(RenderPlayerEvent event) {
      if (event.getEntity() == mc.player && this.rotation != null) {
         event.setYaw(Interpolation.interpolateFloat(this.prevYaw, this.getServerYaw(), mc.getTimer().getGameTimeDeltaPartialTick(true)));
         event.setPitch(Interpolation.interpolateFloat(this.prevPitch, this.getServerPitch(), mc.getTimer().getGameTimeDeltaPartialTick(true)));
         this.prevYaw = event.getYaw();
         this.prevPitch = event.getPitch();
         event.cancel();
      }
   }

   public void setRotation(Rotation rotation) {
      if (RotationsModule.getInstance().getMouseSensFix()) {
         double fix = Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
         rotation.setYaw((float)(rotation.getYaw() - (rotation.getYaw() - this.serverYaw) % fix));
         rotation.setPitch((float)(rotation.getPitch() - (rotation.getPitch() - this.serverPitch) % fix));
      }

      if (rotation.getPriority() == Integer.MAX_VALUE) {
         this.rotation = rotation;
      }

      Rotation request = this.requests.stream().filter(r -> rotation.getPriority() == r.getPriority()).findFirst().orElse(null);
      if (request == null) {
         this.requests.add(rotation);
      } else {
         request.setYaw(rotation.getYaw());
         request.setPitch(rotation.getPitch());
      }
   }

   public void setRotationClient(float yaw, float pitch) {
      if (mc.player != null) {
         mc.player.setYRot(yaw);
         mc.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
      }
   }

   public void setRotationSilent(float yaw, float pitch) {
      this.setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      Managers.NETWORK.sendPacket(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
   }

   public void setRotationSilentSync() {
      float yaw = mc.player.getYRot();
      float pitch = mc.player.getXRot();
      this.setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      Managers.NETWORK.sendPacket(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround()));
   }

   public boolean removeRotation(Rotation request) {
      return this.requests.remove(request);
   }

   public boolean isRotationBlocked(int priority) {
      return this.rotation != null && priority < this.rotation.getPriority();
   }

   public boolean isDoneRotating() {
      return this.rotateTicks > RotationsModule.getInstance().getPreserveTicks();
   }

   public boolean isRotating() {
      return this.rotation != null;
   }

   public float getRotationYaw() {
      return this.rotation.getYaw();
   }

   public float getRotationPitch() {
      return this.rotation.getPitch();
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getWrappedYaw() {
      return Mth.wrapDegrees(this.serverYaw);
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   private Vec3 movementInputToVelocity(float yaw, Vec3 movementInput, float speed) {
      double d = movementInput.lengthSqr();
      if (d < 1.0E-7) {
         return Vec3.ZERO;
      } else {
         Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
         float f = Mth.sin(yaw * (float) (Math.PI / 180.0));
         float g = Mth.cos(yaw * (float) (Math.PI / 180.0));
         return new Vec3(vec3d.x * g - vec3d.z * f, vec3d.y, vec3d.z * g + vec3d.x * f);
      }
   }

   private Rotation getRotationRequest() {
      Rotation rotationRequest = null;
      int priority = 0;

      for (Rotation request : this.requests) {
         if (request.getPriority() > priority) {
            rotationRequest = request;
            priority = request.getPriority();
         }
      }

      return rotationRequest;
   }
}
