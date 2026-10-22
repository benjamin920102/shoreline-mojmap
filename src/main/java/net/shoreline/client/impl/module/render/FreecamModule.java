package net.shoreline.client.impl.module.render;

import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.MacroConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.macro.Macro;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.MouseUpdateEvent;
import net.shoreline.client.impl.event.PerspectiveEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.camera.CameraPositionEvent;
import net.shoreline.client.impl.event.camera.CameraRotationEvent;
import net.shoreline.client.impl.event.camera.EntityCameraPositionEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.entity.EntityRotationVectorEvent;
import net.shoreline.client.impl.event.keyboard.KeyboardInputEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.render.BobViewEvent;
import net.shoreline.client.impl.event.render.item.RenderFirstPersonEvent;
import net.shoreline.client.impl.manager.player.rotation.Rotation;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.RayCastUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class FreecamModule extends ToggleModule {
   private static FreecamModule INSTANCE;
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The move speed of the camera", 0.1F, 4.0F, 10.0F));
   Config<Macro> controlConfig = this.register(new MacroConfig("ControlKey", "", new Macro(this.getId() + "-control", 342, () -> {})));
   Config<Boolean> toggleControlConfig = this.register(new BooleanConfig("ToggleControl", "Allows toggling control key instead of holding", false));
   Config<FreecamModule.Interact> interactConfig = this.register(
      new EnumConfig<>("Interact", "The interaction type of the camera", FreecamModule.Interact.CAMERA, FreecamModule.Interact.values())
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate to the point of interaction", false));
   public Vec3 position;
   public Vec3 lastPosition;
   public float yaw;
   public float pitch;
   public boolean control = false;

   public FreecamModule() {
      super("Freecam", "Allows you to control the camera separately from the player", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static FreecamModule getInstance() {
      return INSTANCE;
   }

   @Override
   protected void onEnable() {
      if (mc.player != null) {
         this.control = false;
         this.position = mc.gameRenderer.getMainCamera().getPosition();
         this.lastPosition = this.position;
         this.yaw = mc.player.getYRot();
         this.pitch = mc.player.getXRot();
         mc.player.input = new FreecamModule.FreecamKeyboardInput(mc.options);
      }
   }

   @Override
   protected void onDisable() {
      if (mc.player != null) {
         mc.player.input = new KeyboardInput(mc.options);
      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.disable();
      }
   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (event.getAction() != 2 && event.getKeycode() == this.controlConfig.getValue().getKeycode()) {
         if (!this.toggleControlConfig.getValue()) {
            this.control = event.getAction() == 1;
         } else if (event.getAction() == 1) {
            this.control = !this.control;
         }
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onCameraPosition(CameraPositionEvent event) {
      event.setPosition(this.control ? this.position : this.lastPosition.lerp(this.position, event.getTickDelta()));
   }

   @EventListener
   public void onCameraRotation(CameraRotationEvent event) {
      event.setRotation(new Vec2(this.yaw, this.pitch));
   }

   @EventListener
   public void onMouseUpdate(MouseUpdateEvent event) {
      if (!this.control) {
         event.cancel();
         this.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
      }
   }

   @EventListener
   public void onEntityCameraPosition(EntityCameraPositionEvent event) {
      if (event.getEntity() == mc.player) {
         if (!this.control && this.interactConfig.getValue() == FreecamModule.Interact.CAMERA) {
            event.setPosition(this.position);
         }
      }
   }

   @EventListener
   public void onEntityRotation(EntityRotationVectorEvent event) {
      if (event.getEntity() == mc.player) {
         if (!this.control && this.interactConfig.getValue() == FreecamModule.Interact.CAMERA) {
            event.setPosition(RotationUtil.getRotationVector(this.pitch, this.yaw));
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!this.control && this.rotateConfig.getValue()) {
            float[] currentAngles = new float[]{this.yaw, this.pitch};
            Vec3 eyePos = this.position;
            HitResult result = RayCastUtil.rayCast(mc.player.blockInteractionRange(), eyePos, currentAngles);
            if (result.getType() == Type.BLOCK) {
               float[] newAngles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), result.getLocation());
               Managers.ROTATION.setRotation(new Rotation(1, newAngles[0], newAngles[1]));
            }
         }
      }
   }

   @EventListener
   public void onPerspective(PerspectiveEvent event) {
      event.cancel();
   }

   @EventListener
   public void onRenderArm(RenderFirstPersonEvent.Head event) {
      event.cancel();
   }

   @EventListener
   public void onBob(BobViewEvent event) {
      if (this.control) {
         event.cancel();
      }
   }

   private float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   private Vec2 handleVanillaMotion(float speed, float forward, float strafe) {
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2.ZERO;
      } else {
         if (forward != 0.0F && strafe != 0.0F) {
            forward *= (float)Math.sin(Math.PI / 4);
            strafe *= (float)Math.cos(Math.PI / 4);
         }

         return new Vec2(
            (float)(forward * speed * -Math.sin(Math.toRadians(this.yaw)) + strafe * speed * Math.cos(Math.toRadians(this.yaw))),
            (float)(forward * speed * Math.cos(Math.toRadians(this.yaw)) - strafe * speed * -Math.sin(Math.toRadians(this.yaw)))
         );
      }
   }

   private void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
      float f = (float)cursorDeltaY * 0.15F;
      float g = (float)cursorDeltaX * 0.15F;
      this.pitch += f;
      this.yaw += g;
      this.pitch = Mth.clamp(this.pitch, -90.0F, 90.0F);
   }

   public Vec3 getCameraPosition() {
      return this.position;
   }

   public Vec3 getLastCameraPosition() {
      return this.lastPosition;
   }

   public float[] getCameraRotations() {
      return new float[]{this.yaw, this.pitch};
   }

   public class FreecamKeyboardInput extends KeyboardInput {
      private final Options options;

      public FreecamKeyboardInput(Options options) {
         super(options);
         this.options = options;
      }

      @Override
      public void tick(boolean slowDown, float slowDownFactor) {
         if (FreecamModule.this.control) {
            super.tick(slowDown, slowDownFactor);
         } else {
            this.unset();
            float speed = FreecamModule.this.speedConfig.getValue() / 10.0F;
            float fakeMovementForward = FreecamModule.this.getMovementMultiplier(this.options.keyUp.isDown(), this.options.keyDown.isDown());
            float fakeMovementSideways = FreecamModule.this.getMovementMultiplier(this.options.keyLeft.isDown(), this.options.keyRight.isDown());
            Vec2 dir = FreecamModule.this.handleVanillaMotion(speed, fakeMovementForward, fakeMovementSideways);
            float y = 0.0F;
            if (this.options.keyJump.isDown()) {
               y += speed;
            } else if (this.options.keyShift.isDown()) {
               y -= speed;
            }

            FreecamModule.this.lastPosition = FreecamModule.this.position;
            FreecamModule.this.position = FreecamModule.this.position.add(dir.x, y, dir.y);
         }
      }

      private void unset() {
         this.up = false;
         this.down = false;
         this.left = false;
         this.right = false;
         this.forwardImpulse = 0.0F;
         this.leftImpulse = 0.0F;
         this.jumping = false;
         this.shiftKeyDown = false;
      }
   }

   public static enum Interact {
      PLAYER,
      CAMERA;
   }
}
