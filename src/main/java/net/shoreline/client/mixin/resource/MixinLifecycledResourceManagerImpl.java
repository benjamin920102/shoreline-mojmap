package net.shoreline.client.mixin.resource;

import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({MultiPackResourceManager.class})
public final class MixinLifecycledResourceManagerImpl {
}
