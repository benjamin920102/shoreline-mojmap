package net.shoreline.client.impl.module.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.network.SetCurrentHandEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.MovementUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;

public final class FastEatModule extends ToggleModule {
   Config<FastEatModule.Mode> modeConfig = this.register(new EnumConfig<>("Mode", "The bypass mode", FastEatModule.Mode.VANILLA, FastEatModule.Mode.values()));
   Config<Integer> ticksConfig = this.register(new NumberConfig<>("Ticks", "The amount of ticks to have 'consumed' an item before fast eating", 0, 10, 30));
   private int packets;

   public FastEatModule() {
      super("FastEat", "Allows you to consume items faster", ModuleCategory.WORLD);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!MovementUtil.isMoving() && mc.player.onGround()) {
         this.packets++;
         if (this.packets > 100) {
            this.packets = 100;
         }
      } else {
         this.packets--;
         if (this.packets <= 0) {
            this.packets = 0;
         }
      }

      if (mc.player.isUsingItem()) {
         ItemStack stack = mc.player.getItemInHand(mc.player.getUsedItemHand());
         if (!stack.isEmpty() && stack.getItem().components().has(DataComponents.FOOD)) {
            int timeUsed = mc.player.getTicksUsingItem();
            if (timeUsed >= this.ticksConfig.getValue()) {
               int usePackets = 32 - timeUsed;

               for (int i = 0; i < usePackets; i++) {
                  this.modeConfig.getValue().ordinal();
               }
            }
         }
      }
   }

   @EventListener
   public void onSetCurrentHand(SetCurrentHandEvent event) {
      if (this.modeConfig.getValue() == FastEatModule.Mode.SHIFT) {
         ItemStack stack = event.getStackInHand();
         if (!stack.getItem().components().has(DataComponents.FOOD) && !(stack.getItem() instanceof PotionItem)) {
            return;
         }

         int maxUseTime = stack.getUseDuration(mc.player);
         if (this.packets < maxUseTime) {
            return;
         }

         for (int i = 0; i < maxUseTime; i++) {
            Managers.NETWORK.sendPacket(new Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.onGround()));
            this.packets -= maxUseTime;
         }

         event.cancel();
         stack.getItem().finishUsingItem(stack, mc.level, mc.player);
      }
   }

   private static enum Mode {
      VANILLA,
      SHIFT,
      GRIM;
   }
}
