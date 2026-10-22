package net.shoreline.client.impl.module.combat;

import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.DisconnectEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.impl.manager.world.tick.TickSync;
import net.shoreline.client.impl.module.CombatModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.EnchantmentUtil;
import net.shoreline.client.util.player.PlayerUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.string.EnumFormatter;
import net.shoreline.eventbus.annotation.EventListener;
import org.apache.commons.lang3.mutable.MutableDouble;

public class AuraModule extends CombatModule {
   private static AuraModule INSTANCE;
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swings the hand after attacking", true));
   Config<AuraModule.TargetMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for targeting entities to attack", AuraModule.TargetMode.SWITCH, AuraModule.TargetMode.values())
   );
   Config<AuraModule.Priority> priorityConfig = this.register(
      new EnumConfig<>("Priority", "The value to prioritize when searching for targets", AuraModule.Priority.HEALTH, AuraModule.Priority.values())
   );
   Config<Float> searchRangeConfig = this.register(new NumberConfig<>("EnemyRange", "Range to search for targets", 1.0F, 5.0F, 10.0F));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to attack entities", 1.0F, 4.5F, 6.0F));
   Config<Float> wallRangeConfig = this.register(new NumberConfig<>("WallRange", "Range to attack entities through walls", 1.0F, 4.5F, 6.0F));
   Config<Boolean> vanillaRangeConfig = this.register(new BooleanConfig("VanillaRange", "Only attack within vanilla range", false));
   Config<Float> fovConfig = this.register(new NumberConfig<>("FOV", "Field of view to attack entities", 1.0F, 180.0F, 180.0F));
   Config<Boolean> attackDelayConfig = this.register(
      new BooleanConfig("AttackDelay", "Delays attacks according to minecraft hit delays for maximum damage per attack", true)
   );
   Config<Float> attackSpeedConfig = this.register(
      new NumberConfig<>(
         "AttackSpeed", "Delay for attacks (Only functions if AttackDelay is off)", 1.0F, 20.0F, 20.0F, () -> !this.attackDelayConfig.getValue()
      )
   );
   Config<Float> randomSpeedConfig = this.register(
      new NumberConfig<>(
         "RandomSpeed", "Randomized delay for attacks (Only functions if AttackDelay is off)", 0.0F, 0.0F, 10.0F, () -> !this.attackDelayConfig.getValue()
      )
   );
   Config<Float> swapDelayConfig = this.register(
      new NumberConfig<>("SwapPenalty", "Delay for attacking after swapping items which prevents NCP flags", 0.0F, 0.0F, 10.0F)
   );
   Config<TickSync> tpsSyncConfig = this.register(new EnumConfig<>("TPS-Sync", "Syncs the attacks with the server TPS", TickSync.NONE, TickSync.values()));
   Config<AuraModule.Swap> autoSwapConfig = this.register(
      new EnumConfig<>("AutoSwap", "Automatically swaps to a weapon before attacking", AuraModule.Swap.OFF, AuraModule.Swap.values())
   );
   Config<Boolean> swordCheckConfig = this.register(new BooleanConfig("Sword-Check", "Checks if a weapon is in the hand before attacking", true));
   Config<AuraModule.Vector> hitVectorConfig = this.register(
      new EnumConfig<>("HitVector", "The vector to aim for when attacking entities", AuraModule.Vector.FEET, AuraModule.Vector.values())
   );
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before attacking", false));
   Config<Boolean> silentRotateConfig = this.register(
      new BooleanConfig("RotateSilent", "Rotates silently to server", false, () -> this.rotateConfig.getValue())
   );
   Config<Boolean> strictRotateConfig = this.register(
      new BooleanConfig("YawStep", "Rotates yaw over multiple ticks to prevent certain rotation flags in NCP", false, () -> this.rotateConfig.getValue())
   );
   Config<Integer> rotateLimitConfig = this.register(
      new NumberConfig<>(
         "YawStep-Limit",
         "Maximum yaw rotation in degrees for one tick",
         1,
         180,
         180,
         NumberDisplay.DEGREES,
         () -> this.rotateConfig.getValue() && this.strictRotateConfig.getValue()
      )
   );
   Config<Integer> ticksExistedConfig = this.register(
      new NumberConfig<>("TicksExisted", "The minimum age of the entity to be considered for attack", 0, 0, 200)
   );
   Config<Boolean> armorCheckConfig = this.register(new BooleanConfig("ArmorCheck", "Checks if target has armor before attacking", false));
   Config<Boolean> stopSprintConfig = this.register(new BooleanConfig("StopSprint", "Stops sprinting before attacking to maintain vanilla behavior", false));
   Config<Boolean> stopShieldConfig = this.register(new BooleanConfig("StopShield", "Automatically handles shielding before attacking", false));
   Config<Boolean> maceBreachConfig = this.register(
      new BooleanConfig(
         "MaceBreach", "Abuses vanilla exploit to apply breach enchantment to swords", false, () -> this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT
      )
   );
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false));
   Config<Boolean> neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false));
   Config<Boolean> invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Target invisible entities", true));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders an indicator over the target", true));
   Config<Boolean> disableDeathConfig = this.register(new BooleanConfig("DisableOnDeath", "Disables during disconnect/death", false));
   private Entity entityTarget;
   private long randomDelay = -1L;
   private boolean shielding;
   private boolean sneaking;
   private boolean sprinting;
   private long lastAttackTime;
   private final Timer critTimer = new CacheTimer();
   private final Timer autoSwapTimer = new CacheTimer();
   private final Timer switchTimer = new CacheTimer();
   private boolean rotated;
   private float[] silentRotations;

   public AuraModule() {
      super("Aura", "Attacks nearby entities", ModuleCategory.COMBAT, 700);
      INSTANCE = this;
   }

   public static AuraModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onDisable() {
      this.entityTarget = null;
      this.silentRotations = null;
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      if (this.disableDeathConfig.getValue()) {
         this.disable();
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (this.disableDeathConfig.getValue() && event.getEntity() == mc.player) {
         this.disable();
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!AutoCrystalModule.getInstance().isAttacking()
         && !AutoCrystalModule.getInstance().isPlacing()
         && (this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT || !AutoMineModule.getInstance().isSilentSwapping())
         && !mc.player.isSpectator()) {
         if (this.multitaskConfig.getValue() || !this.checkMultitask(true)) {
            Vec3 eyepos = Managers.POSITION.getEyePos();

            this.entityTarget = switch ((AuraModule.TargetMode)this.modeConfig.getValue()) {
               case SWITCH -> this.getAttackTarget(eyepos);
               case SINGLE -> this.entityTarget != null && this.entityTarget.isAlive() && this.isInAttackRange(eyepos, this.entityTarget)
                  ? this.entityTarget
                  : this.getAttackTarget(eyepos);
            };
            if (this.entityTarget != null && this.switchTimer.passed(this.swapDelayConfig.getValue() * 25.0F)) {
               if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
                  || mc.options.keyAttack.isDown()
                  || PlayerUtil.isHotbarKeysPressed()) {
                  this.autoSwapTimer.reset();
               }

               int slot = this.getSwordSlot();
               boolean silentSwapped = false;
               if (!(mc.player.getMainHandItem().getItem() instanceof SwordItem) && slot != -1) {
                  switch ((AuraModule.Swap)this.autoSwapConfig.getValue()) {
                     case NORMAL:
                        if (this.autoSwapTimer.passed(500)) {
                           Managers.INVENTORY.setClientSlot(slot);
                        }
                        break;
                     case SILENT:
                        Managers.INVENTORY.setSlot(slot);
                        silentSwapped = true;
                  }
               }

               if (this.isHoldingSword() || this.autoSwapConfig.getValue() == AuraModule.Swap.SILENT) {
                  if (this.rotateConfig.getValue()) {
                     float[] rotation = RotationUtil.getRotationsTo(mc.player.getEyePosition(), this.getAttackRotateVec(this.entityTarget));
                     if (!this.silentRotateConfig.getValue() && this.strictRotateConfig.getValue()) {
                        float serverYaw = Managers.ROTATION.getWrappedYaw();
                        float diff = serverYaw - rotation[0];
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
                           yaw = rotation[0];
                           this.rotated = true;
                        }

                        rotation[0] = yaw;
                     } else {
                        this.rotated = true;
                     }

                     if (this.silentRotateConfig.getValue()) {
                        this.silentRotations = rotation;
                     } else {
                        this.setRotation(rotation[0], rotation[1]);
                     }
                  }

                  if (!this.isRotationBlocked() && (this.rotated || !this.rotateConfig.getValue()) && this.isInAttackRange(eyepos, this.entityTarget)) {
                     if (this.attackDelayConfig.getValue()) {
                        Inventory inventory = mc.player.getInventory();
                        ItemStack itemStack = inventory.getItem(slot != -1 && this.swordCheckConfig.getValue() ? slot : mc.player.getInventory().selected);
                        MutableDouble attackSpeed = new MutableDouble(mc.player.getAttributeBaseValue(Attributes.ATTACK_SPEED));
                        ItemAttributeModifiers attributeModifiers = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                        if (attributeModifiers != null) {
                           attributeModifiers.forEach(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                              if (entry == Attributes.ATTACK_SPEED) {
                                 attackSpeed.add(modifier.amount());
                              }
                           });
                        }

                        double attackCooldownTicks = 1.0 / attackSpeed.getValue() * 20.0;
                        int breachSlot = this.getBreachMaceSlot();
                        if (this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT && this.maceBreachConfig.getValue() && breachSlot != -1) {
                           Managers.INVENTORY.setSlot(breachSlot);
                        }

                        float ticks = 20.0F - Managers.TICK.getTickSync(this.tpsSyncConfig.getValue());
                        float currentTime = (float)(System.currentTimeMillis() - this.lastAttackTime) + ticks * 50.0F;
                        if (currentTime / 50.0F >= attackCooldownTicks && this.attackTarget(this.entityTarget)) {
                           this.lastAttackTime = System.currentTimeMillis();
                        }

                        if (this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT && this.maceBreachConfig.getValue() && breachSlot != -1) {
                           Managers.INVENTORY.syncToClient();
                        }
                     } else {
                        if (this.randomDelay < 0L) {
                           this.randomDelay = (long)RANDOM.nextFloat(this.randomSpeedConfig.getValue() * 10.0F + 1.0F);
                        }

                        float delay = this.attackSpeedConfig.getValue() * 50.0F + (float)this.randomDelay;
                        int breachSlotx = this.getBreachMaceSlot();
                        if (this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT && this.maceBreachConfig.getValue() && breachSlotx != -1) {
                           Managers.INVENTORY.setSlot(breachSlotx);
                        }

                        long currentTimex = System.currentTimeMillis() - this.lastAttackTime;
                        if ((float)currentTimex >= 1000.0F - delay && this.attackTarget(this.entityTarget)) {
                           this.randomDelay = -1L;
                           this.lastAttackTime = System.currentTimeMillis();
                        }

                        if (this.autoSwapConfig.getValue() != AuraModule.Swap.SILENT && this.maceBreachConfig.getValue() && breachSlotx != -1) {
                           Managers.INVENTORY.syncToClient();
                        }
                     }

                     if (this.autoSwapConfig.getValue() == AuraModule.Swap.SILENT && silentSwapped) {
                        Managers.INVENTORY.syncToClient();
                     }
                  } else {
                     Managers.INVENTORY.syncToClient();
                  }
               }
            } else {
               this.silentRotations = null;
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof ServerboundSetCarriedItemPacket) {
            this.switchTimer.reset();
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!AutoCrystalModule.getInstance().isAttacking() && !AutoCrystalModule.getInstance().isPlacing() && !mc.player.isSpectator()) {
         if (this.entityTarget != null && this.renderConfig.getValue() && (this.isHoldingSword() || this.autoSwapConfig.getValue() == AuraModule.Swap.SILENT)) {
            long currentTime = System.currentTimeMillis() - this.lastAttackTime;
            float animFactor = 1.0F - Mth.clamp((float)currentTime / 1000.0F, 0.0F, 1.0F);
            int attackDelay = (int)(70.0 * animFactor);
            RenderBuffers.preRender();
            RenderManager.renderBox(
               event.getMatrices(), Interpolation.getInterpolatedEntityBox(this.entityTarget), ColorsModule.getInstance().getRGB(30 + attackDelay)
            );
            RenderManager.renderBoundingBox(
               event.getMatrices(), Interpolation.getInterpolatedEntityBox(this.entityTarget), 1.5F, ColorsModule.getInstance().getRGB(100)
            );
            RenderBuffers.postRender();
         }
      }
   }

   private boolean attackTarget(Entity entity) {
      this.preAttackTarget();
      if (this.silentRotateConfig.getValue() && this.silentRotations != null) {
         this.setRotationSilent(this.silentRotations[0], this.silentRotations[1]);
      }

      ServerboundInteractPacket packet = ServerboundInteractPacket.createAttackPacket(entity, mc.player.isShiftKeyDown());
      Managers.NETWORK.sendPacket(packet);
      if (this.swingConfig.getValue()) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      } else {
         Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }

      this.postAttackTarget(entity);
      if (this.silentRotateConfig.getValue()) {
         Managers.ROTATION.setRotationSilentSync();
      }

      return true;
   }

   private int getSwordSlot() {
      float sharp = 0.0F;
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof SwordItem swordItem) {
            float sharpness = EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
            float dmg = swordItem.getTier().getAttackDamageBonus() + sharpness;
            if (dmg > sharp) {
               sharp = dmg;
               slot = i;
            }
         } else if (stack.getItem() instanceof AxeItem axeItem) {
            float sharpness = EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
            float dmg = axeItem.getTier().getAttackDamageBonus() + sharpness;
            if (dmg > sharp) {
               sharp = dmg;
               slot = i;
            }
         } else if (stack.getItem() instanceof TridentItem) {
            float sharpness = EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
            float dmg = 8.0F + sharpness;
            if (dmg > sharp) {
               sharp = dmg;
               slot = i;
            }
         } else if (stack.getItem() instanceof MaceItem) {
            float sharpness = EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
            float dmg = 5.0F + sharpness;
            if (dmg > sharp) {
               sharp = dmg;
               slot = i;
            }
         }
      }

      return slot;
   }

   private int getBreachMaceSlot() {
      int slot = -1;
      int maxBreach = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof MaceItem) {
            int breach = EnchantmentUtil.getLevel(stack, Enchantments.BREACH);
            if (breach > maxBreach) {
               slot = i;
               maxBreach = breach;
            }
         }
      }

      return slot;
   }

   private void preAttackTarget() {
      ItemStack offhand = mc.player.getOffhandItem();
      this.shielding = false;
      if (this.stopShieldConfig.getValue()) {
         this.shielding = offhand.getItem() == Items.SHIELD && mc.player.isBlocking();
         if (this.shielding) {
            Managers.NETWORK
               .sendPacket(
                  new ServerboundPlayerActionPacket(
                     Action.RELEASE_USE_ITEM, Managers.POSITION.getBlockPos(), Direction.getNearest(mc.player.getX(), mc.player.getY(), mc.player.getZ())
                  )
               );
         }
      }

      this.sneaking = false;
      this.sprinting = false;
      if (this.stopSprintConfig.getValue()) {
         this.sneaking = Managers.POSITION.isSneaking();
         if (this.sneaking) {
            Managers.NETWORK
               .sendPacket(
                  new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY)
               );
         }

         this.sprinting = Managers.POSITION.isSprinting();
         if (this.sprinting) {
            Managers.NETWORK
               .sendPacket(
                  new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING)
               );
         }
      }
   }

   private void postAttackTarget(Entity entity) {
      if (this.shielding) {
         Managers.NETWORK.sendSequencedPacket(s -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, s, mc.player.getYRot(), mc.player.getXRot()));
      }

      if (this.sneaking) {
         Managers.NETWORK
            .sendPacket(
               new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY)
            );
      }

      if (this.sprinting) {
         Managers.NETWORK
            .sendPacket(
               new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING)
            );
      }
   }

   private Entity getAttackTarget(Vec3 pos) {
      double min = Double.MAX_VALUE;
      Entity attackTarget = null;

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != null
            && entity != mc.player
            && entity.isAlive()
            && this.isEnemy(entity)
            && !Managers.SOCIAL.isFriend(entity.getName())
            && !(entity instanceof EndCrystal)
            && !(entity instanceof ItemEntity)
            && !(entity instanceof Arrow)
            && !(entity instanceof ThrownExperienceBottle)
            && (!(this.armorCheckConfig.getValue() && entity instanceof LivingEntity livingEntity) || livingEntity.getArmorSlots().iterator().hasNext())) {
            double dist = pos.distanceTo(entity.position());
            if (dist <= this.searchRangeConfig.getValue().floatValue() && entity.tickCount >= this.ticksExistedConfig.getValue()) {
               switch ((AuraModule.Priority)this.priorityConfig.getValue()) {
                  case HEALTH:
                     if (entity instanceof LivingEntity ex) {
                        float health = ex.getHealth() + ex.getAbsorptionAmount();
                        if (health < min) {
                           min = health;
                           attackTarget = entity;
                        }
                     }
                     break;
                  case DISTANCE:
                     if (dist < min) {
                        min = dist;
                        attackTarget = entity;
                     }
                     break;
                  case ARMOR:
                     if (entity instanceof LivingEntity e) {
                        float armor = this.getArmorDurability(e);
                        if (armor < min) {
                           min = armor;
                           attackTarget = entity;
                        }
                     }
               }
            }
         }
      }

      return attackTarget;
   }

   private float getArmorDurability(LivingEntity e) {
      float edmg = 0.0F;
      float emax = 0.0F;

      for (ItemStack armor : e.getArmorSlots()) {
         if (armor != null && !armor.isEmpty()) {
            edmg += armor.getDamageValue();
            emax += armor.getMaxDamage();
         }
      }

      return 100.0F - edmg / emax;
   }

   public boolean isInAttackRange(Vec3 pos, Entity entity) {
      Vec3 entityPos = this.getAttackRotateVec(entity);
      double dist = pos.distanceTo(entityPos);
      return this.isInAttackRange(dist, pos, entityPos);
   }

   public boolean isInAttackRange(double dist, Vec3 pos, Vec3 entityPos) {
      if (this.vanillaRangeConfig.getValue() && dist > 3.0) {
         return false;
      } else if (dist > this.rangeConfig.getValue().floatValue()) {
         return false;
      } else {
         BlockHitResult result = mc.level.clip(new ClipContext(pos, entityPos, Block.COLLIDER, Fluid.NONE, mc.player));
         if (result != null && !result.getBlockPos().equals(BlockPos.containing(entityPos)) && dist > this.wallRangeConfig.getValue().floatValue()) {
            return false;
         } else if (this.fovConfig.getValue() != 180.0F) {
            float[] rots = RotationUtil.getRotationsTo(pos, entityPos);
            float diff = Mth.wrapDegrees(mc.player.getYRot()) - rots[0];
            float magnitude = Math.abs(diff);
            return magnitude <= this.fovConfig.getValue();
         } else {
            return true;
         }
      }
   }

   public boolean isHoldingSword() {
      return !this.swordCheckConfig.getValue()
         || mc.player.getMainHandItem().getItem() instanceof SwordItem
         || mc.player.getMainHandItem().getItem() instanceof AxeItem
         || mc.player.getMainHandItem().getItem() instanceof TridentItem
         || mc.player.getMainHandItem().getItem() instanceof MaceItem;
   }

   private Vec3 getAttackRotateVec(Entity entity) {
      Vec3 feetPos = entity.position();

      return switch ((AuraModule.Vector)this.hitVectorConfig.getValue()) {
         case EYES -> entity.getEyePosition();
         case TORSO -> feetPos.add(0.0, entity.getBbHeight() / 2.0F, 0.0);
         case FEET -> feetPos;
         case AUTO -> {
            Vec3 torsoPos = feetPos.add(0.0, entity.getBbHeight() / 2.0F, 0.0);
            Vec3 eyesPos = entity.getEyePosition();
            yield Stream.of(feetPos, torsoPos, eyesPos).min(Comparator.comparing(b -> mc.player.getEyePosition().distanceToSqr(b))).orElse(eyesPos);
         }
      };
   }

   private boolean isEnemy(Entity e) {
      return (!e.isInvisible() || this.invisiblesConfig.getValue()) && e instanceof Player && this.playersConfig.getValue()
         || EntityUtil.isMonster(e) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(e) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(e) && this.animalsConfig.getValue();
   }

   public Entity getEntityTarget() {
      return this.entityTarget;
   }

   public static enum Priority {
      HEALTH,
      DISTANCE,
      ARMOR;
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      OFF;
   }

   public static enum TargetMode {
      SWITCH,
      SINGLE;
   }

   public static enum Vector {
      EYES,
      TORSO,
      FEET,
      AUTO;
   }
}
