package net.shoreline.client.mixin.accessor;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyMapping.class})
public interface AccessorKeyBinding {
   @Accessor("key")
   Key getBoundKey();
}
