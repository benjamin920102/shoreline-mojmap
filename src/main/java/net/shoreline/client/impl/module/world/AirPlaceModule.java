package net.shoreline.client.impl.module.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.ItemUseEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorMinecraftClient;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public final class AirPlaceModule extends ToggleModule {
   private static AirPlaceModule INSTANCE;
   Config<Boolean> manualConfig = this.register(new BooleanConfig("Click", "Allow manual air place", true));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Place on air on grim", false));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to air place", 1.0F, 4.0F, 6.0F, NumberDisplay.DEFAULT));
   Config<Boolean> fluidsConfig = this.register(new BooleanConfig("Fluids", "Place against fluids", false));
   private int airPlaceTicks;

   public AirPlaceModule() {
      super("AirPlace", "Allows you to place blocks in the air", ModuleCategory.WORLD);
      INSTANCE = this;
   }

   public static AirPlaceModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.airPlaceTicks = 0;
   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (this.airPlaceTicks > 0) {
         this.airPlaceTicks--;
      }

      if (mc.player != null && mc.gameMode != null && this.manualConfig.getValue() && event.getStage() == StageEvent.EventStage.PRE) {
         if (!(mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos()))) {
            ItemStack stack = mc.player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && mc.options.keyUse.isDown()) {
               if (((AccessorMinecraftClient)mc).hookGetItemUseCooldown() == 0
                  && this.airPlaceTicks == 0
                  && !mc.player.isUsingItem()
                  && mc.player.pick(this.rangeConfig.getValue().floatValue(), 1.0F, this.fluidsConfig.getValue()) instanceof BlockHitResult blockHitResult) {
                  BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
                  if (!mc.level.isEmptyBlock(blockPos) || this.isEntityInBlockPos(blockPos)) {
                     return;
                  }

                  ((AccessorMinecraftClient)mc).hookSetItemUseCooldown(4);
                  this.airPlaceTicks = 4;
                  if (this.grimConfig.getValue()) {
                     Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                     mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, blockHitResult);
                     mc.player.swing(InteractionHand.MAIN_HAND, false);
                     Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
                     Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                  } else {
                     mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                     mc.player.swing(InteractionHand.MAIN_HAND);
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onItemUse(ItemUseEvent event) {
      if (this.airPlaceTicks > 0 && this.manualConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && this.manualConfig.getValue()) {
         if (!(mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos()))) {
            ItemStack stack = mc.player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
               if (mc.player.pick(this.rangeConfig.getValue().floatValue(), 1.0F, this.fluidsConfig.getValue()) instanceof BlockHitResult blockHitResult) {
                  BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
                  if (mc.level.isEmptyBlock(blockPos) && !this.isEntityInBlockPos(blockPos)) {
                     RenderBuffers.preRender();
                     RenderManager.renderBoundingBox(event.getMatrices(), blockPos, 1.5F, ColorsModule.getInstance().getRGB(145));
                     RenderBuffers.postRender();
                  }
               }
            }
         }
      }
   }

   private boolean isEntityInBlockPos(BlockPos blockPos) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity.getBoundingBox().intersects(new AABB(blockPos))) {
            return true;
         }
      }

      return false;
   }
}
