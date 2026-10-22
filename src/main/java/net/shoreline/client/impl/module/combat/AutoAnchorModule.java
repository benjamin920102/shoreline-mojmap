package net.shoreline.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.BlockPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.player.InventoryUtil;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.ExplosionUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AutoAnchorModule extends BlockPlacerModule {
   private static AutoAnchorModule INSTANCE;
   Config<Float> targetRangeConfig = this.register(new NumberConfig<>("EnemyRange", "Range to search for potential enemies", 1.0F, 10.0F, 13.0F));
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swing hand when exploding anchors", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before exploding", false));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false));
   Config<Boolean> neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Range to explode anchors", 0.1F, 4.0F, 6.0F));
   Config<Float> explodeSpeedConfig = this.register(new NumberConfig<>("ExplodeSpeed", "Speed to explode anchors", 0.1F, 18.0F, 20.0F));
   Config<Boolean> placeConfig = this.register(new BooleanConfig("Place", "Places anchors to damage enemies", true));
   Config<Float> placeSpeedConfig = this.register(
      new NumberConfig<>("PlaceSpeed", "Speed to place anchors", 0.1F, 18.0F, 20.0F, () -> this.placeConfig.getValue())
   );
   Config<Boolean> strictDirectionConfig = this.register(
      new BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> this.placeConfig.getValue())
   );
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Places using grim instant rotations", false, () -> this.placeConfig.getValue()));
   Config<Boolean> assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Config<Float> minDamageConfig = this.register(new NumberConfig<>("MinDamage", "Minimum damage required to consider exploding anchors", 1.0F, 4.0F, 10.0F));
   Config<Boolean> safetyConfig = this.register(new BooleanConfig("Safety", "Accounts for total player safety when exploding anchors", true));
   Config<Float> maxLocalDamageConfig = this.register(new NumberConfig<>("MaxLocalDamage", "The maximum player damage", 4.0F, 12.0F, 20.0F));
   Config<Boolean> blockDestructionConfig = this.register(
      new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false)
   );
   Config<Boolean> selfExtrapolateConfig = this.register(new BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", false));
   Config<Integer> extrapolateTicksConfig = this.register(
      new NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10)
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where anchors will be placed", true));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> false));
   private AutoAnchorModule.AnchorCalc anchorCalc;
   private final Timer explodeTimer = new CacheTimer();
   private final Timer placeTimer = new CacheTimer();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();

   public AutoAnchorModule() {
      super("AutoAnchor", "Automatically places and explodes respawn anchors", ModuleCategory.COMBAT, 740);
      INSTANCE = this;
   }

   public static AutoAnchorModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.anchorCalc = null;
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (InventoryUtil.hasItemInHotbar(Items.RESPAWN_ANCHOR) && InventoryUtil.hasItemInHotbar(Items.GLOWSTONE)) {
            if ((this.multitaskConfig.getValue() || !this.checkMultitask()) && (!this.stopMotionConfig.getValue() || mc.player.onGround())) {
               this.anchorCalc = this.calculateAnchorExplosion();
               if (this.anchorCalc != null) {
                  BlockPos anchorPos = this.anchorCalc.pos();
                  if (this.anchorCalc.isAnchor()) {
                     if (this.rotateConfig.getValue()) {
                        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), anchorPos.getCenter());
                        this.setRotation(rotations[0], rotations[1]);
                     }

                     if (this.explodeTimer.passed(1000.0F - this.explodeSpeedConfig.getValue() * 50.0F)) {
                        this.setAnchor(anchorPos);
                        this.explodeTimer.reset();
                     }
                  } else {
                     if (mc.level.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        return;
                     }

                     int slot = this.getBlockItemSlot(Blocks.RESPAWN_ANCHOR);
                     if (slot == -1) {
                        return;
                     }

                     if (this.placeTimer.passed(1000.0F - this.placeSpeedConfig.getValue() * 50.0F)) {
                        Managers.INVENTORY.setSlot(slot);
                        Vec3 prevMotion = mc.player.getDeltaMovement();
                        if (this.stopMotionConfig.getValue()) {
                           mc.player.setDeltaMovement(Vec3.ZERO);
                        }

                        Managers.INTERACT
                           .placeBlock(anchorPos, slot, this.grimConfig.getValue(), this.strictDirectionConfig.getValue(), false, (state, angles) -> {
                              if (this.rotateConfig.getValue()) {
                                 if (state) {
                                    Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                                 } else if (this.grimConfig.getValue()) {
                                    Managers.ROTATION.setRotationSilentSync();
                                 }
                              }
                           });
                        if (this.stopMotionConfig.getValue()) {
                           mc.player.setDeltaMovement(prevMotion);
                        }

                        Managers.INVENTORY.syncToClient();
                        this.placeTimer.reset();
                     }
                  }
               }
            } else {
               this.anchorCalc = null;
            }
         } else {
            this.anchorCalc = null;
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (this.anchorCalc == null || set.getKey() != this.anchorCalc.pos()) {
               set.getValue().setState(false);
               int boxAlpha = (int)(40.0 * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
            }
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
         if (this.anchorCalc != null) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.anchorCalc.pos(), animation);
         }
      }
   }

   private void setAnchor(BlockPos pos) {
      BlockState state = mc.level.getBlockState(pos);
      if (state.getBlock() instanceof RespawnAnchorBlock) {
         int slot1 = this.findNonBlockSlot();
         if (slot1 != -1) {
            int charges = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charges <= 0) {
               int slot = this.getBlockItemSlot(Blocks.GLOWSTONE);
               if (slot == -1) {
                  return;
               }

               Managers.INVENTORY.setSlot(slot);
               BlockHitResult result = new BlockHitResult(
                  pos.getCenter(), Managers.INTERACT.getInteractDirection(pos, this.strictDirectionConfig.getValue()), pos, true
               );
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result);
               Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
               Managers.INVENTORY.setSlot(slot1);
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result);
               if (this.swingConfig.getValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            } else {
               Managers.INVENTORY.setSlot(slot1);
               BlockHitResult result1 = new BlockHitResult(
                  pos.getCenter(), Managers.INTERACT.getInteractDirection(pos, this.strictDirectionConfig.getValue()), pos, true
               );
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result1);
               if (this.swingConfig.getValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            }
         }
      }
   }

   private AutoAnchorModule.AnchorCalc calculateAnchorExplosion() {
      BlockPos data = null;
      double bestAnchorDamage = 0.0;
      boolean isAnchor = false;

      for (BlockPos pos : this.getSphere(mc.player.position())) {
         BlockState state = mc.level.getBlockState(pos);
         double dist1 = mc.player.getEyePosition().distanceToSqr(pos.getCenter());
         if (!(dist1 > ((NumberConfig)this.rangeConfig).getValueSq())) {
            boolean explosion = state.getBlock() instanceof RespawnAnchorBlock;
            if (state.canBeReplaced() || explosion) {
               double selfDamage = ExplosionUtil.getDamageTo(
                  mc.player,
                  pos.getCenter(),
                  this.blockDestructionConfig.getValue(),
                  10.0F,
                  Set.of(pos),
                  this.selfExtrapolateConfig.getValue() ? this.extrapolateTicksConfig.getValue() : 0,
                  false
               );
               boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
               if (!unsafeToPlayer
                  && (AirPlaceModule.getInstance().isEnabled() || Managers.INTERACT.getInteractDirectionInternal(pos, false) != null)
                  && Managers.INTERACT.canPlace(pos, Blocks.RESPAWN_ANCHOR)) {
                  for (Entity entity : mc.level.entitiesForRendering()) {
                     if (!entity.getBoundingBox().intersects(new AABB(pos))
                        && entity != null
                        && entity.isAlive()
                        && entity != mc.player
                        && this.isValidTarget(entity)
                        && !Managers.SOCIAL.isFriend(entity.getName())) {
                        double blockDist = pos.distToCenterSqr(entity.position());
                        double dist = mc.player.distanceToSqr(entity);
                        if (!(blockDist > 144.0)
                           && !(dist > this.targetRangeConfig.getValue() * this.targetRangeConfig.getValue())
                           && ExplosionUtil.getDamageTo(
                                 entity,
                                 pos.getCenter(),
                                 this.blockDestructionConfig.getValue(),
                                 10.0F,
                                 Set.of(pos),
                                 this.extrapolateTicksConfig.getValue(),
                                 this.assumeArmorConfig.getValue()
                              )
                              > bestAnchorDamage) {
                           double damage = ExplosionUtil.getDamageTo(
                              entity,
                              pos.getCenter(),
                              this.blockDestructionConfig.getValue(),
                              10.0F,
                              Set.of(pos),
                              this.extrapolateTicksConfig.getValue(),
                              this.assumeArmorConfig.getValue()
                           );
                           data = pos;
                           bestAnchorDamage = damage;
                           isAnchor = explosion;
                        }
                     }
                  }
               }
            }
         }
      }

      return data != null && bestAnchorDamage >= this.minDamageConfig.getValue().floatValue() ? new AutoAnchorModule.AnchorCalc(data, isAnchor) : null;
   }

   private boolean playerDamageCheck(double playerDamage) {
      if (!mc.player.isCreative()) {
         float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         return this.safetyConfig.getValue() && playerDamage >= health + 0.5F ? true : playerDamage > this.maxLocalDamageConfig.getValue().floatValue();
      } else {
         return false;
      }
   }

   private List<BlockPos> getSphere(Vec3 origin) {
      List<BlockPos> sphere = new ArrayList<>();
      double rad = Math.ceil(this.rangeConfig.getValue().floatValue());

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.x() + x), (int)(origin.y() + y), (int)(origin.z() + z));
               sphere.add(new BlockPos(pos));
            }
         }
      }

      return sphere;
   }

   private boolean isValidTarget(Entity e) {
      return e instanceof Player && this.playersConfig.getValue()
         || EntityUtil.isMonster(e) && this.monstersConfig.getValue()
         || EntityUtil.isNeutral(e) && this.neutralsConfig.getValue()
         || EntityUtil.isPassive(e) && this.animalsConfig.getValue();
   }

   private int findNonBlockSlot() {
      int slot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private record AnchorCalc(BlockPos pos, boolean isAnchor) {
   }
}
