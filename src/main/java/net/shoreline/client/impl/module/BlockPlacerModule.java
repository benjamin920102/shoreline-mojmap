package net.shoreline.client.impl.module;

import java.util.function.Predicate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;

public class BlockPlacerModule extends CombatModule {
   protected Config<Boolean> strictDirectionConfig = this.register(new BooleanConfig("StrictDirection", "Places on visible sides only", false));
   protected Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   protected Config<Boolean> stopMotionConfig = this.register(new BooleanConfig("StopMotion", "Stops movement before placing", false));

   public BlockPlacerModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
      this.register(new Config[]{this.strictDirectionConfig, this.rotateConfig, this.stopMotionConfig});
   }

   public BlockPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
      this.register(new Config[]{this.strictDirectionConfig, this.rotateConfig, this.stopMotionConfig});
   }

   protected int getSlot(Predicate<ItemStack> filter) {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (!itemStack.isEmpty() && filter.test(itemStack)) {
            return i;
         }
      }

      return -1;
   }

   protected int getBlockItemSlot(Block block) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block) {
            return i;
         }
      }

      return -1;
   }

   public record BlockSlot(Block block, int slot) {
      @Override
      public boolean equals(Object obj) {
         return obj instanceof BlockPlacerModule.BlockSlot other ? other.block() == this.block : false;
      }
   }
}
