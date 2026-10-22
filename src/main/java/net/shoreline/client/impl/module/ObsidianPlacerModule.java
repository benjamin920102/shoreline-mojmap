package net.shoreline.client.impl.module;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.api.module.ModuleCategory;

public class ObsidianPlacerModule extends BlockPlacerModule {
   protected static final BlockState DEFAULT_OBSIDIAN_STATE = Blocks.OBSIDIAN.defaultBlockState();
   private static final List<Block> RESISTANT_BLOCKS = new LinkedList<Block>() {
      {
         this.add(Blocks.OBSIDIAN);
         this.add(Blocks.CRYING_OBSIDIAN);
         this.add(Blocks.ENDER_CHEST);
      }
   };

   public ObsidianPlacerModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
   }

   public ObsidianPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
   }

   protected ObsidianPlacerModule.BlockSlot getResistantBlockItem() {
      Set<ObsidianPlacerModule.BlockSlot> blockSlots = new HashSet<>();

      for (Block type : RESISTANT_BLOCKS) {
         int slot = this.getBlockItemSlot(type);
         if (slot != -1) {
            blockSlots.add(new ObsidianPlacerModule.BlockSlot(type, slot));
         }
      }

      ObsidianPlacerModule.BlockSlot slot = blockSlots.stream().filter(b -> b.block() == Blocks.OBSIDIAN).findFirst().orElse(null);
      if (slot != null) {
         return slot;
      } else {
         ObsidianPlacerModule.BlockSlot slot1 = blockSlots.stream().filter(b -> b.block() == Blocks.CRYING_OBSIDIAN).findFirst().orElse(null);
         if (slot1 != null) {
            return slot1;
         } else {
            ObsidianPlacerModule.BlockSlot slot2 = blockSlots.stream().filter(b -> b.block() == Blocks.ENDER_CHEST).findFirst().orElse(null);
            return slot2 != null ? slot2 : null;
         }
      }
   }

   public record BlockSlot(Block block, int slot) {
      @Override
      public boolean equals(Object obj) {
         return obj instanceof ObsidianPlacerModule.BlockSlot b && b.block() == this.block;
      }
   }
}
