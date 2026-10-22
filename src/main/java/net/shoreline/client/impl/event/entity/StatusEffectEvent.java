package net.shoreline.client.impl.event.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.shoreline.eventbus.event.Event;

public class StatusEffectEvent extends Event {
   private final MobEffectInstance statusEffectInstance;

   public StatusEffectEvent(MobEffectInstance statusEffectInstance) {
      this.statusEffectInstance = statusEffectInstance;
   }

   public MobEffectInstance getStatusEffect() {
      return this.statusEffectInstance;
   }

   public static class Add extends StatusEffectEvent {
      public Add(MobEffectInstance statusEffectInstance) {
         super(statusEffectInstance);
      }
   }

   public static class Remove extends StatusEffectEvent {
      private final Holder<MobEffect> type;

      public Remove(Holder<MobEffect> type) {
         super(null);
         this.type = type;
      }

      public Holder<MobEffect> getType() {
         return this.type;
      }
   }
}
