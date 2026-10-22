package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({NativeImage.class})
public interface AccessorNativeImage {
   @Accessor("pixels")
   long hookGetPointer();
}
