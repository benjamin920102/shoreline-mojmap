package net.shoreline.client.impl.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.annotation.EventListener;

public class BedAuraModule extends ToggleModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to place beds", 1.0F, 4.5F, 6.0F));
   Config<Float> wallRangeConfig = this.register(new NumberConfig<>("WallRange", "Range to attack entities through walls", 1.0F, 4.5F, 6.0F));
   Config<BedAuraModule.Priority> priorityConfig = this.register(
      new EnumConfig<>("Priority", "The value to prioritize when searching for targets", BedAuraModule.Priority.HEALTH, BedAuraModule.Priority.values())
   );
   Config<Boolean> armorCheckConfig = this.register(new BooleanConfig("ArmorCheck", "Checks if target has armor before attacking", false));
   private Player lastTarget;
   private Player currentTarget;

   public BedAuraModule() {
      super("BedAura", "Automatically places and explodes beds (designed for 1.13+)", ModuleCategory.COMBAT);
   }

   @Override
   public void onEnable() {
      if (!this.shouldRun()) {
         this.disable();
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      this.currentTarget = null;
      if (!this.shouldRun()) {
         this.disable();
      } else {
         List<Player> possibleTargets = this.getPossibleTargets();
         if (possibleTargets.isEmpty()) {
            this.lastTarget = null;
            this.currentTarget = null;
         } else {
            if (this.lastTarget != null && possibleTargets.contains(this.lastTarget)) {
               this.currentTarget = this.lastTarget;
            } else {
               this.currentTarget = possibleTargets.get(0);
            }
         }
      }
   }

   private List<Player> getPossibleTargets() {
      List<Player> possibleTargets = new ArrayList<>();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof Player playerEntity
            && playerEntity != mc.player
            && playerEntity.isAlive()
            && !Managers.SOCIAL.isFriend(playerEntity.getName())
            && !playerEntity.isCreative()
            && (!this.armorCheckConfig.getValue() || playerEntity.getArmorSlots().iterator().hasNext())
            && !(mc.player.position().distanceTo(entity.position()) > 8.0)) {
            List<BlockPos> lowerSurroundBlocks = SurroundModule.getInstance().getSurroundNoDown(playerEntity);
            List<BlockPos> middleSurroundBlocks = new ArrayList<>();
            lowerSurroundBlocks.forEach(blockpos -> middleSurroundBlocks.add(blockpos.above()));
            List<BlockPos> upperSurroundBlocks = new ArrayList<>();
            lowerSurroundBlocks.forEach(blockpos -> upperSurroundBlocks.add(blockpos.below()));
            possibleTargets.add(playerEntity);
         }
      }

      possibleTargets.sort(this.priorityConfig.getValue().comparator);
      return possibleTargets;
   }

   private void test(LocalPlayer target) {
   }

   private boolean shouldRun() {
      return mc.player == null || mc.level == null ? false : BedBlock.canSetSpawn(mc.level);
   }

   private static float getArmorDurability(LivingEntity e) {
      float edmg = 0.0F;
      float emax = 0.0F;

      for (ItemStack armor : e.getArmorSlots()) {
         if (armor != null && !armor.isEmpty()) {
            edmg += armor.getDamageValue();
            emax += armor.getMaxDamage();
         }
      }

      return 100.0F - edmg / emax;
   }

   public static enum Priority {
      HEALTH(Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount())),
      DISTANCE(Comparator.comparingDouble(entity -> Globals.mc.player.position().distanceTo(entity.position()))),
      ARMOR(Comparator.comparingDouble(BedAuraModule::getArmorDurability));

      private final Comparator<Player> comparator;

      private Priority(Comparator<Player> comparator) {
         this.comparator = comparator;
      }
   }
}
