package net.shoreline.client.mixin.accessor;

import java.util.Set;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Options.class})
public interface AccessorGameOptions {
   @Accessor("modelParts")
   @Mutable
   Set<PlayerModelPart> getPlayerModelParts();
}
