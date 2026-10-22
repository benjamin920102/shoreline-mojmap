package net.shoreline.client.impl.module.combat;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.player.RotationUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class ClickCrystalModule extends RotationModule {
   Config<Float> breakDelayConfig = this.register(new NumberConfig<>("SpawnDelay", "Speed to break crystals after spawning", 0.0F, 0.0F, 20.0F));
   Config<Float> randomDelayConfig = this.register(new NumberConfig<>("RandomDelay", "Randomized break delay", 0.0F, 0.0F, 5.0F));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before breaking", false));
   Config<Boolean> randomRotateConfig = this.register(
      new BooleanConfig("RotateJitter", "Slightly randomizes rotations", false, () -> this.rotateConfig.getValue())
   );
   private final Set<BlockPos> placedCrystals = new HashSet<>();
   private final Map<EndCrystal, Long> spawnedCrystals = new LinkedHashMap<>();
   private float randomDelay = -1.0F;

   public ClickCrystalModule() {
      super("ClickCrystal", "Automatically breaks placed crystals", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!this.spawnedCrystals.isEmpty()) {
         Entry<EndCrystal, Long> e = this.spawnedCrystals.entrySet().iterator().next();
         EndCrystal crystalEntity = e.getKey();
         Long time = e.getValue();
         if (this.randomDelay == -1.0F) {
            this.randomDelay = this.randomDelayConfig.getValue() == 0.0F ? 0.0F : RANDOM.nextFloat(this.randomDelayConfig.getValue() * 25.0F);
         }

         float breakDelay = this.breakDelayConfig.getValue() * 50.0F + this.randomDelay;
         double dist = mc.player.getEyePosition().distanceToSqr(crystalEntity.position());
         if (dist <= 12.25 && (float)(System.currentTimeMillis() - time) >= breakDelay) {
            if (this.rotateConfig.getValue()) {
               Vec3 rotatePos = crystalEntity.position();
               if (this.randomRotateConfig.getValue()) {
                  AABB bb = crystalEntity.getBoundingBox();
                  rotatePos = new Vec3(RANDOM.nextDouble(bb.minX, bb.maxX), RANDOM.nextDouble(bb.minY, bb.maxY), RANDOM.nextDouble(bb.minZ, bb.maxZ));
               }

               float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), rotatePos);
               this.setRotation(rotations[0], rotations[1]);
            }

            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(crystalEntity, mc.player.isShiftKeyDown()));
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.randomDelay = -1.0F;
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof ServerboundUseItemOnPacket packet
         && !event.isClientPacket()
         && mc.player.getItemInHand(packet.getHand()).getItem() instanceof EndCrystalItem) {
         this.placedCrystals.add(packet.getHitResult().getBlockPos());
      }
   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      if (event.getEntity() instanceof EndCrystal crystalEntity) {
         BlockPos base = crystalEntity.blockPosition().below();
         if (this.placedCrystals.contains(base)) {
            this.spawnedCrystals.put(crystalEntity, System.currentTimeMillis());
            this.placedCrystals.remove(base);
         }
      }
   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if (event.getEntity() instanceof EndCrystal crystalEntity) {
         this.spawnedCrystals.remove(crystalEntity);
      }
   }
}
