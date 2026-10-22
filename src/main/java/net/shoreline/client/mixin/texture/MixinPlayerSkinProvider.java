package net.shoreline.client.mixin.texture;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.resources.SkinManager.TextureCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({TextureCache.class})
public class MixinPlayerSkinProvider {
   @Shadow
   @Final
   private Type type;
}
