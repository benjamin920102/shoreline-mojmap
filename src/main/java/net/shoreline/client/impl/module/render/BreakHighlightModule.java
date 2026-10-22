package net.shoreline.client.impl.module.render;

import java.awt.Color;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

public class BreakHighlightModule extends ToggleModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to render breaking blocks", 5.0F, 10.0F, 50.0F));
   Config<Color> colorConfig = this.register(new ColorConfig("Color", "The break highlight color", new Color(255, 0, 0), false, true));
   private final Map<ClientboundBlockDestructionPacket, Long> breakingProgress = new ConcurrentHashMap<>();

   public BreakHighlightModule() {
      super("BreakHighlight", "Highlights blocks that are being broken", ModuleCategory.RENDER);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet && !BlastResistantBlocks.isUnbreakable(packet.getPos())) {
         ClientboundBlockDestructionPacket p = this.getPacketFromPos(packet.getPos());
         if (p != null) {
            this.breakingProgress.replace(p, System.currentTimeMillis());
         } else {
            this.breakingProgress.put(packet, System.currentTimeMillis());
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.level != null) {
         RenderBuffers.preRender();

         for (Entry<ClientboundBlockDestructionPacket, Long> mine : this.breakingProgress.entrySet()) {
            BlockPos mining = mine.getKey().getPos();
            long elapsedTime = System.currentTimeMillis() - mine.getValue();

            for (long count = this.breakingProgress.keySet().stream().filter(p -> p.getId() == mine.getKey().getId()).count(); count > 2L; count--) {
               this.breakingProgress
                  .entrySet()
                  .stream()
                  .filter(p -> p.getKey().getId() == mine.getKey().getId())
                  .min(Comparator.comparingLong(Entry::getValue))
                  .ifPresent(min -> this.breakingProgress.remove(min.getKey(), min.getValue()));
            }

            if (!mc.level.isEmptyBlock(mining) && elapsedTime <= 2500L) {
               double dist = mc.player.distanceToSqr(mining.getCenter());
               if (!(dist > ((NumberConfig)this.rangeConfig).getValueSq())) {
                  VoxelShape outlineShape = mc.level.getBlockState(mining).getShape(mc.level, mining);
                  outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
                  AABB render1 = outlineShape.bounds();
                  AABB render = new AABB(
                     mining.getX() + render1.minX,
                     mining.getY() + render1.minY,
                     mining.getZ() + render1.minZ,
                     mining.getX() + render1.maxX,
                     mining.getY() + render1.maxY,
                     mining.getZ() + render1.maxZ
                  );
                  Vec3 center = render.getCenter();
                  float scale = Mth.clamp((float)elapsedTime / 2500.0F, 0.0F, 1.0F);
                  double dx = (render1.maxX - render1.minX) / 2.0;
                  double dy = (render1.maxY - render1.minY) / 2.0;
                  double dz = (render1.maxZ - render1.minZ) / 2.0;
                  AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
                  RenderManager.renderBox(event.getMatrices(), scaled, ((ColorConfig)this.colorConfig).getValue(40).getRGB());
                  RenderManager.renderBoundingBox(event.getMatrices(), scaled, 1.5F, ((ColorConfig)this.colorConfig).getValue(100).getRGB());
               }
            } else {
               this.breakingProgress.remove(mine.getKey(), mine.getValue());
            }
         }

         RenderBuffers.postRender();
      }
   }

   private ClientboundBlockDestructionPacket getPacketFromPos(BlockPos pos) {
      return this.breakingProgress.keySet().stream().filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
   }
}
