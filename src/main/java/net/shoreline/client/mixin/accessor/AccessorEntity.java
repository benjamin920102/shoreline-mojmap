package net.shoreline.client.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Entity.class})
public interface AccessorEntity {
   @Invoker("unsetRemoved")
   void hookUnsetRemoved();

   @Invoker("setSharedFlag")
   void hookSetFlag(int var1, boolean var2);
}
