package net.shoreline.client.impl.module.combat;

import com.google.common.collect.Lists;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.impl.module.exploit.ChorusInvincibilityModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.world.ExplosionUtil;
import net.shoreline.client.util.world.SneakBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public final class AutoTotemModule extends ToggleModule {
   private static AutoTotemModule INSTANCE;
   Config<AutoTotemModule.OffhandItem> itemConfig = this.register(
      new EnumConfig<>("Item", "The item to wield in your offhand", AutoTotemModule.OffhandItem.TOTEM, AutoTotemModule.OffhandItem.values())
   );
   Config<Float> healthConfig = this.register(new NumberConfig<>("Health", "The health required to fall below before swapping to a totem", 0.0F, 14.0F, 20.0F));
   Config<Boolean> gappleConfig = this.register(new BooleanConfig("OffhandGapple", "Equips a golden apple if holding down the item use button", true));
   Config<Boolean> crappleConfig = this.register(new BooleanConfig("Crapple", "Uses a normal golden apple if Absorption is present", true));
   Config<Boolean> lethalConfig = this.register(
      new BooleanConfig("Lethal", "Calculates lethal damage sources", false, () -> this.itemConfig.getValue() != AutoTotemModule.OffhandItem.TOTEM)
   );
   Config<Boolean> fastConfig = this.register(new BooleanConfig("FastSwap", "Swaps items to offhand", true));
   Config<Boolean> mainhandTotemConfig = this.register(new BooleanConfig("MainhandTotem", "Swaps to a totem in your mainhand", false));
   Config<Integer> totemSlotConfig = this.register(
      new NumberConfig<>("TotemSlot", "Slot to use for mainhand totem", 1, 1, 9, () -> this.mainhandTotemConfig.getValue())
   );
   Config<Boolean> alternativeConfig = this.register(new BooleanConfig("Alternative", "Replaces totem using the swap packet", false));
   Config<Boolean> debugConfig = this.register(new BooleanConfig("Debug", "Debug on death", false));
   private int lastHotbarSlot;
   private int lastTotemCount;
   private Item lastHotbarItem;
   private Item offhandItem;
   private boolean replacing;
   private long replaceTime;
   private final Timer mainhandSwapTimer = new CacheTimer();
   private boolean totemInMainhand;

   public AutoTotemModule() {
      super("AutoTotem", "Automatically replenishes the totem in your offhand", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoTotemModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return String.valueOf(InventoryUtil.count(Items.TOTEM_OF_UNDYING));
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.lastHotbarSlot = -1;
      this.lastHotbarItem = null;
      this.offhandItem = null;
      this.totemInMainhand = false;
   }

   @EventListener
   public void onLoadWorld(LoadWorldEvent event) {
      this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
   }

   @EventListener(
      priority = 2147483646
   )
   public void onTick(TickEvent event) {
      if (mc.player != null && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.mainhandTotemConfig.getValue() && this.mainhandSwapTimer.passed(200)) {
            int totemSlot1 = this.totemSlotConfig.getValue() - 1;
            ItemStack totemSlotStack = mc.player.getInventory().getItem(totemSlot1);
            totemSlot1 += 36;
            if (totemSlotStack.getItem() != Items.TOTEM_OF_UNDYING) {
               for (int n = 35; n >= 0; n--) {
                  if (mc.player.getInventory().getItem(n).getItem() == Items.TOTEM_OF_UNDYING) {
                     int slot = n < 9 ? n + 36 : n;
                     this.replacing = true;
                     if (this.alternativeConfig.getValue()) {
                        mc.gameMode.handleInventoryMouseClick(0, slot, totemSlot1, ClickType.SWAP, mc.player);
                        this.replacing = false;
                     } else {
                        if (mc.player.containerMenu.getCarried().getItem() != Items.TOTEM_OF_UNDYING) {
                           mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        }

                        if (mc.player.containerMenu.getCarried().getItem() == Items.TOTEM_OF_UNDYING) {
                           mc.gameMode.handleInventoryMouseClick(0, totemSlot1, 0, ClickType.PICKUP, mc.player);
                           this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING) - 1;
                        }

                        this.replacing = false;
                        if (!mc.player.containerMenu.getCarried().isEmpty() && mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                           mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                           return;
                        }
                     }
                  }
               }
            }

            this.totemInMainhand = this.checkMainhandTotem();
            if (this.totemInMainhand) {
               int totemSlot = -1;

               for (int i = 0; i < 9; i++) {
                  ItemStack stack = mc.player.getInventory().getItem(i);
                  if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                     totemSlot = i;
                     break;
                  }
               }

               if (totemSlot != -1) {
                  Managers.INVENTORY.setClientSlot(totemSlot);
               }
            }
         } else {
            this.totemInMainhand = false;
         }

         this.offhandItem = this.itemConfig.getValue().getItem();
         if (this.checkLethal()) {
            this.offhandItem = Items.TOTEM_OF_UNDYING;
         } else {
            Item mainHandItem = mc.player.getMainHandItem().getItem();
            if (this.gappleConfig.getValue()
               && mc.options.keyUse.isDown()
               && (mainHandItem instanceof SwordItem || mainHandItem instanceof TridentItem || mainHandItem instanceof AxeItem)
               && PlayerUtil.getLocalPlayerHealth() >= this.healthConfig.getValue()) {
               if (mc.hitResult instanceof BlockHitResult result) {
                  BlockState interactBlock = mc.level.getBlockState(result.getBlockPos());
                  if (!SneakBlocks.isSneakBlock(interactBlock)) {
                     this.offhandItem = this.getGoldenAppleType();
                  }
               } else {
                  this.offhandItem = this.getGoldenAppleType();
               }
            }
         }

         if (mc.player.getOffhandItem().getItem() != this.offhandItem) {
            int nx = 35;
            if (this.lastHotbarSlot != -1 && this.lastHotbarItem != null) {
               ItemStack stack = mc.player.getInventory().getItem(this.lastHotbarSlot);
               if (stack.getItem().equals(this.offhandItem) && this.lastHotbarItem.equals(mc.player.getOffhandItem().getItem())) {
                  int tmp = this.lastHotbarSlot;
                  this.lastHotbarSlot = -1;
                  this.lastHotbarItem = null;
                  nx = tmp;
               }
            }

            for (; nx >= 0; nx--) {
               if (mc.player.getInventory().getItem(nx).getItem() == this.offhandItem) {
                  if (nx < 9) {
                     this.lastHotbarItem = this.offhandItem;
                     this.lastHotbarSlot = nx;
                  }

                  int slot = nx < 9 ? nx + 36 : nx;
                  this.replacing = true;
                  if (this.alternativeConfig.getValue()) {
                     mc.gameMode.handleInventoryMouseClick(0, slot, 40, ClickType.SWAP, mc.player);
                     this.replacing = false;
                  } else {
                     if (mc.player.containerMenu.getCarried().getItem() != this.offhandItem) {
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                     }

                     if (mc.player.containerMenu.getCarried().getItem() == this.offhandItem) {
                        mc.gameMode.handleInventoryMouseClick(0, 45, 0, ClickType.PICKUP, mc.player);
                        this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING) - 1;
                     }

                     this.replacing = false;
                     if (!mc.player.containerMenu.getCarried().isEmpty() && mc.player.getOffhandItem().getItem() == this.offhandItem) {
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundSetHealthPacket packet && packet.getHealth() <= 0.0F && this.debugConfig.getValue()) {
            if (this.lastTotemCount <= 0) {
               return;
            }

            Set<String> failureReasonsSet = this.getFailureReasons();
            if (failureReasonsSet.isEmpty()) {
               long serverLatency = System.currentTimeMillis() - this.replaceTime;
               this.sendModuleError("Failed to replace totem in %sms!", new Object[]{serverLatency});
            } else {
               this.sendModuleError("Failed to replace totem! Possible reasons: %s", new Object[]{String.join(", ", failureReasonsSet)});
            }
         }

         if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet && packet.getSlot() == 45 && this.offhandItem == Items.TOTEM_OF_UNDYING) {
            if (mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING || !packet.getItem().isEmpty()) {
               return;
            }

            this.replaceTime = System.currentTimeMillis();
         }
      }
   }

   private Set<String> getFailureReasons() {
      Set<String> failureReasonsSet = new LinkedHashSet<>();
      if (mc.player.containerMenu.containerId != 0) {
         failureReasonsSet.add("Current screen handler is not the player inventory");
      }

      if (!mc.player.containerMenu.getCarried().isEmpty()) {
         failureReasonsSet.add("Totem was not placed in offhand on time");
      }

      return failureReasonsSet;
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getConfig() == this.totemSlotConfig) {
         this.mainhandSwapTimer.reset();
      }
   }

   private boolean checkLethal() {
      float health = PlayerUtil.getLocalPlayerHealth();
      return health <= this.healthConfig.getValue()
         || this.lethalConfig.getValue() && this.checkLethalCrystal(health)
         || PlayerUtil.computeFallDamage(mc.player.fallDistance, 1.0F) + 0.5F > mc.player.getHealth();
   }

   private boolean checkLethalCrystal(float health) {
      for (Entity e : Lists.newArrayList(mc.level.entitiesForRendering())) {
         if (e != null && e.isAlive() && e instanceof EndCrystal crystal && !(mc.player.distanceToSqr(e) > 144.0)) {
            double potential = ExplosionUtil.getDamageTo(mc.player, crystal.position(), false);
            if (!(health + 0.5 > potential)) {
               return true;
            }
         }
      }

      return false;
   }

   private Item getGoldenAppleType() {
      return !this.crappleConfig.getValue()
            || !InventoryUtil.hasItemInInventory(Items.GOLDEN_APPLE, true)
            || !mc.player.hasEffect(MobEffects.ABSORPTION) && InventoryUtil.hasItemInInventory(Items.ENCHANTED_GOLDEN_APPLE, true)
         ? Items.ENCHANTED_GOLDEN_APPLE
         : Items.GOLDEN_APPLE;
   }

   private boolean checkMainhandTotem() {
      return mc.player.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING && !ChorusInvincibilityModule.getInstance().isUsingChorus()
         ? this.checkLethalCrystal(PlayerUtil.getLocalPlayerHealth())
         : false;
   }

   public boolean isTotemInMainhand() {
      return this.totemInMainhand;
   }

   public boolean isReplacing() {
      return this.replacing;
   }

   private static enum OffhandItem {
      TOTEM(Items.TOTEM_OF_UNDYING),
      GAPPLE(Items.ENCHANTED_GOLDEN_APPLE),
      CRYSTAL(Items.END_CRYSTAL);

      private final Item item;

      private OffhandItem(Item item) {
         this.item = item;
      }

      public Item getItem() {
         return this.item;
      }
   }
}
