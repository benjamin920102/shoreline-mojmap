package net.shoreline.client.api.account.type.impl;

import net.minecraft.client.User;
import net.shoreline.client.api.account.type.MinecraftAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * @author xgraza
 * @since 03/31/24
 */
public record CrackedAccount(String username) implements MinecraftAccount
{
    @Override
    public User login()
    {
        return new User(username(),
                UUID.randomUUID(), "", Optional.empty(),
                Optional.empty(), User.Type.LEGACY);
    }
}
