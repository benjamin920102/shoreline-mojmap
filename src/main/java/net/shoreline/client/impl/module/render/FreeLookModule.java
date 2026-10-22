package net.shoreline.client.impl.module.render;

import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.MouseUpdateEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.camera.CameraRotationEvent;
import net.shoreline.client.impl.event.option.PerspectiveUpdateEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class FreeLookModule extends ToggleModule {
   private float cameraYaw;
   private float cameraPitch;
   private CameraType perspective;

   public FreeLookModule() {
      super("FreeLook", "Allows you to freely move the camera in third person", ModuleCategory.RENDER);
   }

   @Override
   public void onEnable() {
      this.perspective = mc.options.getCameraType();
   }

   @Override
   public void onDisable() {
      if (this.perspective != null) {
         mc.options.setCameraType(this.perspective);
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.perspective != null && this.perspective != CameraType.THIRD_PERSON_BACK) {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }
   }

   @EventListener
   public void onPerspectiveUpdate(PerspectiveUpdateEvent event) {
      if (mc.options.getCameraType() != event.getPerspective() && event.getPerspective() != CameraType.FIRST_PERSON) {
         this.cameraYaw = mc.player.getYRot();
         this.cameraPitch = mc.player.getXRot();
      }
   }

   @EventListener
   public void onCameraRotation(CameraRotationEvent event) {
      if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
         event.setYaw(this.cameraYaw);
         event.setPitch(this.cameraPitch);
      }
   }

   @EventListener
   public void onMouseUpdate(MouseUpdateEvent event) {
      if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
         event.cancel();
         this.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
      }
   }

   private void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
      float f = (float)cursorDeltaY * 0.15F;
      float g = (float)cursorDeltaX * 0.15F;
      this.cameraPitch += f;
      this.cameraYaw += g;
      this.cameraPitch = Mth.clamp(this.cameraPitch, -90.0F, 90.0F);
   }
}
