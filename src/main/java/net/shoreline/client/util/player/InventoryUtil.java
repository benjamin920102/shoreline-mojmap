package net.shoreline.client.util.player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.shoreline.client.util.Globals;

public class InventoryUtil implements Globals {
   public static boolean isHolding32k() {
      return isHolding32k(1000);
   }

   public static boolean isHolding32k(int lvl) {
      ItemStack mainhand = mc.player.getMainHandItem();
      return EnchantmentUtil.getLevel(mainhand, Enchantments.SHARPNESS) >= lvl;
   }

   public static boolean hasItemInInventory(Item item, boolean hotbar) {
      int startSlot = hotbar ? 0 : 9;

      for (int i = startSlot; i < 36; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (!itemStack.isEmpty() && itemStack.getItem() == item) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasItemInHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (!itemStack.isEmpty() && itemStack.getItem() == item) {
            return true;
         }
      }

      return false;
   }

   public static int count(Item item) {
      if (mc.player == null) {
         return 0;
      } else {
         ItemStack offhandStack = mc.player.getOffhandItem();
         int itemCount = offhandStack.getItem() == item ? offhandStack.getCount() : 0;

         for (int i = 0; i < 36; i++) {
            ItemStack slot = mc.player.getInventory().getItem(i);
            if (slot.getItem() == item) {
               itemCount += slot.getCount();
            }
         }

         return itemCount;
      }
   }
}
