package net.shoreline.client.impl.module.combat;

import java.util.PriorityQueue;
import java.util.Queue;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoArmorModule extends ToggleModule {
   Config<AutoArmorModule.Priority> priorityConfig = this.register(
      new EnumConfig<>("Priority", "Armor enchantment priority", AutoArmorModule.Priority.BLAST_PROTECTION, AutoArmorModule.Priority.values())
   );
   Config<Float> minDurabilityConfig = this.register(
      new NumberConfig<>("MinDurability", "Durability percent to replace armor", 0.0F, 0.0F, 20.0F, NumberDisplay.PERCENT)
   );
   Config<Boolean> elytraPriorityConfig = this.register(new BooleanConfig("ElytraPriority", "Prioritizes existing elytras in the chestplate armor slot", true));
   Config<Boolean> blastLeggingsConfig = this.register(new BooleanConfig("Leggings-BlastPriority", "Prioritizes Blast Protection leggings", true));
   Config<Boolean> noBindingConfig = this.register(new BooleanConfig("NoBinding", "Avoids armor with the Curse of Binding enchantment", true));
   Config<Boolean> inventoryConfig = this.register(new BooleanConfig("AllowInventory", "Allows armor to be swapped while in the inventory menu", false));
   private final Queue<AutoArmorModule.ArmorSlot> helmet = new PriorityQueue<>();
   private final Queue<AutoArmorModule.ArmorSlot> chestplate = new PriorityQueue<>();
   private final Queue<AutoArmorModule.ArmorSlot> leggings = new PriorityQueue<>();
   private final Queue<AutoArmorModule.ArmorSlot> boots = new PriorityQueue<>();

   public AutoArmorModule() {
      super("AutoArmor", "Automatically replaces armor pieces", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.screen == null || mc.screen instanceof InventoryScreen && this.inventoryConfig.getValue()) {
         this.helmet.clear();
         this.chestplate.clear();
         this.leggings.clear();
         this.boots.clear();

         for (int j = 0; j < 36; j++) {
            ItemStack stack = mc.player.getInventory().getItem(j);
            if (!stack.isEmpty()
               && stack.getItem() instanceof ArmorItem armor
               && (!this.noBindingConfig.getValue() || !this.hasEnchantment(stack, Enchantments.BINDING_CURSE))) {
               int index = armor.getEquipmentSlot().getIndex();
               float dura = (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
               if (!(dura < this.minDurabilityConfig.getValue())) {
                  AutoArmorModule.ArmorSlot data = new AutoArmorModule.ArmorSlot(index, j, stack);
                  switch (index) {
                     case 0:
                        this.helmet.add(data);
                        break;
                     case 1:
                        this.chestplate.add(data);
                        break;
                     case 2:
                        this.leggings.add(data);
                        break;
                     case 3:
                        this.boots.add(data);
                  }
               }
            }
         }

         for (int i = 0; i < 4; i++) {
            ItemStack armorStack = mc.player.getInventory().getArmor(i);
            if (!this.elytraPriorityConfig.getValue() || armorStack.getItem() != Items.ELYTRA) {
               float armorDura = (float)(armorStack.getMaxDamage() - armorStack.getDamageValue()) / armorStack.getMaxDamage();
               if (armorStack.isEmpty() && !(armorDura >= this.minDurabilityConfig.getValue())) {
                  switch (i) {
                     case 0:
                        if (!this.helmet.isEmpty()) {
                           AutoArmorModule.ArmorSlot helmetSlot = this.helmet.poll();
                           this.swapArmor(helmetSlot.getType(), helmetSlot.getSlot());
                        }
                        break;
                     case 1:
                        if (!this.chestplate.isEmpty()) {
                           AutoArmorModule.ArmorSlot chestSlot = this.chestplate.poll();
                           this.swapArmor(chestSlot.getType(), chestSlot.getSlot());
                        }
                        break;
                     case 2:
                        if (!this.leggings.isEmpty()) {
                           AutoArmorModule.ArmorSlot leggingsSlot = this.leggings.poll();
                           this.swapArmor(leggingsSlot.getType(), leggingsSlot.getSlot());
                        }
                        break;
                     case 3:
                        if (!this.boots.isEmpty()) {
                           AutoArmorModule.ArmorSlot bootsSlot = this.boots.poll();
                           this.swapArmor(bootsSlot.getType(), bootsSlot.getSlot());
                        }
                  }
               }
            }
         }
      }
   }

   public void swapArmor(int armorSlot, int slot) {
      ItemStack stack = mc.player.getInventory().getArmor(armorSlot);
      armorSlot = 8 - armorSlot;
      Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
      boolean rt = !stack.isEmpty();
      Managers.INVENTORY.pickupSlot(armorSlot);
      if (rt) {
         Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
      }
   }

   public boolean hasEnchantment(ItemStack armorStack, ResourceKey<Enchantment> enchantment) {
      if (armorStack.getComponents().has(DataComponents.ENCHANTMENTS)) {
         for (Holder<Enchantment> entry : armorStack.getComponents().get(DataComponents.ENCHANTMENTS).keySet()) {
            if (entry.unwrapKey().isPresent() && entry.unwrapKey().get().equals(enchantment)) {
               return true;
            }
         }
      }

      return false;
   }

   public class ArmorSlot implements Comparable<AutoArmorModule.ArmorSlot> {
      private final int armorType;
      private final int slot;
      private final ItemStack armorStack;

      public ArmorSlot(int armorType, int slot, ItemStack armorStack) {
         this.armorType = armorType;
         this.slot = slot;
         this.armorStack = armorStack;
      }

      public int compareTo(AutoArmorModule.ArmorSlot other) {
         if (this.armorType != other.armorType) {
            return 0;
         } else {
            ItemStack otherStack = other.getArmorStack();
            ArmorItem armorItem = (ArmorItem)this.armorStack.getItem();
            ArmorItem otherItem = (ArmorItem)otherStack.getItem();
            int durabilityDiff = armorItem.getMaterial().value().getDefense(armorItem.getType())
               - otherItem.getMaterial().value().getDefense(otherItem.getType());
            if (durabilityDiff != 0) {
               return durabilityDiff;
            } else {
               ResourceKey<Enchantment> enchantment = AutoArmorModule.this.priorityConfig.getValue().getEnchantment();
               if (AutoArmorModule.this.blastLeggingsConfig.getValue()
                  && this.armorType == 2
                  && AutoArmorModule.this.hasEnchantment(this.armorStack, Enchantments.BLAST_PROTECTION)) {
                  return -1;
               } else if (AutoArmorModule.this.hasEnchantment(this.armorStack, enchantment)) {
                  return AutoArmorModule.this.hasEnchantment(otherStack, enchantment) ? 0 : -1;
               } else {
                  return AutoArmorModule.this.hasEnchantment(otherStack, enchantment) ? 1 : 0;
               }
            }
         }
      }

      public ItemStack getArmorStack() {
         return this.armorStack;
      }

      public int getType() {
         return this.armorType;
      }

      public int getSlot() {
         return this.slot;
      }
   }

   public static enum Priority {
      BLAST_PROTECTION(Enchantments.BLAST_PROTECTION),
      PROTECTION(Enchantments.PROTECTION),
      PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION);

      private final ResourceKey<Enchantment> enchant;

      private Priority(ResourceKey<Enchantment> enchant) {
         this.enchant = enchant;
      }

      public ResourceKey<Enchantment> getEnchantment() {
         return this.enchant;
      }
   }
}
