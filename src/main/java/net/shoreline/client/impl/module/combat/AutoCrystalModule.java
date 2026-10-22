package net.shoreline.client.impl.module.combat;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.RunTickEvent;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.module.CombatModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.exploit.FastLatencyModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.collection.EvictingQueue;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.PerSecondCounter;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.client.util.world.ExplosionUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoCrystalModule extends CombatModule {
   private static AutoCrystalModule INSTANCE;
   Config<Boolean> whileMiningConfig = this.register(new BooleanConfig("WhileMining", "Allows attacking while mining blocks", false));
   Config<Float> targetRangeConfig = this.register(new NumberConfig<>("EnemyRange", "Range to search for potential enemies", 1.0F, 10.0F, 13.0F));
   Config<Boolean> instantConfig = this.register(new BooleanConfig("Instant", "Instantly attacks crystals when they spawn", false));
   Config<AutoCrystalModule.Sequential> sequentialConfig = this.register(
      new EnumConfig<>("Sequential", "Places a crystal after spawn", AutoCrystalModule.Sequential.NONE, AutoCrystalModule.Sequential.values())
   );
   Config<Boolean> idPredictConfig = this.register(new BooleanConfig("BreakPredict", "Attempts to predict crystal entity ids", false));
   Config<Boolean> instantCalcConfig = this.register(
      new BooleanConfig(
         "Instant-Calc",
         "Calculates a crystal when it spawns and attacks if it meets MINIMUM requirements, this will result in non-ideal crystal attacks",
         false,
         () -> false
      )
   );
   Config<Float> instantDamageConfig = this.register(
      new NumberConfig<>("InstantDamage", "Minimum damage to attack crystals instantly", 1.0F, 6.0F, 10.0F, () -> false)
   );
   Config<Boolean> instantMaxConfig = this.register(
      new BooleanConfig(
         "InstantMax",
         "Attacks crystals instantly if they exceed the previous max attack damage (Note: This is still not a perfect check because the next tick could have better damages)",
         true,
         () -> false
      )
   );
   Config<Boolean> raytraceConfig = this.register(new BooleanConfig("Raytrace", "Raytrace to crystal position", true));
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swing hand when placing and attacking crystals", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before placing and breaking", false));
   Config<AutoCrystalModule.Rotate> strictRotateConfig = this.register(
      new EnumConfig<>(
         "YawStep",
         "Rotates yaw over multiple ticks to prevent certain rotation flags in NCP",
         AutoCrystalModule.Rotate.OFF,
         AutoCrystalModule.Rotate.values(),
         () -> this.rotateConfig.getValue()
      )
   );
   Config<Integer> rotateLimitConfig = this.register(
      new NumberConfig<>(
         "YawStep-Limit",
         "Maximum yaw rotation in degrees for one tick",
         1,
         180,
         180,
         NumberDisplay.DEGREES,
         () -> this.rotateConfig.getValue() && this.strictRotateConfig.getValue() != AutoCrystalModule.Rotate.OFF
      )
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false));
   Config<Boolean> neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false));
   Config<Boolean> shulkersConfig = this.register(new BooleanConfig("Shulkers", "Target shulker boxes", false));
   Config<Float> breakSpeedConfig = this.register(new NumberConfig<>("BreakSpeed", "Speed to break crystals", 0.1F, 18.0F, 20.0F));
   Config<Float> attackDelayConfig = this.register(new NumberConfig<>("AttackDelay", "Added delays", 0.0F, 0.0F, 5.0F));
   Config<Integer> attackFactorConfig = this.register(
      new NumberConfig<>("AttackFactor", "Factor of attack delay", 0, 0, 3, () -> this.attackDelayConfig.getValue().floatValue() > 0.0)
   );
   Config<Float> attackLimitConfig = this.register(
      new NumberConfig<>("AttackLimit", "The number of attacks before considering a crystal unbreakable", 0.5F, 1.5F, 20.0F)
   );
   Config<Boolean> breakDelayConfig = this.register(new BooleanConfig("BreakDelay", "Uses attack latency to calculate break delays", false));
   Config<Float> breakTimeoutConfig = this.register(
      new NumberConfig<>(
         "BreakTimeout",
         "Time after waiting for the average break time before considering a crystal attack failed",
         0.0F,
         3.0F,
         10.0F,
         () -> this.breakDelayConfig.getValue()
      )
   );
   Config<Float> minTimeoutConfig = this.register(
      new NumberConfig<>(
         "MinTimeout", "Minimum time before considering a crystal break/place failed", 0.0F, 5.0F, 20.0F, () -> this.breakDelayConfig.getValue()
      )
   );
   Config<Integer> ticksExistedConfig = this.register(new NumberConfig<>("TicksExisted", "Minimum ticks alive to consider crystals for attack", 0, 0, 10));
   Config<Float> breakRangeConfig = this.register(new NumberConfig<>("BreakRange", "Range to break crystals", 0.1F, 4.0F, 6.0F));
   Config<Float> maxYOffsetConfig = this.register(new NumberConfig<>("MaxYOffset", "Maximum crystal y-offset difference", 1.0F, 5.0F, 10.0F));
   Config<Float> breakWallRangeConfig = this.register(new NumberConfig<>("BreakWallRange", "Range to break crystals through walls", 0.1F, 4.0F, 6.0F));
   Config<AutoCrystalModule.Swap> antiWeaknessConfig = this.register(
      new EnumConfig<>("AntiWeakness", "Swap to tools before attacking crystals", AutoCrystalModule.Swap.OFF, AutoCrystalModule.Swap.values())
   );
   Config<Float> swapDelayConfig = this.register(
      new NumberConfig<>("SwapPenalty", "Delay for attacking after swapping items which prevents NCP flags", 0.0F, 0.0F, 10.0F)
   );
   Config<Boolean> inhibitConfig = this.register(new BooleanConfig("Inhibit", "Prevents excessive attacks", true));
   Config<Boolean> placeConfig = this.register(
      new BooleanConfig("Place", "Places crystals to damage enemies. Place settings will only function if this setting is enabled.", true)
   );
   Config<Float> placeSpeedConfig = this.register(
      new NumberConfig<>("PlaceSpeed", "Speed to place crystals", 0.1F, 18.0F, 20.0F, () -> this.placeConfig.getValue())
   );
   Config<Float> placeRangeConfig = this.register(
      new NumberConfig<>("PlaceRange", "Range to place crystals", 0.1F, 4.0F, 6.0F, () -> this.placeConfig.getValue())
   );
   Config<Float> placeWallRangeConfig = this.register(
      new NumberConfig<>("PlaceWallRange", "Range to place crystals through walls", 0.1F, 4.0F, 6.0F, () -> this.placeConfig.getValue())
   );
   Config<Boolean> placeRangeEyeConfig = this.register(
      new BooleanConfig("PlaceRangeEye", "Calculates place ranges starting from the eye position of the player", false, () -> this.placeConfig.getValue())
   );
   Config<Boolean> placeRangeCenterConfig = this.register(
      new BooleanConfig("PlaceRangeCenter", "Calculates place ranges to the center of the block", true, () -> this.placeConfig.getValue())
   );
   Config<AutoCrystalModule.Swap> autoSwapConfig = this.register(
      new EnumConfig<>(
         "Swap",
         "Swaps to an end crystal before placing if the player is not holding one",
         AutoCrystalModule.Swap.OFF,
         AutoCrystalModule.Swap.values(),
         () -> this.placeConfig.getValue()
      )
   );
   Config<Boolean> antiSurroundConfig = this.register(
      new BooleanConfig(
         "AntiSurround",
         "Places on mining blocks that when broken, can be placed on to damage enemies. Instantly destroys items spawned from breaking block and allows faster placing",
         false,
         () -> this.placeConfig.getValue()
      )
   );
   Config<AutoCrystalModule.ForcePlace> forcePlaceConfig = this.register(
      new EnumConfig<>("PreventReplace", "Attempts to replace crystals in surrounds", AutoCrystalModule.ForcePlace.NONE, AutoCrystalModule.ForcePlace.values())
   );
   Config<Boolean> breakValidConfig = this.register(
      new BooleanConfig("Strict", "Only places crystals that can be attacked", false, () -> this.placeConfig.getValue())
   );
   Config<Boolean> strictDirectionConfig = this.register(
      new BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> this.placeConfig.getValue())
   );
   Config<AutoCrystalModule.Placements> placementsConfig = this.register(
      new EnumConfig<>(
         "Placements",
         "Version standard for placing end crystals",
         AutoCrystalModule.Placements.NATIVE,
         AutoCrystalModule.Placements.values(),
         () -> this.placeConfig.getValue()
      )
   );
   Config<Float> minDamageConfig = this.register(
      new NumberConfig<>("MinDamage", "Minimum damage required to consider attacking or placing an end crystal", 1.0F, 4.0F, 10.0F)
   );
   Config<Float> maxLocalDamageConfig = this.register(new NumberConfig<>("MaxLocalDamage", "The maximum player damage", 4.0F, 12.0F, 20.0F));
   Config<Boolean> assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Config<Boolean> armorBreakerConfig = this.register(new BooleanConfig("ArmorBreaker", "Attempts to break enemy armor with crystals", true));
   Config<Float> armorScaleConfig = this.register(
      new NumberConfig<>(
         "ArmorScale",
         "Armor damage scale before attempting to break enemy armor with crystals",
         1.0F,
         5.0F,
         20.0F,
         NumberDisplay.PERCENT,
         () -> this.armorBreakerConfig.getValue()
      )
   );
   Config<Float> lethalMultiplier = this.register(
      new NumberConfig<>("LethalMultiplier", "If we can kill an enemy with this many crystals, disregard damage values", 0.0F, 1.5F, 4.0F)
   );
   Config<Boolean> antiTotemConfig = this.register(
      new BooleanConfig(
         "Lethal-Totem", "Predicts totems and places crystals to instantly double pop and kill the target", false, () -> this.placeConfig.getValue()
      )
   );
   Config<Boolean> lethalDamageConfig = this.register(
      new BooleanConfig("Lethal-DamageTick", "Places lethal crystals only on ticks where they damage entities", false)
   );
   Config<Boolean> safetyConfig = this.register(new BooleanConfig("Safety", "Accounts for total player safety when attacking and placing crystals", true));
   Config<Boolean> safetyOverride = this.register(new BooleanConfig("SafetyOverride", "Overrides the safety checks if the crystal will kill an enemy", false));
   Config<Boolean> blockDestructionConfig = this.register(
      new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false)
   );
   Config<Boolean> selfExtrapolateConfig = this.register(new BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", false));
   Config<Integer> extrapolateTicksConfig = this.register(
      new NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10)
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders the current placement", true));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> false));
   Config<Boolean> disableDeathConfig = this.register(new BooleanConfig("DisableOnDeath", "Disables during disconnect/death", false));
   Config<Boolean> debugConfig = new BooleanConfig("Debug", "Adds extra debug info to arraylist", false);
   Config<Boolean> debugDamageConfig = new BooleanConfig("Debug-Damage", "Renders damage", false, () -> this.renderConfig.getValue());
   private AutoCrystalModule.DamageData<EndCrystal> attackCrystal;
   private AutoCrystalModule.DamageData<BlockPos> placeCrystal;
   private BlockPos renderPos;
   private double renderDamage;
   private BlockPos renderSpawnPos;
   private Vec3 crystalRotation;
   private boolean attackRotate;
   private boolean rotated;
   private float[] silentRotations;
   private float calculatePlaceCrystalTime = 0.0F;
   private static final AABB FULL_CRYSTAL_BB = new AABB(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
   private static final AABB HALF_CRYSTAL_BB = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   private final CacheTimer lastAttackTimer = new CacheTimer();
   private final Timer lastPlaceTimer = new CacheTimer();
   private final Timer lastSwapTimer = new CacheTimer();
   private final Timer autoSwapTimer = new CacheTimer();
   private final Deque<Long> attackLatency = new EvictingQueue<>(20);
   private final Map<Integer, Long> attackPackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final Map<BlockPos, Long> placePackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
   private final PerSecondCounter crystalCounter = new PerSecondCounter();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private long predictId;
   private final Map<Integer, Integer> antiStuckCrystals = new HashMap<>();
   private final List<AutoCrystalModule.AntiStuckData> stuckCrystals = new CopyOnWriteArrayList<>();
   private final ExecutorService executor = Executors.newFixedThreadPool(2);

   public AutoCrystalModule() {
      super("AutoCrystal", "Attacks entities with end crystals", ModuleCategory.COMBAT, 750);
      INSTANCE = this;
      this.register(this.debugConfig);
      this.register(this.debugDamageConfig);
   }

   public static AutoCrystalModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return this.debugConfig.getValue()
         ? String.format(
            "%sms, %.0f, %dms, %d"
               .formatted(
                  new DecimalFormat("0.00").format(this.calculatePlaceCrystalTime / 1000000.0),
                  this.placeCrystal == null ? 0.0 : this.lastAttackTimer.getLastResetTime() / 1000000.0,
                  this.lastAttackTimer.passed((20.0F - this.breakSpeedConfig.getValue()) * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
                  this.crystalCounter.getPerSecond()
               )
         )
         : String.format(
            "%dms, %d",
            this.lastAttackTimer.passed((20.0F - this.breakSpeedConfig.getValue()) * 50.0F + 2000.0F) ? 0 : this.getBreakMs(),
            this.crystalCounter.getPerSecond()
         );
   }

   @Override
   public void onDisable() {
      this.renderPos = null;
      this.attackCrystal = null;
      this.placeCrystal = null;
      this.crystalRotation = null;
      this.silentRotations = null;
      this.calculatePlaceCrystalTime = 0.0F;
      this.stuckCrystals.clear();
      this.attackPackets.clear();
      this.antiStuckCrystals.clear();
      this.placePackets.clear();
      this.attackLatency.clear();
      this.fadeList.clear();
      this.setStage("NONE");
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      if (this.disableDeathConfig.getValue()) {
         this.disable();
      } else {
         this.onDisable();
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!mc.player.isSpectator() && (!this.isSilentSwap(this.autoSwapConfig.getValue()) || !AutoMineModule.getInstance().isSilentSwapping())) {
         for (AutoCrystalModule.AntiStuckData d : this.stuckCrystals) {
            double dist = mc.player.distanceToSqr(d.pos());
            double diff = d.stuckDist() - dist;
            if (diff > 0.5) {
               this.stuckCrystals.remove(d);
            }
         }

         if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
            || mc.options.keyAttack.isDown()
            || PlayerUtil.isHotbarKeysPressed()) {
            this.autoSwapTimer.reset();
         }

         this.renderPos = null;
         ArrayList<Entity> entities = Lists.newArrayList(mc.level.entitiesForRendering());
         List<BlockPos> blocks = this.getSphere(this.placeRangeEyeConfig.getValue() ? mc.player.getEyePosition() : mc.player.position());
         long timePre = System.nanoTime();
         if (this.placeConfig.getValue()) {
            this.placeCrystal = this.calculatePlaceCrystal(blocks, entities);
         }

         this.attackCrystal = this.calculateAttackCrystal(entities);
         if (this.attackCrystal == null) {
            if (this.placeCrystal != null) {
               EndCrystal crystalEntity = this.intersectingCrystalCheck(this.placeCrystal.getDamageData());
               if (crystalEntity != null) {
                  double self = ExplosionUtil.getDamageTo(
                     mc.player,
                     crystalEntity.position(),
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  if (!this.safetyConfig.getValue() || !this.playerDamageCheck(self)) {
                     this.attackCrystal = new AutoCrystalModule.DamageData<>(
                        crystalEntity, this.placeCrystal.getAttackTarget(), this.placeCrystal.getDamage(), self, crystalEntity.blockPosition().below(), false
                     );
                  }
               }
            }

            this.calculatePlaceCrystalTime = (float)(System.nanoTime() - timePre);
         }

         if (this.inhibitConfig.getValue() && this.attackCrystal != null && this.attackPackets.containsKey(this.attackCrystal.getDamageData().getId())) {
            float delay;
            if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
               float attackFactor = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
               delay = this.attackDelayConfig.getValue() * attackFactor;
            } else {
               delay = 1000.0F - this.breakSpeedConfig.getValue() * 50.0F;
            }

            this.lastAttackTimer.setDelay(delay + 100.0F);
            this.attackPackets.remove(this.attackCrystal.getDamageData().getId());
         }

         float breakDelay = this.getBreakDelay();
         if (this.breakDelayConfig.getValue()) {
            breakDelay = Math.max(this.minTimeoutConfig.getValue() * 50.0F, this.getBreakMs() + this.breakTimeoutConfig.getValue() * 50.0F);
         }

         this.attackRotate = this.attackCrystal != null && this.attackDelayConfig.getValue().floatValue() <= 0.0 && this.lastAttackTimer.passed(breakDelay);
         if (this.attackCrystal != null) {
            this.crystalRotation = this.attackCrystal.damageData.position();
         } else if (this.placeCrystal != null) {
            this.crystalRotation = this.placeCrystal.damageData.getCenter().add(0.0, 0.5, 0.0);
         }

         if (this.rotateConfig.getValue() && this.crystalRotation != null && (this.placeCrystal == null || this.canHoldCrystal())) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), this.crystalRotation);
            if (this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.FULL
               || this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.SEMI && this.attackRotate) {
               float serverYaw = Managers.ROTATION.getWrappedYaw();
               float diff = serverYaw - rotations[0];
               float diff1 = Math.abs(diff);
               if (diff1 > 180.0F) {
                  diff += diff > 0.0F ? -360.0F : 360.0F;
               }

               int dir = diff > 0.0F ? -1 : 1;
               float deltaYaw = dir * this.rotateLimitConfig.getValue();
               float yaw;
               if (diff1 > this.rotateLimitConfig.getValue().intValue()) {
                  yaw = serverYaw + deltaYaw;
                  this.rotated = false;
               } else {
                  yaw = rotations[0];
                  this.rotated = true;
                  this.crystalRotation = null;
               }

               rotations[0] = yaw;
            } else {
               this.rotated = true;
               this.crystalRotation = null;
            }

            this.setRotation(rotations[0], rotations[1]);
         } else {
            this.silentRotations = null;
         }

         if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
            InteractionHand hand = this.getCrystalHand();
            if (this.attackCrystal != null && this.attackRotate) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
            }

            boolean placeRotate = this.lastPlaceTimer.passed(1000.0F - this.placeSpeedConfig.getValue() * 50.0F);
            if (this.placeCrystal != null) {
               this.renderPos = this.placeCrystal.getDamageData();
               this.renderDamage = this.placeCrystal.getDamage();
               if (placeRotate) {
                  this.placeCrystal(this.placeCrystal.getDamageData(), hand);
                  this.setStage("PLACING");
                  this.lastPlaceTimer.reset();
               }
            }
         }
      }
   }

   @EventListener
   public void onRunTick(RunTickEvent event) {
      if (mc.player != null) {
         InteractionHand hand = this.getCrystalHand();
         if (this.attackDelayConfig.getValue().floatValue() > 0.0) {
            float attackFactor = 50.0F / Math.max(1.0F, (float)this.attackFactorConfig.getValue().intValue());
            if (this.attackCrystal != null && this.lastAttackTimer.passed(this.attackDelayConfig.getValue() * attackFactor)) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.lastAttackTimer.reset();
            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue()) {
         RenderBuffers.preRender();
         BlockPos renderPos1 = null;
         double factor = 0.0;

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (set.getKey() != this.renderPos) {
               if (set.getValue().getFactor() > factor) {
                  renderPos1 = set.getKey();
                  factor = set.getValue().getFactor();
               }

               set.getValue().setState(false);
               int boxAlpha = (int)(40.0 * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
            }
         }

         if (this.debugDamageConfig.getValue() && renderPos1 != null) {
            RenderManager.renderSign(String.format("%.1f", this.renderDamage), renderPos1.getCenter(), new Color(255, 255, 255, (int)(255.0 * factor)).getRGB());
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
         if (this.renderPos != null && this.isHoldingCrystal()) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.renderPos, animation);
         }
      }
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
               this.handleServerPackets(packet1);
            }
         } else {
            this.handleServerPackets(event.getPacket());
         }
      }
   }

   private void handleServerPackets(Packet<?> serverPacket) {
      if (serverPacket instanceof ClientboundExplodePacket packet) {
         for (Entity entity : Lists.newArrayList(mc.level.entitiesForRendering())) {
            if (entity instanceof EndCrystal && entity.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
               mc.executeIfPossible(() -> mc.level.removeEntity(entity.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(entity.getId());
               Long attackTime = this.attackPackets.remove(entity.getId());
               if (attackTime != null) {
                  this.attackLatency.add(System.currentTimeMillis() - attackTime);
               }
            }
         }
      }

      if (serverPacket instanceof ClientboundSoundPacket packet
         && packet.getSound().value() == SoundEvents.GENERIC_EXPLODE.value()
         && packet.getSource() == SoundSource.BLOCKS) {
         for (Entity entityx : Lists.newArrayList(mc.level.entitiesForRendering())) {
            if (entityx instanceof EndCrystal && entityx.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
               mc.executeIfPossible(() -> mc.level.removeEntity(entityx.getId(), RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(entityx.getId());
               Long attackTime = this.attackPackets.remove(entityx.getId());
               if (attackTime != null) {
                  this.attackLatency.add(System.currentTimeMillis() - attackTime);
               }
            }
         }
      }

      if (serverPacket instanceof ClientboundRemoveEntitiesPacket packet) {
         IntListIterator var11 = packet.getEntityIds().iterator();

         while (var11.hasNext()) {
            int id = (Integer)var11.next();
            this.antiStuckCrystals.remove(id);
            Long attackTime = this.attackPackets.remove(id);
            if (attackTime != null) {
               this.attackLatency.add(System.currentTimeMillis() - attackTime);
            }
         }
      }

      if (serverPacket instanceof ClientboundAddExperienceOrbPacket packet && packet.getId() > this.predictId) {
         this.predictId = packet.getId();
      }

      if (serverPacket instanceof ClientboundAddEntityPacket packet && packet.getId() > this.predictId) {
         this.predictId = packet.getId();
      }
   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      if (event.getEntity() instanceof EndCrystal crystalEntity) {
         Vec3 crystalPos = crystalEntity.position();
         BlockPos blockPos = BlockPos.containing(crystalPos.add(0.0, -1.0, 0.0));
         this.renderSpawnPos = blockPos;
         Long time = this.placePackets.remove(blockPos);
         this.attackRotate = time != null;
         if (this.attackRotate) {
            this.crystalCounter.updateCounter();
         }

         if (this.instantConfig.getValue()) {
            if (this.attackRotate) {
               InteractionHand hand = this.getCrystalHand();
               this.attackInternal(crystalEntity, hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                  this.placeSequentialCrystal(hand);
               }
            } else if (this.instantCalcConfig.getValue()) {
               if (this.attackRangeCheck(crystalPos)) {
                  return;
               }

               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  crystalPos,
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               if (this.playerDamageCheck(selfDamage)) {
                  return;
               }

               for (Entity entity : mc.level.entitiesForRendering()) {
                  if (entity != null && entity.isAlive() && entity != mc.player && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
                     double crystalDist = crystalPos.distanceToSqr(entity.position());
                     if (!(crystalDist > 144.0)) {
                        double dist = mc.player.distanceToSqr(entity);
                        if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                           double damage = ExplosionUtil.getDamageTo(
                              entity,
                              crystalPos,
                              this.blockDestructionConfig.getValue(),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           );
                           AutoCrystalModule.DamageData<EndCrystal> data = new AutoCrystalModule.DamageData<>(
                              crystalEntity, entity, damage, selfDamage, crystalEntity.blockPosition().below(), false
                           );
                           this.attackRotate = damage > this.instantDamageConfig.getValue().floatValue()
                              || this.attackCrystal != null && damage >= this.attackCrystal.getDamage() && this.instantMaxConfig.getValue()
                              || entity instanceof LivingEntity entity1 && this.isCrystalLethalTo(data, entity1);
                           if (this.attackRotate) {
                              InteractionHand hand = this.getCrystalHand();
                              this.attackInternal(crystalEntity, hand);
                              this.setStage("ATTACKING");
                              this.lastAttackTimer.reset();
                              if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                                 this.placeSequentialCrystal(hand);
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof ServerboundSetCarriedItemPacket) {
            this.lastSwapTimer.reset();
         }
      }
   }

   public boolean isAttacking() {
      return this.attackCrystal != null;
   }

   public boolean isPlacing() {
      return this.placeCrystal != null && this.isHoldingCrystal();
   }

   public void attackCrystal(EndCrystal entity, InteractionHand hand) {
      if (!this.attackCheckPre(hand)) {
         MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
         MobEffectInstance strength = mc.player.getEffect(MobEffects.DAMAGE_BOOST);
         if (weakness != null && (strength == null || weakness.getAmplifier() > strength.getAmplifier())) {
            int slot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof PickaxeItem)) {
                  slot = i;
                  break;
               }
            }

            if (slot != -1) {
               boolean canSwap = slot != Managers.INVENTORY.getServerSlot()
                  && (this.antiWeaknessConfig.getValue() != AutoCrystalModule.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (this.antiWeaknessConfig.getValue() != AutoCrystalModule.Swap.OFF && canSwap) {
                  if (this.antiWeaknessConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.gameMode
                        .handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, mc.player.getInventory().selected, ClickType.SWAP, mc.player);
                  } else if (this.antiWeaknessConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.setSlot(slot);
                  } else {
                     Managers.INVENTORY.setClientSlot(slot);
                  }
               }

               this.attackInternal(entity, InteractionHand.MAIN_HAND);
               if (canSwap) {
                  if (this.antiWeaknessConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.gameMode
                        .handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, mc.player.getInventory().selected, ClickType.SWAP, mc.player);
                  } else if (this.antiWeaknessConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.syncToClient();
                  }
               }

               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
                  this.placeSequentialCrystal(hand);
               }
            }
         } else {
            this.attackInternal(entity, hand);
            if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
               this.placeSequentialCrystal(hand);
            }
         }
      }
   }

   private void attackInternal(EndCrystal crystalEntity, InteractionHand hand) {
      this.attackInternal(crystalEntity.getId(), hand);
   }

   private void attackInternal(int crystalEntity, InteractionHand hand) {
      hand = hand != null ? hand : InteractionHand.MAIN_HAND;
      EndCrystal entity2 = new EndCrystal(mc.level, 0.0, 0.0, 0.0);
      entity2.setId(crystalEntity);
      ServerboundInteractPacket packet = ServerboundInteractPacket.createAttackPacket(entity2, mc.player.isShiftKeyDown());
      Managers.NETWORK.sendPacket(packet);
      if (this.swingConfig.getValue()) {
         mc.player.swing(hand);
      } else {
         Managers.NETWORK.sendPacket(new ServerboundSwingPacket(hand));
      }

      this.attackPackets.put(crystalEntity, System.currentTimeMillis());
      Integer antiStuckCount = this.antiStuckCrystals.get(crystalEntity);
      if (antiStuckCount != null) {
         this.antiStuckCrystals.replace(crystalEntity, antiStuckCount + 1);
      } else {
         this.antiStuckCrystals.put(crystalEntity, 1);
      }
   }

   private void placeSequentialCrystal(InteractionHand hand) {
      if (this.placeCrystal != null) {
         int latency = FastLatencyModule.getInstance().isEnabled() ? (int)FastLatencyModule.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
         if (!Managers.NETWORK.is2b2t() || latency >= 50) {
            this.placeCrystal(this.placeCrystal.getBlockPos(), hand);
         }
      }
   }

   private void placeCrystal(BlockPos blockPos, InteractionHand hand) {
      if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue())) {
         this.placeCrystal(blockPos, hand, true);
      }
   }

   public void placeCrystal(BlockPos blockPos, InteractionHand hand, boolean checkPlacement) {
      if (!checkPlacement || !this.checkCanUseCrystal()) {
         Direction sidePlace = this.getPlaceDirection(blockPos);
         BlockHitResult result = new BlockHitResult(blockPos.getCenter(), sidePlace, blockPos, false);
         if (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && hand != InteractionHand.OFF_HAND && this.getCrystalHand() == null) {
            if (this.isSilentSwap(this.autoSwapConfig.getValue()) && InventoryUtil.count(Items.END_CRYSTAL) == 0) {
               return;
            }

            int crystalSlot = this.getCrystalSlot();
            if (crystalSlot != -1) {
               boolean canSwap = crystalSlot != Managers.INVENTORY.getServerSlot()
                  && (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.gameMode
                        .handleInventoryMouseClick(
                           mc.player.inventoryMenu.containerId, crystalSlot + 36, mc.player.getInventory().selected, ClickType.SWAP, mc.player
                        );
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.setSlot(crystalSlot);
                  } else {
                     Managers.INVENTORY.setClientSlot(crystalSlot);
                  }
               }

               this.placeInternal(result, InteractionHand.MAIN_HAND);
               this.placePackets.put(blockPos, System.currentTimeMillis());
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.gameMode
                        .handleInventoryMouseClick(
                           mc.player.inventoryMenu.containerId, crystalSlot + 36, mc.player.getInventory().selected, ClickType.SWAP, mc.player
                        );
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.syncToClient();
                  }
               }
            }
         } else if (this.isHoldingCrystal()) {
            this.placeInternal(result, hand);
            this.placePackets.put(blockPos, System.currentTimeMillis());
         }
      }
   }

   private void placeInternal(BlockHitResult result, InteractionHand hand) {
      if (hand != null) {
         Managers.NETWORK.sendSequencedPacket(idx -> new ServerboundUseItemOnPacket(hand, result, idx));
         if (this.swingConfig.getValue()) {
            mc.player.swing(hand);
         } else {
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(hand));
         }

         if (this.idPredictConfig.getValue()) {
            boolean flag = AutoXPModule.getInstance().isEnabled()
               || mc.player.isUsingItem() && mc.player.getItemInHand(mc.player.getUsedItemHand()).getItem() instanceof ExperienceBottleItem;
            int id = (int)(this.predictId + 1L);
            if (flag || this.attackPackets.containsKey(id)) {
               return;
            }

            Entity entity = mc.level.getEntity(id);
            if (entity != null && !(entity instanceof EndCrystal)) {
               return;
            }

            EndCrystal entity2 = new EndCrystal(mc.level, 0.0, 0.0, 0.0);
            entity2.setId(id);
            ServerboundInteractPacket packet = ServerboundInteractPacket.createAttackPacket(entity2, false);
            Managers.NETWORK.sendPacket(packet);
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            this.attackPackets.put(id, System.currentTimeMillis());
         }
      }
   }

   private boolean isSilentSwap(AutoCrystalModule.Swap swap) {
      return swap == AutoCrystalModule.Swap.SILENT || swap == AutoCrystalModule.Swap.SILENT_ALT;
   }

   private int getCrystalSlot() {
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof EndCrystalItem) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private Direction getPlaceDirection(BlockPos blockPos) {
      int x = blockPos.getX();
      int y = blockPos.getY();
      int z = blockPos.getZ();
      if (this.strictDirectionConfig.getValue()) {
         if (mc.player.getY() >= blockPos.getY()) {
            return Direction.UP;
         }

         BlockHitResult result = mc.level
            .clip(new ClipContext(mc.player.getEyePosition(), new Vec3(x + 0.5, y + 0.5, z + 0.5), Block.OUTLINE, Fluid.NONE, mc.player));
         if (result != null && result.getType() == Type.BLOCK) {
            return result.getDirection();
         }
      } else {
         if (mc.level.isInWorldBounds(blockPos)) {
            return Direction.DOWN;
         }

         BlockHitResult result = mc.level
            .clip(new ClipContext(mc.player.getEyePosition(), new Vec3(x + 0.5, y + 0.5, z + 0.5), Block.OUTLINE, Fluid.NONE, mc.player));
         if (result != null && result.getType() == Type.BLOCK) {
            return result.getDirection();
         }
      }

      return Direction.UP;
   }

   private AutoCrystalModule.DamageData<EndCrystal> calculateAttackCrystal(List<Entity> entities) {
      if (entities.isEmpty()) {
         return null;
      } else {
         List<AutoCrystalModule.DamageData<EndCrystal>> validData = new ArrayList<>();
         AutoCrystalModule.DamageData<EndCrystal> data = null;

         for (Entity crystal : entities) {
            if (crystal instanceof EndCrystal crystal1 && crystal.isAlive() && !this.stuckCrystals.stream().anyMatch(d -> d.id() == crystal.getId())) {
               Long time = this.attackPackets.get(crystal.getId());
               boolean attacked = time != null && time < this.getBreakMs();
               if ((crystal.tickCount >= this.ticksExistedConfig.getValue() && !attacked || !this.inhibitConfig.getValue()) && !this.attackRangeCheck(crystal1)
                  )
                {
                  double selfDamage = ExplosionUtil.getDamageTo(
                     mc.player,
                     crystal.position(),
                     this.blockDestructionConfig.getValue(),
                     this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                     false
                  );
                  boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
                  if (!unsafeToPlayer || this.safetyOverride.getValue()) {
                     for (Entity entity : entities) {
                        if (entity != null
                           && entity.isAlive()
                           && entity != mc.player
                           && this.isValidTarget(entity)
                           && !Managers.SOCIAL.isFriend(entity.getName())) {
                           double crystalDist = crystal.distanceToSqr(entity);
                           if (!(crystalDist > 144.0)) {
                              double dist = mc.player.distanceToSqr(entity);
                              if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                                 boolean antiSurround = false;
                                 if (this.antiSurroundConfig.getValue()
                                    && entity instanceof Player player
                                    && !BlastResistantBlocks.isUnbreakable(player.blockPosition())) {
                                    Set<BlockPos> miningPositions = new HashSet<>();
                                    BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                                    if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                       miningPositions.add(miningBlock);
                                    }

                                    if (Managers.BLOCK.getMines(0.75F).contains(player.blockPosition().above())) {
                                       miningPositions.add(player.blockPosition().above());
                                    }

                                    for (BlockPos miningBlockPos : miningPositions) {
                                       if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                          for (Direction direction : Direction.values()) {
                                             BlockPos pos1 = miningBlockPos.relative(direction);
                                             if (crystal.blockPosition().equals(pos1.below())) {
                                                antiSurround = true;
                                             }
                                          }
                                       }
                                    }
                                 }

                                 double damage = ExplosionUtil.getDamageTo(
                                    entity,
                                    crystal.position(),
                                    this.blockDestructionConfig.getValue(),
                                    this.extrapolateTicksConfig.getValue(),
                                    this.assumeArmorConfig.getValue()
                                 );
                                 if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                                    AutoCrystalModule.DamageData<EndCrystal> currentData = new AutoCrystalModule.DamageData<>(
                                       crystal1, entity, damage, selfDamage, crystal1.blockPosition().below(), antiSurround
                                    );
                                    validData.add(currentData);
                                    if (data == null || damage > data.getDamage()) {
                                       data = currentData;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else {
            return this.antiSurroundConfig.getValue()
               ? validData.stream()
                  .filter(AutoCrystalModule.DamageData::isAntiSurround)
                  .min(Comparator.comparingDouble(d -> mc.player.distanceToSqr(d.getBlockPos().getCenter())))
                  .orElse(null)
               : null;
         }
      }
   }

   private boolean attackRangeCheck(EndCrystal entity) {
      return this.attackRangeCheck(entity.position());
   }

   private boolean attackRangeCheck(Vec3 entityPos) {
      double breakRange = this.breakRangeConfig.getValue().floatValue();
      double breakWallRange = this.breakWallRangeConfig.getValue().floatValue();
      Vec3 playerPos = mc.player.getEyePosition();
      double dist = playerPos.distanceToSqr(entityPos);
      if (dist > breakRange * breakRange) {
         return true;
      } else {
         double yOff = Math.abs(entityPos.y() - mc.player.getY());
         if (yOff > this.maxYOffsetConfig.getValue().floatValue()) {
            return true;
         } else {
            BlockHitResult result = mc.level.clip(new ClipContext(playerPos, entityPos, Block.COLLIDER, Fluid.NONE, mc.player));
            return result.getType() != Type.MISS && dist > breakWallRange * breakWallRange;
         }
      }
   }

   private AutoCrystalModule.DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> placeBlocks, List<Entity> entities) {
      if (!placeBlocks.isEmpty() && !entities.isEmpty()) {
         List<AutoCrystalModule.DamageData<BlockPos>> validData = new ArrayList<>();
         AutoCrystalModule.DamageData<BlockPos> data = null;

         for (BlockPos pos : placeBlocks) {
            if (this.canUseCrystalOnBlock(pos) && !this.placeRangeCheck(pos) && !this.intersectingAntiStuckCheck(pos)) {
               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  this.crystalDamageVec(pos),
                  this.blockDestructionConfig.getValue(),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
               if (!unsafeToPlayer || this.safetyOverride.getValue()) {
                  for (Entity entity : entities) {
                     if (entity != null && entity.isAlive() && entity != mc.player && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())
                        )
                      {
                        double blockDist = pos.distToCenterSqr(entity.position());
                        if (!(blockDist > 144.0)) {
                           double dist = mc.player.distanceToSqr(entity);
                           if (!(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())) {
                              boolean antiSurround = false;
                              if (this.antiSurroundConfig.getValue()
                                 && entity instanceof Player player
                                 && !BlastResistantBlocks.isUnbreakable(player.blockPosition())) {
                                 Set<BlockPos> miningPositions = new HashSet<>();
                                 BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                                 if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                    miningPositions.add(miningBlock);
                                 }

                                 if (Managers.BLOCK.getMines(0.75F).contains(player.blockPosition().above())) {
                                    miningPositions.add(player.blockPosition().above());
                                 }

                                 for (BlockPos miningBlockPos : miningPositions) {
                                    if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                       for (Direction direction : Direction.values()) {
                                          BlockPos pos1 = miningBlockPos.relative(direction);
                                          if (pos.equals(pos1.below())) {
                                             antiSurround = true;
                                          }
                                       }
                                    }
                                 }
                              }

                              double damage = ExplosionUtil.getDamageTo(
                                 entity,
                                 this.crystalDamageVec(pos),
                                 this.blockDestructionConfig.getValue(),
                                 this.extrapolateTicksConfig.getValue(),
                                 this.assumeArmorConfig.getValue()
                              );
                              if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                                 AutoCrystalModule.DamageData<BlockPos> currentData = new AutoCrystalModule.DamageData<>(
                                    pos, entity, damage, selfDamage, antiSurround
                                 );
                                 validData.add(currentData);
                                 if (data == null || damage > data.getDamage()) {
                                    data = currentData;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else {
            return this.antiSurroundConfig.getValue()
               ? validData.stream()
                  .filter(AutoCrystalModule.DamageData::isAntiSurround)
                  .min(Comparator.comparingDouble(d -> mc.player.distanceToSqr(d.getBlockPos().getCenter())))
                  .orElse(null)
               : null;
         }
      } else {
         return null;
      }
   }

   private boolean placeRangeCheck(BlockPos pos) {
      double placeRange = this.placeRangeConfig.getValue().floatValue();
      double placeWallRange = this.placeWallRangeConfig.getValue().floatValue();
      Vec3 player = this.placeRangeEyeConfig.getValue() ? mc.player.getEyePosition() : mc.player.position();
      double dist = this.placeRangeCenterConfig.getValue() ? player.distanceToSqr(pos.getCenter()) : pos.distToLowCornerSqr(player.x, player.y, player.z);
      if (dist > placeRange * placeRange) {
         return true;
      } else {
         Vec3 raytrace = Vec3.atLowerCornerOf(pos).add(0.5, 2.70000004768372, 0.5);
         BlockHitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), raytrace, Block.COLLIDER, Fluid.NONE, mc.player));
         float maxDist = this.breakRangeConfig.getValue() * this.breakRangeConfig.getValue();
         if (result != null && result.getType() == Type.BLOCK && !result.getBlockPos().equals(pos)) {
            maxDist = this.breakWallRangeConfig.getValue() * this.breakWallRangeConfig.getValue();
            if (!this.raytraceConfig.getValue() || dist > placeWallRange * placeWallRange) {
               return true;
            }
         }

         return this.breakValidConfig.getValue() && dist > maxDist;
      }
   }

   public void placeCrystalForTarget(Player target, BlockPos blockPos) {
      if (target != null && !target.isDeadOrDying() && !this.placeRangeCheck(blockPos) && this.canUseCrystalOnBlock(blockPos)) {
         double selfDamage = ExplosionUtil.getDamageTo(
            mc.player,
            this.crystalDamageVec(blockPos),
            this.blockDestructionConfig.getValue(),
            Set.of(blockPos),
            this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
            false
         );
         if (!this.playerDamageCheck(selfDamage)) {
            double damage = ExplosionUtil.getDamageTo(
               target,
               this.crystalDamageVec(blockPos),
               this.blockDestructionConfig.getValue(),
               Set.of(blockPos),
               this.extrapolateTicksConfig.getValue(),
               this.assumeArmorConfig.getValue()
            );
            if ((!(damage < this.minDamageConfig.getValue().floatValue()) || this.isCrystalLethalTo(damage, target))
               && (this.placeCrystal == null || !(this.placeCrystal.getDamage() >= damage))) {
               float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), blockPos.getCenter());
               this.setRotation(rotations[0], rotations[1]);
               this.placeCrystal(blockPos, InteractionHand.MAIN_HAND, false);
               this.fadeList.put(blockPos, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
            }
         }
      }
   }

   private boolean checkOverrideSafety(boolean unsafeToPlayer, double damage, Entity entity) {
      return this.safetyOverride.getValue() && unsafeToPlayer && damage < EntityUtil.getHealth(entity) + 0.5;
   }

   private boolean targetDamageCheck(AutoCrystalModule.DamageData<?> crystal) {
      double minDmg = this.minDamageConfig.getValue().floatValue();
      if (crystal.getAttackTarget() instanceof LivingEntity entity && this.isCrystalLethalTo(crystal, entity)) {
         minDmg = 2.0;
      }

      return crystal.getDamage() < minDmg;
   }

   private boolean playerDamageCheck(double playerDamage) {
      if (!mc.player.isCreative()) {
         float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         return this.safetyConfig.getValue() && playerDamage >= health + 0.5F ? true : playerDamage > this.maxLocalDamageConfig.getValue().floatValue();
      } else {
         return false;
      }
   }

   private boolean isFeetSurrounded(LivingEntity entity) {
      BlockPos pos1 = entity.blockPosition();
      if (!mc.level.getBlockState(pos1).canBeReplaced()) {
         return true;
      } else {
         for (Direction direction : Direction.values()) {
            if (direction.getAxis().isHorizontal()) {
               BlockPos pos2 = pos1.relative(direction);
               if (mc.level.getBlockState(pos2).canBeReplaced()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private boolean checkAntiTotem(double damage, LivingEntity entity) {
      if (entity instanceof Player p) {
         float phealth = EntityUtil.getHealth(p);
         if (phealth <= 2.0F && phealth - damage < 0.5) {
            long time = Managers.TOTEM.getLastPopTime(p);
            if (time != -1L) {
               return System.currentTimeMillis() - time <= 500L;
            }
         }
      }

      return false;
   }

   private boolean isCrystalLethalTo(AutoCrystalModule.DamageData<?> crystal, LivingEntity entity) {
      return this.isCrystalLethalTo(crystal.getDamage(), entity);
   }

   private boolean isCrystalLethalTo(double damage, LivingEntity entity) {
      if (this.lethalDamageConfig.getValue() && this.lastAttackTimer.passed(500)) {
         return true;
      } else if (this.antiTotemConfig.getValue() && this.checkAntiTotem(damage, entity)) {
         return true;
      } else {
         float health = entity.getHealth() + entity.getAbsorptionAmount();
         if (damage * (1.0F + this.lethalMultiplier.getValue()) >= health + 0.5F) {
            return true;
         } else {
            if (this.armorBreakerConfig.getValue()) {
               for (ItemStack armorStack : entity.getArmorSlots()) {
                  int n = armorStack.getDamageValue();
                  int n1 = armorStack.getMaxDamage();
                  float durability = (float)(n1 - n) / n1 * 100.0F;
                  if (durability < this.armorScaleConfig.getValue()) {
                     return true;
                  }
               }
            }

            if (this.shulkersConfig.getValue() && entity instanceof Player) {
               for (BlockPos pos : this.getSphere(3.0, entity.position())) {
                  BlockState state = mc.level.getBlockState(pos);
                  if (state.getBlock() instanceof ShulkerBoxBlock) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean attackCheckPre(InteractionHand hand) {
      if (!this.lastSwapTimer.passed(this.swapDelayConfig.getValue() * 25.0F)) {
         return true;
      } else {
         return hand == InteractionHand.MAIN_HAND ? this.checkCanUseCrystal() : false;
      }
   }

   private boolean checkCanUseCrystal() {
      return !this.multitaskConfig.getValue() && this.checkMultitask() || !this.whileMiningConfig.getValue() && mc.gameMode.isDestroying();
   }

   private boolean isHoldingCrystal() {
      return this.checkCanUseCrystal()
            || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT && this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT_ALT
         ? this.getCrystalHand() != null
         : true;
   }

   private Vec3 crystalDamageVec(BlockPos pos) {
      return Vec3.atLowerCornerOf(pos).add(0.5, 1.0, 0.5);
   }

   private boolean isValidTarget(Entity e) {
      return e instanceof Player && this.playersConfig.getValue()
         || EntityUtil.isMonster(e) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(e) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(e) && this.animalsConfig.getValue();
   }

   public boolean canUseCrystalOnBlock(BlockPos pos) {
      BlockState state = mc.level.getBlockState(pos);
      return !state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK) ? false : this.isCrystalHitboxClear(pos);
   }

   public boolean isCrystalHitboxClear(BlockPos pos) {
      BlockPos p2 = pos.above();
      BlockState state2 = mc.level.getBlockState(p2);
      if (this.placementsConfig.getValue() == AutoCrystalModule.Placements.PROTOCOL && !mc.level.isEmptyBlock(p2.above())) {
         return false;
      } else if (!mc.level.isEmptyBlock(p2) && !state2.is(Blocks.FIRE)) {
         return false;
      } else {
         AABB bb = Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
         double d = p2.getX();
         double e = p2.getY();
         double f = p2.getZ();
         List<Entity> list = this.getEntitiesBlockingCrystal(new AABB(d, e, f, d + bb.maxX, e + bb.maxY, f + bb.maxZ));
         return list.isEmpty();
      }
   }

   private List<Entity> getEntitiesBlockingCrystal(AABB box) {
      List<Entity> entities = new CopyOnWriteArrayList<>(mc.level.getEntities(null, box));

      for (Entity entity : entities) {
         if (entity != null
            && entity.isAlive()
            && !(entity instanceof ExperienceOrb)
            && (this.forcePlaceConfig.getValue() == AutoCrystalModule.ForcePlace.NONE || !(entity instanceof ItemEntity) || entity.tickCount > 10)) {
            if (entity instanceof EndCrystal entity1 && entity1.getBoundingBox().intersects(box)) {
               Integer antiStuckAttacks = this.antiStuckCrystals.get(entity1.getId());
               if (this.attackRangeCheck(entity1) || antiStuckAttacks != null && !(antiStuckAttacks.intValue() <= this.attackLimitConfig.getValue() * 10.0F)) {
                  double dist = mc.player.distanceToSqr(entity1);
                  this.stuckCrystals.add(new AutoCrystalModule.AntiStuckData(entity1.getId(), entity1.blockPosition(), entity1.position(), dist));
               } else {
                  entities.remove(entity);
               }
            }
         } else {
            entities.remove(entity);
         }
      }

      return entities;
   }

   private boolean intersectingAntiStuckCheck(BlockPos blockPos) {
      return this.stuckCrystals.isEmpty() ? false : this.stuckCrystals.stream().anyMatch(d -> d.blockPos().equals(blockPos.above()));
   }

   private EndCrystal intersectingCrystalCheck(BlockPos pos) {
      return (EndCrystal)mc.level
         .getEntities(null, new AABB(pos))
         .stream()
         .filter(e -> e instanceof EndCrystal)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }

   private List<BlockPos> getSphere(Vec3 origin) {
      double rad = Math.ceil(this.placeRangeConfig.getValue().floatValue());
      return this.getSphere(rad, origin);
   }

   private List<BlockPos> getSphere(double rad, Vec3 origin) {
      List<BlockPos> sphere = new ArrayList<>();

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.x() + x), (int)(origin.y() + y), (int)(origin.z() + z));
               BlockPos p = new BlockPos(pos);
               sphere.add(p);
            }
         }
      }

      return sphere;
   }

   private boolean canHoldCrystal() {
      return this.isHoldingCrystal() || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && this.getCrystalSlot() != -1;
   }

   private InteractionHand getCrystalHand() {
      ItemStack offhand = mc.player.getOffhandItem();
      ItemStack mainhand = mc.player.getMainHandItem();
      if (offhand.getItem() instanceof EndCrystalItem) {
         return InteractionHand.OFF_HAND;
      } else {
         return mainhand.getItem() instanceof EndCrystalItem ? InteractionHand.MAIN_HAND : null;
      }
   }

   public float getBreakDelay() {
      return 1000.0F - this.breakSpeedConfig.getValue() * 50.0F;
   }

   public void setStage(String crystalStage) {
   }

   public int getBreakMs() {
      if (this.attackLatency.isEmpty()) {
         return 0;
      } else {
         float avg = 0.0F;
         ArrayList<Long> latencyCopy = Lists.newArrayList(this.attackLatency);
         if (!latencyCopy.isEmpty()) {
            Iterator var3 = latencyCopy.iterator();

            while (var3.hasNext()) {
               float t = (float)((Long)var3.next()).longValue();
               avg += t;
            }

            avg /= latencyCopy.size();
         }

         return (int)avg;
      }
   }

   public boolean shouldPreForcePlace() {
      return this.forcePlaceConfig.getValue() == AutoCrystalModule.ForcePlace.PRE;
   }

   public float getPlaceRange() {
      return this.placeRangeConfig.getValue();
   }

   private record AntiStuckData(int id, BlockPos blockPos, Vec3 pos, double stuckDist) {
   }

   private class AttackCrystalTask implements Callable<AutoCrystalModule.DamageData<EndCrystal>> {
      private final List<Entity> threadSafeEntities;

      public AttackCrystalTask(List<Entity> threadSafeEntities) {
         this.threadSafeEntities = threadSafeEntities;
      }

      public AutoCrystalModule.DamageData<EndCrystal> call() throws Exception {
         return AutoCrystalModule.this.calculateAttackCrystal(this.threadSafeEntities);
      }
   }

   private static class DamageData<T> {
      private final List<String> tags = new ArrayList<>();
      private T damageData;
      private Entity attackTarget;
      private BlockPos blockPos;
      private double damage;
      private double selfDamage;
      private boolean antiSurround;

      public DamageData() {
      }

      public DamageData(BlockPos damageData, Entity attackTarget, double damage, double selfDamage, boolean antiSurround) {
         this.damageData = (T)damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = damageData;
         this.antiSurround = antiSurround;
      }

      public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos, boolean antiSurround) {
         this.damageData = damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = blockPos;
         this.antiSurround = antiSurround;
      }

      public void setDamageData(T damageData, Entity attackTarget, double damage, double selfDamage) {
         this.damageData = damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
      }

      public T getDamageData() {
         return this.damageData;
      }

      public Entity getAttackTarget() {
         return this.attackTarget;
      }

      public double getDamage() {
         return this.damage;
      }

      public double getSelfDamage() {
         return this.selfDamage;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public boolean isAntiSurround() {
         return this.antiSurround;
      }
   }

   public static enum ForcePlace {
      PRE,
      POST,
      NONE;
   }

   private class PlaceCrystalTask implements Callable<AutoCrystalModule.DamageData<BlockPos>> {
      private final List<BlockPos> threadSafeBlocks;
      private final List<Entity> threadSafeEntities;

      public PlaceCrystalTask(List<BlockPos> threadSafeBlocks, List<Entity> threadSafeEntities) {
         this.threadSafeBlocks = threadSafeBlocks;
         this.threadSafeEntities = threadSafeEntities;
      }

      public AutoCrystalModule.DamageData<BlockPos> call() throws Exception {
         return AutoCrystalModule.this.calculatePlaceCrystal(this.threadSafeBlocks, this.threadSafeEntities);
      }
   }

   public static enum Placements {
      NATIVE,
      PROTOCOL;
   }

   public static enum Rotate {
      FULL,
      SEMI,
      OFF;
   }

   public static enum Sequential {
      NORMAL,
      STRICT,
      NONE;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;
   }
}
