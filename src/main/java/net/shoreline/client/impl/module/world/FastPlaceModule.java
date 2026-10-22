package net.shoreline.client.impl.module.world;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.ItemListConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorMinecraftClient;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.world.SneakBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class FastPlaceModule extends ToggleModule {
   Config<FastPlaceModule.Selection> selectionConfig = this.register(
      new EnumConfig<>("Selection", "The selection of items to apply fast placements", FastPlaceModule.Selection.WHITELIST, FastPlaceModule.Selection.values())
   );
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "Fast place click delay", 0, 1, 4));
   Config<Float> startDelayConfig = this.register(new NumberConfig<>("StartDelay", "Fast place start delay", 0.0F, 0.0F, 1.0F));
   Config<Boolean> ghostFixConfig = this.register(new BooleanConfig("GhostFix", "Fixes item ghosting issue on some servers", false));
   Config<List<Item>> whitelistConfig = this.register(
      new ItemListConfig<>("Whitelist", "Valid item whitelist", Items.EXPERIENCE_BOTTLE, Items.SNOWBALL, Items.EGG)
   );
   Config<List<Item>> blacklistConfig = this.register(new ItemListConfig<>("Blacklist", "Valid item blacklist", Items.ENDER_PEARL, Items.ENDER_EYE));
   private final CacheTimer startTimer = new CacheTimer();

   public FastPlaceModule() {
      super("FastPlace", "Place items and blocks faster", ModuleCategory.WORLD);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (!mc.options.keyUse.isDown()) {
            this.startTimer.reset();
         } else if (this.startTimer.passed(this.startDelayConfig.getValue(), TimeUnit.SECONDS)
            && ((AccessorMinecraftClient)mc).hookGetItemUseCooldown() > this.delayConfig.getValue()
            && this.placeCheck(mc.player.getMainHandItem())) {
            if (this.ghostFixConfig.getValue()) {
               Managers.NETWORK
                  .sendSequencedPacket(id -> new ServerboundUseItemPacket(mc.player.getUsedItemHand(), id, mc.player.getYRot(), mc.player.getXRot()));
            }

            ((AccessorMinecraftClient)mc).hookSetItemUseCooldown(this.delayConfig.getValue());
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ServerboundUseItemOnPacket packet
            && this.ghostFixConfig.getValue()
            && !event.isClientPacket()
            && this.placeCheck(mc.player.getItemInHand(packet.getHand()))) {
            BlockState state = mc.level.getBlockState(packet.getHitResult().getBlockPos());
            if (!SneakBlocks.isSneakBlock(state)) {
               event.cancel();
            }
         }
      }
   }

   private boolean placeCheck(ItemStack held) {
      return switch ((FastPlaceModule.Selection)this.selectionConfig.getValue()) {
         case WHITELIST -> ((ItemListConfig)this.whitelistConfig).contains(held.getItem());
         case BLACKLIST -> !((ItemListConfig)this.blacklistConfig).contains(held.getItem());
         case ALL -> true;
      };
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;
   }
}
