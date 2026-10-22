package net.shoreline.client.impl.module;

import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.init.Managers;

public class CombatModule extends RotationModule {
   protected Config<Boolean> multitaskConfig = this.register(new BooleanConfig("Multitask", "Allows actions while using items", false));

   public CombatModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
   }

   public CombatModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
   }

   public Player getClosestPlayer(double range) {
      return mc.level
         .players()
         .stream()
         .filter(e -> !(e instanceof LocalPlayer) && !e.isSpectator())
         .filter(e -> mc.player.distanceToSqr(e) <= range * range)
         .filter(e -> !Managers.SOCIAL.isFriend(e.getName().getString()))
         .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e)))
         .orElse(null);
   }

   public Player getClosestPlayer(Predicate<AbstractClientPlayer> entityPredicate, double range) {
      return mc.level
         .players()
         .stream()
         .filter(e -> !(e instanceof LocalPlayer) && !e.isSpectator())
         .filter(entityPredicate)
         .filter(e -> mc.player.distanceToSqr(e) <= range * range)
         .filter(e -> !Managers.SOCIAL.isFriend(e.getName().getString()))
         .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e)))
         .orElse(null);
   }

   public boolean checkMultitask() {
      return this.checkMultitask(false);
   }

   public boolean checkMultitask(boolean checkOffhand) {
      return checkOffhand && mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND ? false : mc.player.isUsingItem();
   }
}
