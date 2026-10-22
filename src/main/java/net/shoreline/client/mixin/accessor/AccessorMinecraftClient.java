package net.shoreline.client.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author linus
 * @see Minecraft
 * @since 1.0
 */
@Mixin(Minecraft.class)
public interface AccessorMinecraftClient
{
    /**
     * @param itemUseCooldown
     */
    @Accessor("rightClickDelay")
    void hookSetItemUseCooldown(int itemUseCooldown);

    /**
     * @return
     */
    @Accessor("rightClickDelay")
    int hookGetItemUseCooldown();

    /**
     * @param attackCooldown
     */
    @Accessor("missTime")
    void hookSetAttackCooldown(int attackCooldown);

    @Invoker("startUseItem")
    void hookDoItemUse();

    @Accessor("user")
    @Final
    @Mutable
    void setSession(User session);

    @Accessor("level")
    void hookSetWorld(ClientLevel world);

    @Accessor("clientLevelTeardownInProgress")
    void hookSetDisconnecting(boolean disconnecting);
}
