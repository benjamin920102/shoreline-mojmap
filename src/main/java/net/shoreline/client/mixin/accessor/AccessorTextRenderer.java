package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Shoreline
 * @since 1.0
 */
@Mixin(Font.class)
public interface AccessorTextRenderer
{
    /**
     * @return
     */
    @Accessor("filterFishyGlyphs")
    boolean hookGetValidateAdvance();

    /**
     * @param id
     * @return
     */
    @Invoker("getFontSet")
    FontSet hookGetFontStorage(ResourceLocation id);

    /**
     * @param glyphRenderer
     * @param bold
     * @param italic
     * @param weight
     * @param x
     * @param y
     * @param matrix
     * @param vertexConsumer
     * @param red
     * @param green
     * @param blue
     * @param alpha
     * @param light
     */
    @Invoker("renderChar")
    void hookDrawGlyph(BakedGlyph glyphRenderer, boolean bold, boolean italic,
                       float weight, float x, float y, Matrix4f matrix,
                       VertexConsumer vertexConsumer, float red, float green,
                       float blue, float alpha, int light);

    /**
     * @param text
     * @param x
     * @param y
     * @param color
     * @param shadow
     * @param matrix
     * @param vertexConsumerProvider
     * @param layerType
     * @param underlineColor
     * @param light
     * @return
     */
    @Invoker("renderText")
    float hookDrawLayer(String text, float x, float y,
                        int color, boolean shadow, Matrix4f matrix,
                        MultiBufferSource vertexConsumerProvider,
                        Font.DisplayMode layerType, int underlineColor,
                        int light);
}
