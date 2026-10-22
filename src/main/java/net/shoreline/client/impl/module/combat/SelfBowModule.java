package net.shoreline.client.impl.module.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TippedArrowItem;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.eventbus.annotation.EventListener;

public class SelfBowModule extends RotationModule {
   private static SelfBowModule INSTANCE;
   private int pullTicks;
   private final Set<MobEffectInstance> arrows = new HashSet<>();

   public SelfBowModule() {
      super("SelfBow", "Shoots player with beneficial tipped arrows", ModuleCategory.COMBAT, 755);
      INSTANCE = this;
   }

   public static SelfBowModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      mc.options.keyUse.setDown(false);
      this.arrows.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      int arrowSlot = -1;
      MobEffectInstance statusEffect = null;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() instanceof TippedArrowItem) {
            if (stack.getComponents().has(DataComponents.POTION_CONTENTS)) {
               for (MobEffectInstance effect : stack.getComponents().get(DataComponents.POTION_CONTENTS).getAllEffects()) {
                  MobEffect type = effect.getEffect().value();
                  if (type.isBeneficial() && !this.arrows.contains(effect)) {
                     arrowSlot = i;
                     statusEffect = effect;
                     break;
                  }
               }
            }

            if (arrowSlot != -1) {
               break;
            }
         }
      }

      int bowSlot = -1;

      for (int ix = 0; ix < 9; ix++) {
         ItemStack stack = mc.player.getInventory().getItem(ix);
         if (!stack.isEmpty() && stack.getItem() == Items.BOW) {
            bowSlot = ix;
            break;
         }
      }

      float pullTime = BowItem.getPowerForTime(this.pullTicks);
      if (mc.player.getMainHandItem().getItem() == Items.BOW && bowSlot != -1 && arrowSlot != -1) {
         this.setRotation(mc.player.getYRot(), -90.0F);
         if (arrowSlot != 9) {
            if (mc.player.containerMenu.getCarried().getItem() != Items.TIPPED_ARROW) {
               mc.gameMode.handleInventoryMouseClick(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, ClickType.PICKUP, mc.player);
            }

            if (mc.player.containerMenu.getCarried().getItem() == Items.TIPPED_ARROW) {
               mc.gameMode.handleInventoryMouseClick(0, 9, 0, ClickType.PICKUP, mc.player);
            }

            if (!mc.player.containerMenu.getCarried().isEmpty()) {
               mc.gameMode.handleInventoryMouseClick(0, arrowSlot < 9 ? arrowSlot + 36 : arrowSlot, 0, ClickType.PICKUP, mc.player);
            }
         }

         if (pullTime >= 0.15F) {
            this.arrows.add(statusEffect);
            mc.options.keyUse.setDown(false);
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
            mc.player.releaseUsingItem();
            this.pullTicks = 0;
         } else {
            mc.options.keyUse.setDown(true);
            this.pullTicks++;
         }
      } else {
         this.disable();
      }
   }
}
