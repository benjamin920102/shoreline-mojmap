package net.shoreline.client.mixin.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelBakery;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;

@Mixin(RenderBuffers.class)
public class MixinBufferBuilderStorage
{

    @Shadow
    @Final
    private SectionBufferBuilderPack blockBufferBuilders;

    @Final
    @Shadow
    @Mutable
    private MultiBufferSource.BufferSource entityVertexConsumers;

    @Final
    @Shadow
    @Mutable
    private OutlineBufferSource outlineVertexConsumers;

    @Mutable
    @Shadow
    @Final
    private MultiBufferSource.BufferSource effectVertexConsumers;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void hookInit(int maxBlockBuildersPoolSize, CallbackInfo ci)
    {
        SequencedMap<RenderType, ByteBufferBuilder> sequencedMap = (SequencedMap) Util.make(new Object2ObjectLinkedOpenHashMap(), (map) ->
        {
            map.put(Sheets.solidBlockSheet(), this.blockBufferBuilders.buffer(RenderType.solid()));
            map.put(Sheets.cutoutBlockSheet(), this.blockBufferBuilders.buffer(RenderType.cutout()));
            map.put(Sheets.bannerSheet(), this.blockBufferBuilders.buffer(RenderType.cutoutMipped()));
            map.put(Sheets.translucentCullBlockSheet(), this.blockBufferBuilders.buffer(RenderType.translucent()));
            map.put(Sheets.shieldSheet(), new ByteBufferBuilder(Sheets.bedSheet().bufferSize()));
            map.put(Sheets.bedSheet(), new ByteBufferBuilder(Sheets.bedSheet().bufferSize()));
            map.put(Sheets.shulkerBoxSheet(), new ByteBufferBuilder(Sheets.shulkerBoxSheet().bufferSize()));
            map.put(Sheets.signSheet(), new ByteBufferBuilder(Sheets.hangingSignSheet().bufferSize()));
            map.put(Sheets.hangingSignSheet(), new ByteBufferBuilder(Sheets.hangingSignSheet().bufferSize()));
            map.put(Sheets.chestSheet(), new ByteBufferBuilder(786432));
            map.put(RenderType.armorEntityGlint(), new ByteBufferBuilder(RenderType.armorEntityGlint().bufferSize()));
            map.put(RenderType.glint(), new ByteBufferBuilder(RenderType.armorEntityGlint().bufferSize()));
            map.put(RenderType.glintTranslucent(), new ByteBufferBuilder(RenderType.glintTranslucent().bufferSize()));
            map.put(RenderType.entityGlint(), new ByteBufferBuilder(RenderType.entityGlint().bufferSize()));
            map.put(RenderType.entityGlintDirect(), new ByteBufferBuilder(RenderType.entityGlintDirect().bufferSize()));
            map.put(RenderType.waterMask(), new ByteBufferBuilder(RenderType.waterMask().bufferSize()));
            ModelBakery.DESTROY_TYPES.forEach((renderLayer) ->
            {
                map.put(renderLayer, new ByteBufferBuilder(renderLayer.bufferSize()));
            });

            map.put(RenderLayersClient.GLINT, new ByteBufferBuilder(RenderLayersClient.GLINT.bufferSize()));
        });
        this.effectVertexConsumers = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
        this.entityVertexConsumers = MultiBufferSource.immediateWithBuffers(sequencedMap, new ByteBufferBuilder(786432));
        this.outlineVertexConsumers = new OutlineBufferSource(this.entityVertexConsumers);
    }
}
