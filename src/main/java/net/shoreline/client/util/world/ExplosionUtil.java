package net.shoreline.client.util.world;

import java.util.Set;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.player.EnchantmentUtil;
import org.apache.commons.lang3.mutable.MutableInt;

public class ExplosionUtil implements Globals {
   public static double getDamageTo(Entity entity, Vec3 explosion, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, false, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, boolean ignoreTerrain, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, boolean ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, ExplosionUtil.IgnoreTerrain ignoreTerrain, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, ExplosionUtil.IgnoreTerrain ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(
      Entity entity, Vec3 explosion, ExplosionUtil.IgnoreTerrain ignoreTerrain, float power, int extrapolationTicks, boolean assumeBestArmor
   ) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3 vec3d2 = Vec3.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.xo) * extrapolationTicks;
         double oy = (y - entity.yo) * extrapolationTicks * 0.3;
         double oz = (z - entity.zo) * extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3(ox, oy, oz);
      }

      Vec3 vec3d = new Vec3(x, y, z);
      double d = Math.sqrt(vec3d.distanceToSqr(explosion));
      double ab = getExposure(explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain);
      double w = d / power;
      double ac = (1.0 - w) * ab;
      double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
      dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
      return Math.max(0.0, dmg);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, boolean ignoreTerrain, Set<BlockPos> ignoreBlocks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, 0, assumeBestArmor);
   }

   public static double getDamageTo(
      Entity entity, Vec3 explosion, boolean ignoreTerrain, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor
   ) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(
      Entity entity, Vec3 explosion, boolean ignoreTerrain, float power, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor
   ) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3 vec3d2 = Vec3.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.xo) * extrapolationTicks;
         double oy = (y - entity.yo) * extrapolationTicks * 0.3;
         double oz = (z - entity.zo) * extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3(ox, oy, oz);
      }

      Vec3 vec3d = new Vec3(x, y, z);
      double d = Math.sqrt(vec3d.distanceToSqr(explosion));
      double ab = getExposure(
         explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE, ignoreBlocks
      );
      double w = d / power;
      double ac = (1.0 - w) * ab;
      double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
      dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
      return Math.max(0.0, dmg);
   }

   public static double getDamageTo(
      Entity entity, Vec3 explosion, ExplosionUtil.IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks, boolean assumeBestArmor
   ) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, 0, assumeBestArmor);
   }

   public static double getDamageTo(
      Entity entity,
      Vec3 explosion,
      ExplosionUtil.IgnoreTerrain ignoreTerrain,
      float power,
      Set<BlockPos> ignoreBlocks,
      int extrapolationTicks,
      boolean assumeBestArmor
   ) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3 vec3d2 = Vec3.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.xo) * extrapolationTicks;
         double oy = (y - entity.yo) * extrapolationTicks * 0.3;
         double oz = (z - entity.zo) * extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3(ox, oy, oz);
      }

      Vec3 vec3d = new Vec3(x, y, z);
      double d = Math.sqrt(vec3d.distanceToSqr(explosion));
      double ab = getExposure(explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain, ignoreBlocks);
      double w = d / power;
      double ac = (1.0 - w) * ab;
      double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
      dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
      return Math.max(0.0, dmg);
   }

   public static double getDamageTo(Entity entity, Vec3 explosion, boolean ignoreTerrain, float power, int extrapolationTicks, boolean assumeBestArmor) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3 vec3d2 = Vec3.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.xo) * extrapolationTicks;
         double oy = (y - entity.yo) * extrapolationTicks * 0.3;
         double oz = (z - entity.zo) * extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3(ox, oy, oz);
      }

      Vec3 vec3d = new Vec3(x, y, z);
      double d = Math.sqrt(vec3d.distanceToSqr(explosion));
      double ab = getExposure(
         explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE
      );
      double w = d / power;
      double ac = (1.0 - w) * ab;
      double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
      dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
      return Math.max(0.0, dmg);
   }

   public static double getDamageToPos(Vec3 pos, Entity entity, Vec3 explosion, boolean ignoreTerrain, boolean assumeBestArmor) {
      AABB bb = entity.getBoundingBox();
      double dx = pos.x() - bb.minX;
      double dy = pos.y() - bb.minY;
      double dz = pos.z() - bb.minZ;
      AABB box = bb.move(dx, dy, dz);
      ExplosionUtil.RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE);
      double ab = getExposure(explosion, box, raycastFactory);
      double w = Math.sqrt(pos.distanceToSqr(explosion)) / 12.0;
      double ac = (1.0 - w) * ab;
      double dmg = (int)((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0);
      dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
      return Math.max(0.0, dmg);
   }

   private static double getReduction(Entity entity, DamageSource damageSource, double damage, boolean assumeBestArmor) {
      if (damageSource.scalesWithDifficulty()) {
         switch (mc.level.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0 + 1.0, damage);
               break;
            case HARD:
               damage *= 1.5;
         }
      }

      if (entity instanceof LivingEntity livingEntity) {
         damage = CombatRules.getDamageAfterAbsorb(
            livingEntity, (float)damage, damageSource, getArmor(livingEntity), (float)livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
         );
         damage = getResistanceReduction(livingEntity, damage);
         damage = getProtectionReduction(livingEntity, damage, damageSource, assumeBestArmor);
      }

      return Math.max(damage, 0.0);
   }

   private static float getArmor(LivingEntity entity) {
      return (float)Math.floor(entity.getAttributeValue(Attributes.ARMOR));
   }

   private static float getProtectionReduction(Entity player, double damage, DamageSource source, boolean assumeBestArmor) {
      if (player instanceof LivingEntity livingEntity) {
         float protLevel = getProtectionAmount(livingEntity.getArmorSlots(), assumeBestArmor);
         return CombatRules.getDamageAfterMagicAbsorb((float)damage, protLevel);
      } else {
         return 0.0F;
      }
   }

   private static float getProtectionAmount(Iterable<ItemStack> equipment, boolean assumeBestArmor) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach(
         i -> {
            if (assumeBestArmor && EnchantmentUtil.isFakeEnchant2b2t(i)) {
               mutableInt.add(i.getItem() instanceof ArmorItem armorItem && armorItem.getType() == Type.LEGGINGS ? 8 : 4);
            } else {
               int modifierBlast = EnchantmentHelper.getItemEnchantmentLevel(
                  mc.level.registryAccess().registryOrThrow(Enchantments.BLAST_PROTECTION.registryKey()).getHolder(Enchantments.BLAST_PROTECTION).get(), i
               );
               int modifier = EnchantmentHelper.getItemEnchantmentLevel(
                  mc.level.registryAccess().registryOrThrow(Enchantments.PROTECTION.registryKey()).getHolder(Enchantments.PROTECTION).get(), i
               );
               mutableInt.add(modifierBlast * 2 + modifier);
            }
         }
      );
      return mutableInt.intValue();
   }

   private static double getResistanceReduction(LivingEntity player, double damage) {
      MobEffectInstance resistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
      if (resistance != null) {
         int lvl = resistance.getAmplifier() + 1;
         damage *= 1.0F - lvl * 0.2F;
      }

      return Math.max(damage, 0.0);
   }

   private static <T extends LivingEntity> AttributeSupplier getDefaultForEntity(T entity) {
      return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>)entity.getType());
   }

   private static float getExposure(Vec3 source, AABB box, ExplosionUtil.IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
      ExplosionUtil.RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain, ignoreBlocks);
      return getExposure(source, box, raycastFactory);
   }

   private static float getExposure(Vec3 source, AABB box, ExplosionUtil.IgnoreTerrain ignoreTerrain) {
      ExplosionUtil.RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain);
      return getExposure(source, box, raycastFactory);
   }

   private static float getExposure(Vec3 source, AABB box, ExplosionUtil.RaycastFactory raycastFactory) {
      double xDiff = box.maxX - box.minX;
      double yDiff = box.maxY - box.minY;
      double zDiff = box.maxZ - box.minZ;
      double xStep = 1.0 / (xDiff * 2.0 + 1.0);
      double yStep = 1.0 / (yDiff * 2.0 + 1.0);
      double zStep = 1.0 / (zDiff * 2.0 + 1.0);
      if (xStep > 0.0 && yStep > 0.0 && zStep > 0.0) {
         int misses = 0;
         int hits = 0;
         double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
         double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
         xStep *= xDiff;
         yStep *= yDiff;
         zStep *= zDiff;
         double startX = box.minX + xOffset;
         double startY = box.minY;
         double startZ = box.minZ + zOffset;
         double endX = box.maxX + xOffset;
         double endY = box.maxY;
         double endZ = box.maxZ + zOffset;

         for (double x = startX; x <= endX; x += xStep) {
            for (double y = startY; y <= endY; y += yStep) {
               for (double z = startZ; z <= endZ; z += zStep) {
                  Vec3 position = new Vec3(x, y, z);
                  if (raycast(new ExplosionUtil.ExposureRaycastContext(position, source), raycastFactory) == null) {
                     misses++;
                  }

                  hits++;
               }
            }
         }

         return (float)misses / hits;
      } else {
         return 0.0F;
      }
   }

   private static ExplosionUtil.RaycastFactory getRaycastFactory(ExplosionUtil.IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
      if (ignoreTerrain == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (context, blockPos) -> {
            if (ignoreBlocks.contains(blockPos)) {
               return null;
            } else {
               BlockState blockState = mc.level.getBlockState(blockPos);
               return blockState.getBlock().getExplosionResistance() < 600.0F
                  ? null
                  : blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            }
         };
      } else {
         return ignoreTerrain == ExplosionUtil.IgnoreTerrain.ALL ? (context, blockPos) -> null : (context, blockPos) -> {
            if (ignoreBlocks.contains(blockPos)) {
               return null;
            } else {
               BlockState blockState = mc.level.getBlockState(blockPos);
               return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            }
         };
      }
   }

   private static ExplosionUtil.RaycastFactory getRaycastFactory(ExplosionUtil.IgnoreTerrain ignoreTerrain) {
      if (ignoreTerrain == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (context, blockPos) -> {
            BlockState blockState = mc.level.getBlockState(blockPos);
            return blockState.getBlock().getExplosionResistance() < 600.0F
               ? null
               : blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
         };
      } else {
         return ignoreTerrain == ExplosionUtil.IgnoreTerrain.ALL ? (context, blockPos) -> null : (context, blockPos) -> {
            BlockState blockState = mc.level.getBlockState(blockPos);
            return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
         };
      }
   }

   private static BlockHitResult raycast(ExplosionUtil.ExposureRaycastContext context, ExplosionUtil.RaycastFactory raycastFactory) {
      return BlockGetter.traverseBlocks(context.start, context.end, context, raycastFactory, ctx -> null);
   }

   public record ExposureRaycastContext(Vec3 start, Vec3 end) {
   }

   public static enum IgnoreTerrain {
      ALL,
      BLAST,
      NONE;
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<ExplosionUtil.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
