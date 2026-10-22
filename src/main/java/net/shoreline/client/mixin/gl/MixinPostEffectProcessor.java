package net.shoreline.client.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.shoreline.client.impl.imixin.IPostEffectProcessor;
import net.shoreline.client.mixin.accessor.AccessorPostEffectPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PostChain.class})
public class MixinPostEffectProcessor implements IPostEffectProcessor {
   @Shadow
   @Final
   private List<PostPass> passes;
   @Shadow
   @Final
   private Map<String, RenderTarget> customRenderTargets;
   @Unique
   private final List<String> bufferTargets = new CopyOnWriteArrayList<>();

   @Override
   public void overwriteBuffer(String name, RenderTarget buffer) {
      RenderTarget target = this.customRenderTargets.get(name);
      if (target != buffer) {
         if (target != null) {
            for (PostPass pass : this.passes) {
               if (pass.inTarget == target) {
                  ((AccessorPostEffectPass)pass).hookSetInput(buffer);
               }

               if (pass.outTarget == target) {
                  ((AccessorPostEffectPass)pass).hookSetOutput(buffer);
               }
            }

            this.customRenderTargets.remove(name);
            this.bufferTargets.remove(name);
         }

         this.customRenderTargets.put(name, buffer);
         this.bufferTargets.add(name);
      }
   }

   @Inject(
      method = {"close()V"},
      at = {@At("HEAD")}
   )
   private void hookClose(CallbackInfo ci) {
      for (String fakedBufferName : this.bufferTargets) {
         this.customRenderTargets.remove(fakedBufferName);
      }
   }
}
