package net.shoreline.client.impl.module.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.MouseClickEvent;
import net.shoreline.client.impl.module.render.FreecamModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.RayCastUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class MiddleClickModule extends ToggleModule {
   Config<Boolean> friendConfig = this.register(new BooleanConfig("Friend", "Friends players when middle click", true));
   Config<Boolean> pearlConfig = this.register(new BooleanConfig("Pearl", "Throws a pearl when middle click", true));
   Config<Boolean> fireworkConfig = this.register(new BooleanConfig("Firework", "Uses firework to boost elytra when middle click", false));

   public MiddleClickModule() {
      super("MiddleClick", "Adds an additional bind on the mouse middle button", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onMouseClick(MouseClickEvent event) {
      if (mc.player != null && mc.gameMode != null) {
         if (event.getButton() == 2 && event.getAction() == 1 && mc.screen == null) {
            double d = mc.player.entityInteractionRange();
            HitResult result = FreecamModule.getInstance().isEnabled()
               ? RayCastUtil.raycastEntity(d, FreecamModule.getInstance().getCameraPosition(), FreecamModule.getInstance().getCameraRotations())
               : RayCastUtil.raycastEntity(d);
            if (!(
               result != null
                  && result.getType() == Type.ENTITY
                  && this.friendConfig.getValue()
                  && ((EntityHitResult)result).getEntity() instanceof Player target
            )) {
               Item item = null;
               if (mc.player.isFallFlying() && this.fireworkConfig.getValue()) {
                  item = Items.FIREWORK_ROCKET;
               } else if (this.pearlConfig.getValue()) {
                  item = Items.ENDER_PEARL;
               }

               if (item == null) {
                  return;
               }

               int slot = -1;

               for (int i = 0; i < 45; i++) {
                  ItemStack stack = mc.player.getInventory().getItem(i);
                  if (stack.getItem() == item) {
                     slot = i;
                     break;
                  }
               }

               if (slot == -1) {
                  return;
               }

               if (slot < 9) {
                  Managers.INVENTORY.setSlot(slot);
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                  Managers.INVENTORY.syncToClient();
               } else {
                  mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                  mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().selected + 36, 0, ClickType.PICKUP, mc.player);
                  mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
               }
            } else if (Managers.SOCIAL.isFriend(target.getName())) {
               Managers.SOCIAL.remove(target.getName());
            } else {
               Managers.SOCIAL.addFriend(target.getName());
            }
         }
      }
   }
}
