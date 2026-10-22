package net.shoreline.client.mixin.resource;

import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Injects our custom resource manager to allow for remote resource loading (out of dev environments)
 *
 * @author bon
 */
@Mixin(MultiPackResourceManager.class)
public final class MixinLifecycledResourceManagerImpl
{
}
