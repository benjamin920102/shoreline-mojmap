package net.shoreline.client.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EffectInstance.class)
public class MixinJsonEffectGlShader
{
    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private ResourceLocation ofVanilla0(String path,
                                  Operation<ResourceLocation> original,
                                  ResourceProvider unused,
                                  String name)
    {
        if (!name.contains(":"))
        {
            return original.call(path);
        }

        ResourceLocation split = ResourceLocation.parse(name);
        return ResourceLocation.fromNamespaceAndPath(split.getNamespace(), "shaders/program/" + split.getPath() + ".json");
    }

    @WrapOperation(
            method = "getOrCreate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;")
    )
    private static ResourceLocation ofVanilla1(String path,
                                         Operation<ResourceLocation> original,
                                         ResourceProvider unused,
                                         Program.Type shaderType,
                                         String name)
    {
        if (!name.contains(":"))
        {
            return original.call(path);
        }

        ResourceLocation split = ResourceLocation.parse(name);
        return ResourceLocation.fromNamespaceAndPath(split.getNamespace(), "shaders/program/" + split.getPath() + shaderType.getExtension());
    }
}
