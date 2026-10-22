package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LightTexture.class})
public interface AccessorLightmapTextureManager {
   @Accessor("updateLightTexture")
   void setUpdateLightmap(boolean var1);
}
