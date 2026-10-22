package net.shoreline.client.impl.module.movement;

import net.minecraft.world.level.block.Blocks;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.mixin.accessor.AccessorAbstractBlock;

public class IceSpeedModule extends ToggleModule {
   public IceSpeedModule() {
      super("IceSpeed", "Modifies the walking speed on ice", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.level != null) {
         ((AccessorAbstractBlock)Blocks.ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.PACKED_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.BLUE_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)Blocks.FROSTED_ICE).setSlipperiness(0.4F);
      }
   }

   @Override
   public void onDisable() {
      if (mc.level != null) {
         ((AccessorAbstractBlock)Blocks.ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.PACKED_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.BLUE_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)Blocks.FROSTED_ICE).setSlipperiness(0.98F);
      }
   }
}
