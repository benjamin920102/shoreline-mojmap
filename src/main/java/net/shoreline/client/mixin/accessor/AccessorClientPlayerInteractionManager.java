package net.shoreline.client.mixin.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author linus
 * @see MultiPlayerGameMode
 * @since 1.0
 */
@Mixin(MultiPlayerGameMode.class)
public interface AccessorClientPlayerInteractionManager
{
    /**
     *
     */
    @Invoker("ensureHasSentCarriedItem")
    void hookSyncSelectedSlot();

    @Invoker("performUseItemOn")
    InteractionResult hookInteractBlockInternal(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult);

    /**
     * @return
     */
    @Accessor("destroyProgress")
    float hookGetCurrentBreakingProgress();

    /**
     * @param currentBreakingProgress
     */
    @Accessor("destroyProgress")
    void hookSetCurrentBreakingProgress(float currentBreakingProgress);
}
