package net.shoreline.client.impl.module.combat;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.TickTimer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoXPModule extends RotationModule {
   private static AutoXPModule INSTANCE;
   Config<Boolean> multiTaskConfig = this.register(new BooleanConfig("MultiTask", "Allows you to throw xp while using items", false));
   Config<Float> delayConfig = this.register(new NumberConfig<>("Delay", "Delay to throw xp in ticks", 1.0F, 1.0F, 10.0F, NumberDisplay.DEFAULT));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of xp bottles to throw in one tick", 1, 1, 64));
   Config<Boolean> durabilityCheckConfig = this.register(
      new BooleanConfig("DurabilityCheck", "Check if your armor and held item durability is full then disables if it is", true)
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates the player while throwing xp", false));
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swings hand while throwing xp", false));
   private final TickTimer delayTimer = new TickTimer();

   public AutoXPModule() {
      super("AutoXP", "Automatically throws xp silently.", ModuleCategory.COMBAT, 850);
      INSTANCE = this;
   }

   public static AutoXPModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return String.valueOf(InventoryUtil.count(Items.EXPERIENCE_BOTTLE));
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && this.delayTimer.passed(this.delayConfig.getValue())) {
         if (!mc.player.isUsingItem() || this.multiTaskConfig.getValue()) {
            if (this.durabilityCheckConfig.getValue() && this.areItemsFullDura(mc.player)) {
               this.disable();
            } else {
               int slot = -1;

               for (int i = 0; i < 9; i++) {
                  ItemStack stack = mc.player.getInventory().getItem(i);
                  if (stack.getItem() instanceof ExperienceBottleItem) {
                     slot = i;
                     break;
                  }
               }

               if (slot == -1) {
                  this.disable();
               } else {
                  Managers.INVENTORY.setSlot(slot);
                  if (this.rotateConfig.getValue()) {
                     this.setRotation(mc.player.getYRot(), 90.0F);
                     if (this.isRotationBlocked()) {
                        return;
                     }
                  }

                  for (int ix = 0; ix < this.shiftTicksConfig.getValue(); ix++) {
                     Managers.NETWORK
                        .sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                     if (this.swingConfig.getValue()) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                     }
                  }

                  Managers.INVENTORY.syncToClient();
                  this.delayTimer.reset();
               }
            }
         }
      }
   }

   private boolean areItemsFullDura(Player player) {
      if (this.isItemFullDura(player.getMainHandItem()) && this.isItemFullDura(player.getOffhandItem())) {
         for (ItemStack stack : player.getArmorSlots()) {
            if (!this.isItemFullDura(stack)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isItemFullDura(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         int maxDura = stack.getMaxDamage();
         int currentDura = stack.getDamageValue();
         return currentDura == 0 || maxDura == 0;
      }
   }
}
