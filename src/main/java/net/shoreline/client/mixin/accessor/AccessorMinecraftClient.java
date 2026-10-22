package net.shoreline.client.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Minecraft.class})
public interface AccessorMinecraftClient {
   @Accessor("rightClickDelay")
   void hookSetItemUseCooldown(int var1);

   @Accessor("rightClickDelay")
   int hookGetItemUseCooldown();

   @Accessor("missTime")
   void hookSetAttackCooldown(int var1);

   @Invoker("startUseItem")
   void hookDoItemUse();

   @Accessor("user")
   @Final
   @Mutable
   void setSession(User var1);

   @Accessor("level")
   void hookSetWorld(ClientLevel var1);

   @Accessor("clientLevelTeardownInProgress")
   void hookSetDisconnecting(boolean var1);
}
