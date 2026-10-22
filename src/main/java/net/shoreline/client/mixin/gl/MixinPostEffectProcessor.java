package net.shoreline.client.mixin.gl;

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
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(PostChain.class)
public class MixinPostEffectProcessor implements IPostEffectProcessor
{
    @Shadow
    @Final
    private List<PostPass> passes;

    @Shadow
    @Final
    private Map<String, RenderTarget> customRenderTargets;

    @Unique
    private final List<String> bufferTargets = new CopyOnWriteArrayList<>();

    @Override
    public void overwriteBuffer(String name, RenderTarget buffer)
    {
        RenderTarget target = customRenderTargets.get(name);
        if (target == buffer)
        {
            return;
        }
        if (target != null)
        {
            for (PostPass pass : passes)
            {
                if (pass.inTarget == target)
                {
                    ((AccessorPostEffectPass) pass).hookSetInput(buffer);
                }
                if (pass.outTarget == target)
                {
                    ((AccessorPostEffectPass) pass).hookSetOutput(buffer);
                }
            }
            customRenderTargets.remove(name);
            bufferTargets.remove(name);
        }

        customRenderTargets.put(name, buffer);
        bufferTargets.add(name);
    }

    @Inject(method = "close", at = @At(value = "HEAD"))
    private void hookClose(CallbackInfo ci)
    {
        for (String fakedBufferName : bufferTargets)
        {
            customRenderTargets.remove(fakedBufferName);
        }
    }
}
