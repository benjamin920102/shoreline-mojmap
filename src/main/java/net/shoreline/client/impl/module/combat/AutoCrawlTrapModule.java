package net.shoreline.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoCrawlTrapModule extends ObsidianPlacerModule {
   private static AutoCrawlTrapModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("PlaceRange", "The range to trap enemies", 0.1F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Boolean> downConfig = this.register(new BooleanConfig("PreventDownwards", "Prevents digging downwards", true));
   Config<Boolean> serverHitboxConfig = this.register(new BooleanConfig("HitboxSync", "Places on serverside crawling hitboxes", false));
   Config<Boolean> mineIgnoreConfig = this.register(new BooleanConfig("PreventMine", "Prevents enemies from mining the trap", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Integer> extrapolateTicksConfig = this.register(
      new NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10)
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders trap placements", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private List<BlockPos> surround = new ArrayList<>();
   private List<BlockPos> placements = new ArrayList<>();
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private Player target;
   private int blocksPlaced;

   public AutoCrawlTrapModule() {
      super("AutoCrawlTrap", "Automatically places blocks to keep enemies in crawl", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoCrawlTrapModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.fadeList.clear();
      this.target = null;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.blocksPlaced = 0;
      if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.surround.clear();
         this.placements.clear();
      } else {
         ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
         if (blockSlot != null) {
            int slot = blockSlot.slot();
            if (slot == -1) {
               this.surround.clear();
               this.placements.clear();
            } else {
               this.target = this.getClosestPlayer(this.enemyRangeConfig.getValue().floatValue());
               if (this.target == null) {
                  this.surround.clear();
                  this.placements.clear();
               } else {
                  BlockPos targetPos = EntityUtil.getRoundedBlockPos(this.target);
                  this.surround = this.getCrawlTrap(this.target, targetPos);
                  if (this.canCrawlTrap(this.target, targetPos) && !this.surround.isEmpty()) {
                     this.placements = this.getPlacementsFromTrap(this.surround);
                     if (!this.placements.isEmpty()) {
                        this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                        while (this.blocksPlaced < this.shiftTicksConfig.getValue() && this.blocksPlaced < this.placements.size()) {
                           BlockPos targetPlacePos = this.placements.get(this.blocksPlaced);
                           this.placeBlock(targetPlacePos, slot);
                        }

                        if (this.rotateConfig.getValue()) {
                           Managers.ROTATION.setRotationSilentSync();
                        }
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
         if (event.getPacket() instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
               this.handlePackets(packet1);
            }
         } else {
            this.handlePackets(event.getPacket());
         }
      }
   }

   private void handlePackets(Packet<?> serverPacket) {
      if (serverPacket instanceof ClientboundBlockUpdatePacket packet) {
         BlockState blockState = packet.getBlockState();
         BlockPos targetPos = packet.getPos();
         if (this.surround.contains(targetPos)) {
            if (blockState.canBeReplaced() && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, targetPos, CollisionContext.empty())) {
               ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
               if (blockSlot == null) {
                  return;
               }

               int slot = blockSlot.slot();
               if (slot == -1) {
                  return;
               }

               this.placeBlock(targetPos, slot);
            } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
               this.packets.remove(targetPos);
            }
         }
      }
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
      this.blocksPlaced++;
   }

   public List<BlockPos> getPlacementsFromTrap(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();

      for (BlockPos surroundPos : surround) {
         Long placed = this.packets.get(surroundPos);
         if (!(this.shiftDelayConfig.getValue() > 0.0F)
            || placed == null
            || !((float)(System.currentTimeMillis() - placed) < this.shiftDelayConfig.getValue() * 50.0F)) {
            AABB surroundBox = new AABB(surroundPos);
            List<Entity> invalid = mc.level.getEntities(null, surroundBox).stream().filter(e -> this.invalidEntity(e)).toList();
            boolean serverCrawling = invalid.stream()
               .allMatch(e -> Managers.HITBOX.isServerCrawling(e) && Managers.HITBOX.getCrawlingBoundingBox(e).intersects(surroundBox));
            if (mc.level.getBlockState(surroundPos).canBeReplaced()
               || serverCrawling && this.serverHitboxConfig.getValue()
               || Managers.BLOCK.isPassed(surroundPos, 0.7F) && this.mineIgnoreConfig.getValue()) {
               double dist = mc.player.distanceToSqr(surroundPos.getCenter());
               if (!(dist > ((NumberConfig)this.rangeConfig).getValueSq())
                  && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, surroundPos, CollisionContext.empty())) {
                  placements.add(surroundPos);
               }
            }
         }
      }

      return placements;
   }

   public List<BlockPos> getCrawlTrap(Player entity, BlockPos playerPos) {
      List<BlockPos> crawlTrap = new ArrayList<>();
      crawlTrap.add(playerPos.above());
      if (this.downConfig.getValue()) {
         crawlTrap.add(playerPos.below());
      }

      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();

      for (int ticks = 0; ticks <= this.extrapolateTicksConfig.getValue(); ticks++) {
         double ox = (x - entity.xo) * ticks;
         double oz = (z - entity.zo) * ticks;
         BlockPos blockPos = BlockPos.containing(x + ox, y, z + oz);
         if (!crawlTrap.contains(blockPos.above())) {
            crawlTrap.add(blockPos.above());
         }

         if (this.downConfig.getValue() && !crawlTrap.contains(blockPos.below())) {
            crawlTrap.add(blockPos.below());
         }
      }

      return crawlTrap;
   }

   public boolean invalidEntity(Entity entity) {
      return !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof Arrow);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            set.getValue().setState(false);
            int boxAlpha = (int)(40.0 * set.getValue().getFactor());
            int lineAlpha = (int)(100.0 * set.getValue().getFactor());
            Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
            RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
         }

         RenderBuffers.postRender();
         if (this.placements.isEmpty()) {
            return;
         }

         for (BlockPos pos : this.placements) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(pos, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   private boolean canCrawlTrap(Player player, BlockPos playerPos) {
      return player.onGround() || !mc.level.getBlockState(playerPos.above()).canBeReplaced() || !mc.level.getBlockState(playerPos.above(2)).canBeReplaced();
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }
}
