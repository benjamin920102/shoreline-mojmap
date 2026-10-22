package net.shoreline.client.api.account.type.impl;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.User;
import net.minecraft.client.User.Type;
import net.shoreline.client.api.account.type.MinecraftAccount;

public record CrackedAccount(String username) implements MinecraftAccount {
   @Override
   public User login() {
      return new User(this.username(), UUID.randomUUID(), "", Optional.empty(), Optional.empty(), Type.LEGACY);
   }
}
