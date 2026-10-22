package net.shoreline.client.mixin.accessor;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractContainerMenu.class})
public interface AccessorScreenHandler {
   @Accessor("stateId")
   void hookSetRevision(int var1);
}
