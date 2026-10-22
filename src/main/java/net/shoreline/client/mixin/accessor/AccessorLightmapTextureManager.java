package net.shoreline.client.mixin.accessor;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author linus
 * @see LightTexture
 * @since 1.0
 */
@Mixin(LightTexture.class)
public interface AccessorLightmapTextureManager
{
    /**
     * @param dirty
     */
    @Accessor("updateLightTexture")
    void setUpdateLightmap(boolean dirty);
}
