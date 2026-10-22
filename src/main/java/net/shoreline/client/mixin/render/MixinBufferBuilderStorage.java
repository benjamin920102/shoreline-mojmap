package net.shoreline.client.mixin.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SequencedMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.resources.model.ModelBakery;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderBuffers.class})
public class MixinBufferBuilderStorage {
   @Shadow
   @Final
   private SectionBufferBuilderPack fixedBufferPack;
   @Final
   @Shadow
   @Mutable
   private BufferSource bufferSource;
   @Final
   @Shadow
   @Mutable
   private OutlineBufferSource outlineBufferSource;
   @Mutable
   @Shadow
   @Final
   private BufferSource crumblingBufferSource;

   @Inject(
      method = {"<init>(I)V"},
      at = {@At("TAIL")}
   )
   private void hookInit(int maxBlockBuildersPoolSize, CallbackInfo ci) {
      SequencedMap<RenderType, ByteBufferBuilder> sequencedMap = Util.make(new Object2ObjectLinkedOpenHashMap(), map -> {
         map.put(Sheets.solidBlockSheet(), this.fixedBufferPack.buffer(RenderType.solid()));
         map.put(Sheets.cutoutBlockSheet(), this.fixedBufferPack.buffer(RenderType.cutout()));
         map.put(Sheets.bannerSheet(), this.fixedBufferPack.buffer(RenderType.cutoutMipped()));
         map.put(Sheets.translucentCullBlockSheet(), this.fixedBufferPack.buffer(RenderType.translucent()));
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
         ModelBakery.DESTROY_TYPES.forEach(renderLayer -> map.put(renderLayer, new ByteBufferBuilder(renderLayer.bufferSize())));
         map.put(RenderLayersClient.GLINT, new ByteBufferBuilder(RenderLayersClient.GLINT.bufferSize()));
      });
      this.crumblingBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
      this.bufferSource = MultiBufferSource.immediateWithBuffers(sequencedMap, new ByteBufferBuilder(786432));
      this.outlineBufferSource = new OutlineBufferSource(this.bufferSource);
   }
}
