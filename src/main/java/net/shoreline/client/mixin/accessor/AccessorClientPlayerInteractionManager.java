package net.shoreline.client.mixin.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface AccessorClientPlayerInteractionManager {
   @Invoker("ensureHasSentCarriedItem")
   void hookSyncSelectedSlot();

   @Invoker("performUseItemOn")
   InteractionResult hookInteractBlockInternal(LocalPlayer var1, InteractionHand var2, BlockHitResult var3);

   @Accessor("destroyProgress")
   float hookGetCurrentBreakingProgress();

   @Accessor("destroyProgress")
   void hookSetCurrentBreakingProgress(float var1);
}
