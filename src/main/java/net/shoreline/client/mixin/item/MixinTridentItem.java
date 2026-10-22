package net.shoreline.client.mixin.item;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.item.TridentPullbackEvent;
import net.shoreline.client.impl.event.item.TridentWaterEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TridentItem.class})
public abstract class MixinTridentItem implements Globals {
   @Shadow
   public abstract int getUseDuration(ItemStack var1, LivingEntity var2);

   @Shadow
   protected static boolean isTooDamagedToUse(ItemStack stack) {
      return false;
   }

   @Inject(
      method = {"use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
      TridentWaterEvent tridentWaterEvent = new TridentWaterEvent();
      EventBus.INSTANCE.dispatch(tridentWaterEvent);
      if (tridentWaterEvent.isCanceled()) {
         cir.cancel();
         ItemStack itemStack = user.getItemInHand(hand);
         if (itemStack.getDamageValue() >= itemStack.getMaxDamage() - 1) {
            cir.setReturnValue(InteractionResultHolder.fail(itemStack));
            return;
         }

         user.startUsingItem(hand);
         cir.setReturnValue(InteractionResultHolder.consume(itemStack));
      }
   }

   @Inject(
      method = {"releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookOnStoppedUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
      if (user instanceof Player playerEntity) {
         int var6 = this.getUseDuration(stack, user) - remainingUseTicks;
         TridentPullbackEvent tridentPullbackEvent = new TridentPullbackEvent();
         EventBus.INSTANCE.dispatch(tridentPullbackEvent);
         if (tridentPullbackEvent.isCanceled() || var6 >= 10) {
            TridentWaterEvent tridentWaterEvent = new TridentWaterEvent();
            EventBus.INSTANCE.dispatch(tridentWaterEvent);
            if (tridentWaterEvent.isCanceled()) {
               ci.cancel();
               if (var6 >= 10) {
                  float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, playerEntity);
                  if ((!(f > 0.0F) || playerEntity.isInWaterOrRain()) && !isTooDamagedToUse(stack)) {
                     Holder<SoundEvent> registryEntry = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                        .orElse(SoundEvents.TRIDENT_THROW);
                     if (!world.isClientSide) {
                        stack.hurtAndBreak(1, playerEntity, LivingEntity.getSlotForHand(user.getUsedItemHand()));
                        if (f == 0.0F) {
                           ThrownTrident tridentEntity = new ThrownTrident(world, playerEntity, stack);
                           tridentEntity.shootFromRotation(playerEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 2.5F, 1.0F);
                           if (playerEntity.hasInfiniteMaterials()) {
                              tridentEntity.pickup = Pickup.CREATIVE_ONLY;
                           }

                           world.addFreshEntity(tridentEntity);
                           world.playSound((Player)null, tridentEntity, registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                           if (!playerEntity.hasInfiniteMaterials()) {
                              playerEntity.getInventory().removeItem(stack);
                           }
                        }
                     }

                     playerEntity.awardStat(Stats.ITEM_USED.get((TridentItem)(Object)this));
                     if (f > 0.0F) {
                        float g = playerEntity.getYRot();
                        float h = playerEntity.getXRot();
                        float j = -Mth.sin(g * (float) (Math.PI / 180.0)) * Mth.cos(h * (float) (Math.PI / 180.0));
                        float k = -Mth.sin(h * (float) (Math.PI / 180.0));
                        float l = Mth.cos(g * (float) (Math.PI / 180.0)) * Mth.cos(h * (float) (Math.PI / 180.0));
                        float m = Mth.sqrt(j * j + k * k + l * l);
                        j *= f / m;
                        k *= f / m;
                        l *= f / m;
                        playerEntity.push(j, k, l);
                        playerEntity.startAutoSpinAttack(20, 8.0F, stack);
                        if (playerEntity.onGround()) {
                           float n = 1.1999999F;
                           playerEntity.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
                        }

                        world.playSound((Player)null, playerEntity, registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                     }
                  }
               }
            }
         }
      }
   }
}
