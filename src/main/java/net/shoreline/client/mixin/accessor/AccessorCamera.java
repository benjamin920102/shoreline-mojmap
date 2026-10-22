package net.shoreline.client.mixin.accessor;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Camera.class})
public interface AccessorCamera {
   @Accessor("eyeHeight")
   float getCameraY();

   @Accessor("eyeHeightOld")
   float getLastCameraY();
}
