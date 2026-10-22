package net.shoreline.client.impl.module.movement;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.block.BlockSlipperinessEvent;
import net.shoreline.client.impl.event.block.SteppedOnSlimeBlockEvent;
import net.shoreline.client.impl.event.entity.SlowMovementEvent;
import net.shoreline.client.impl.event.entity.VelocityMultiplierEvent;
import net.shoreline.client.impl.event.network.MovementSlowdownEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.event.network.SetCurrentHandEvent;
import net.shoreline.client.impl.event.network.StrafeFixEvent;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorKeyBinding;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class NoSlowModule extends ToggleModule {
   public static NoSlowModule INSTANCE;
   Config<Boolean> strictConfig = this.register(new BooleanConfig("Strict", "Strict NCP bypass for ground slowdowns", false));
   Config<Boolean> airStrictConfig = this.register(new BooleanConfig("AirStrict", "Strict NCP bypass for air slowdowns", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Strict Grim bypass for slowdown", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Strict GrimV3 bypass for slowdown", false));
   Config<Boolean> strafeFixConfig = this.register(new BooleanConfig("StrafeFix", "Old NCP bypass for strafe", false));
   Config<Boolean> inventoryMoveConfig = this.register(new BooleanConfig("InventoryMove", "Allows the player to move while in inventories or screens", true));
   Config<Boolean> arrowMoveConfig = this.register(
      new BooleanConfig("ArrowMove", "Allows the player to look while in inventories or screens by using the arrow keys", false)
   );
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Removes the slowdown effect caused by using items", true));
   Config<Boolean> sneakConfig = this.register(new BooleanConfig("Sneak", "Removes sneak slowdown", false));
   Config<Boolean> crawlConfig = this.register(new BooleanConfig("Crawl", "Removes crawl slowdown", false));
   Config<Boolean> shieldsConfig = this.register(new BooleanConfig("Shields", "Removes the slowdown effect caused by shields", true));
   Config<Boolean> websConfig = this.register(new BooleanConfig("Webs", "Removes the slowdown caused when moving through webs", false));
   Config<Boolean> berryBushConfig = this.register(new BooleanConfig("BerryBush", "Removes the slowdown caused when moving through webs", false));
   Config<Float> webSpeedConfig = this.register(
      new NumberConfig<>("WebMultiplier", "Speed to fall through webs", 0.0F, 1.0F, 1.0F, () -> this.websConfig.getValue() || this.berryBushConfig.getValue())
   );
   Config<Boolean> soulsandConfig = this.register(new BooleanConfig("SoulSand", "Removes the slowdown effect caused by walking over SoulSand blocks", false));
   Config<Boolean> honeyblockConfig = this.register(new BooleanConfig("HoneyBlock", "Removes the slowdown effect caused by walking over Honey blocks", false));
   Config<Boolean> slimeblockConfig = this.register(new BooleanConfig("SlimeBlock", "Removes the slowdown effect caused by walking over Slime blocks", false));
   private boolean sneaking;

   public NoSlowModule() {
      super("NoSlow", "Prevents items from slowing down player", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static NoSlowModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      if (this.airStrictConfig.getValue() && this.sneaking) {
         Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.RELEASE_SHIFT_KEY));
      }

      this.sneaking = false;
      Managers.TICK.setClientTick(1.0F);
   }

   @EventListener
   public void onSetCurrentHand(SetCurrentHandEvent event) {
      if (this.airStrictConfig.getValue() && !this.sneaking && this.checkSlowed()) {
         this.sneaking = true;
         Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.PRESS_SHIFT_KEY));
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE
         && this.grimConfig.getValue()
         && mc.player.isUsingItem()
         && !mc.player.isShiftKeyDown()
         && this.itemsConfig.getValue()) {
         if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND && this.checkStack(mc.player.getMainHandItem())) {
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
         } else if (this.checkStack(mc.player.getOffhandItem())) {
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
         }
      }
   }

   private boolean checkStack(ItemStack stack) {
      return !stack.getComponents().has(DataComponents.FOOD)
         && stack.getItem() != Items.BOW
         && stack.getItem() != Items.CROSSBOW
         && stack.getItem() != Items.SHIELD;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.airStrictConfig.getValue() && !mc.player.isUsingItem()) {
            this.sneaking = false;
            Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.RELEASE_SHIFT_KEY));
         }

         if (this.strafeFixConfig.getValue() && this.checkSlowed()) {
         }

         if (this.inventoryMoveConfig.getValue() && this.checkScreen()) {
            long handle = mc.getWindow().getWindow();
            KeyMapping[] keys = new KeyMapping[]{mc.options.keyJump, mc.options.keyUp, mc.options.keyDown, mc.options.keyRight, mc.options.keyLeft};

            for (KeyMapping binding : keys) {
               binding.setDown(InputConstants.isKeyDown(handle, ((AccessorKeyBinding)binding).getBoundKey().getValue()));
            }

            if (this.arrowMoveConfig.getValue()) {
               float yaw = mc.player.getYRot();
               float pitch = mc.player.getXRot();
               if (InputConstants.isKeyDown(handle, 265)) {
                  pitch -= 3.0F;
               } else if (InputConstants.isKeyDown(handle, 264)) {
                  pitch += 3.0F;
               } else if (InputConstants.isKeyDown(handle, 263)) {
                  yaw -= 3.0F;
               } else if (InputConstants.isKeyDown(handle, 262)) {
                  yaw += 3.0F;
               }

               mc.player.setYRot(yaw);
               mc.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
            }
         }

         if ((this.grimConfig.getValue() || this.grimNewConfig.getValue()) && this.websConfig.getValue()) {
            AABB bb = this.grimConfig.getValue() ? mc.player.getBoundingBox().inflate(1.0) : mc.player.getBoundingBox();

            for (BlockPos pos : this.getIntersectingWebs(bb)) {
               Managers.NETWORK
                  .sendPacket(
                     new ServerboundPlayerActionPacket(
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN
                     )
                  );
            }
         }
      }
   }

   @EventListener
   public void onStrafeFix(StrafeFixEvent event) {
      if (this.strafeFixConfig.getValue()) {
         float yaw = Managers.ROTATION.getServerYaw();
         float pitch = Managers.ROTATION.getServerPitch();
         if (Managers.ROTATION.isRotating()) {
            yaw = Managers.ROTATION.getRotationYaw();
            pitch = Managers.ROTATION.getRotationPitch();
         }

         event.cancel();
         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }

   @EventListener
   public void onSlowMovement(SlowMovementEvent event) {
      Block block = event.getState().getBlock();
      if (block instanceof WebBlock && this.websConfig.getValue() || block instanceof SweetBerryBushBlock && this.berryBushConfig.getValue()) {
         float multiplier = this.webSpeedConfig.getValue();
         if (this.webSpeedConfig.getValue() == 1.0F) {
            multiplier = 0.0F;
         }

         event.cancel();
         event.setMultiplier(multiplier);
      }
   }

   @EventListener
   public void onMovementSlowdown(MovementSlowdownEvent event) {
      if (this.sneakConfig.getValue() && mc.player.isShiftKeyDown() || this.crawlConfig.getValue() && mc.player.isVisuallyCrawling()) {
         float f = 1.0F / (float)mc.player.getAttributeValue(Attributes.SNEAKING_SPEED);
         event.input.forwardImpulse *= f;
         event.input.leftImpulse *= f;
      }

      if (this.checkSlowed()) {
         event.input.forwardImpulse *= 5.0F;
         event.input.leftImpulse *= 5.0F;
      }
   }

   @EventListener
   public void onVelocityMultiplier(VelocityMultiplierEvent event) {
      if (event.getBlock() == Blocks.SOUL_SAND && this.soulsandConfig.getValue() || event.getBlock() == Blocks.HONEY_BLOCK && this.honeyblockConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event) {
      if (this.slimeblockConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onBlockSlipperiness(BlockSlipperinessEvent event) {
      if (event.getBlock() == Blocks.SLIME_BLOCK && this.slimeblockConfig.getValue()) {
         event.cancel();
         event.setSlipperiness(0.6F);
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.level != null && !mc.isLocalServer()) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasPosition() && this.strictConfig.getValue() && this.checkSlowed()) {
            Managers.INVENTORY.setSlotForced(mc.player.getInventory().selected);
         } else if (event.getPacket() instanceof ServerboundContainerClickPacket && this.strictConfig.getValue()) {
            if (mc.player.isUsingItem()) {
               mc.player.releaseUsingItem();
            }

            if (this.sneaking || Managers.POSITION.isSneaking()) {
               Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.RELEASE_SHIFT_KEY));
            }

            if (Managers.POSITION.isSprinting()) {
               Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
            }
         }
      }
   }

   private boolean checkGrimNew() {
      return !mc.player.isShiftKeyDown() && !mc.player.isVisuallyCrawling() && !mc.player.isHandsBusy() && mc.player.getUseItemRemainingTicks() < 5
         || mc.player.getTicksUsingItem() > 1 && mc.player.getTicksUsingItem() % 2 != 0;
   }

   public boolean checkSlowed() {
      if (DisablerModule.getInstance().grimFireworkCheck2()) {
         return true;
      } else {
         return this.grimNewConfig.getValue() && !this.checkGrimNew()
            ? false
            : !mc.player.isHandsBusy()
               && !mc.player.isShiftKeyDown()
               && (
                  mc.player.isUsingItem() && this.itemsConfig.getValue()
                     || mc.player.isBlocking() && this.shieldsConfig.getValue() && !this.grimNewConfig.getValue() && !this.grimConfig.getValue()
               );
      }
   }

   public boolean checkScreen() {
      return mc.screen != null && !(mc.screen instanceof ChatScreen) && !(mc.screen instanceof SignEditScreen) && !(mc.screen instanceof DeathScreen);
   }

   public List<BlockPos> getIntersectingWebs(AABB boundingBox) {
      List<BlockPos> blocks = new ArrayList<>();

      for (BlockPos blockPos : PositionUtil.getAllInBox(boundingBox)) {
         BlockState state = mc.level.getBlockState(blockPos);
         if (state.getBlock() instanceof WebBlock) {
            blocks.add(blockPos);
         }
      }

      return blocks;
   }

   public boolean getStrafeFix() {
      return this.strafeFixConfig.getValue();
   }
}
