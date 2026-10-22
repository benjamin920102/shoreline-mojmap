package net.shoreline.client.impl.manager.combat.hole;

import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.module.render.HoleESPModule;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.client.util.world.BlockUtil;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class HoleManager implements Globals {
   private final ExecutorService executor = Executors.newFixedThreadPool(1);
   private Future<Set<Hole>> result;
   private Set<Hole> holes = new ConcurrentSet();

   public HoleManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onTickEvent(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         HoleManager.HoleTask runnable = new HoleManager.HoleTask(this.getSphere(mc.player.position()));
         this.result = this.executor.submit(runnable);
      }
   }

   public List<BlockPos> getSphere(Vec3 start) {
      List<BlockPos> sphere = new ArrayList<>();
      double rad = Math.ceil(Math.max(5.0, HoleESPModule.getInstance().getRange()));

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(start.x() + x), (int)(start.y() + y), (int)(start.z() + z));
               BlockPos p = new BlockPos(pos);
               sphere.add(p);
            }
         }
      }

      return sphere;
   }

   public Hole checkHole(BlockPos pos) {
      if (pos.getY() == mc.level.getMinBuildHeight() && !BlastResistantBlocks.isUnbreakable(pos)) {
         return new Hole(pos, HoleType.VOID);
      } else {
         int resistant = 0;
         int unbreakable = 0;
         if (BlockUtil.isBlockAccessible(pos)) {
            BlockPos pos1 = pos.offset(-1, 0, 0);
            BlockPos pos2 = pos.offset(0, 0, -1);
            if (BlastResistantBlocks.isBlastResistant(pos1)) {
               resistant++;
            } else if (BlastResistantBlocks.isUnbreakable(pos1)) {
               unbreakable++;
            }

            if (BlastResistantBlocks.isBlastResistant(pos2)) {
               resistant++;
            } else if (BlastResistantBlocks.isUnbreakable(pos2)) {
               unbreakable++;
            }

            if (resistant + unbreakable < 2) {
               return null;
            } else {
               BlockPos pos3 = pos.offset(0, 0, 1);
               BlockPos pos4 = pos.offset(1, 0, 0);
               boolean air3 = mc.level.isEmptyBlock(pos3);
               boolean air4 = mc.level.isEmptyBlock(pos4);
               if (air3 && air4) {
                  BlockPos pos5 = pos.offset(1, 0, 1);
                  if (!mc.level.isEmptyBlock(pos5)) {
                     return null;
                  } else {
                     BlockPos[] quad = new BlockPos[]{
                        pos.offset(-1, 0, 1), pos.offset(0, 0, 2), pos.offset(1, 0, 2), pos.offset(2, 0, 1), pos.offset(2, 0, 0), pos.offset(1, 0, -1)
                     };

                     for (BlockPos p : quad) {
                        if (BlastResistantBlocks.isBlastResistant(p)) {
                           resistant++;
                        } else if (BlastResistantBlocks.isUnbreakable(p)) {
                           unbreakable++;
                        }
                     }

                     if (resistant != 8 && unbreakable != 8 && resistant + unbreakable != 8) {
                        return null;
                     } else {
                        Hole quadHole = new Hole(
                           pos,
                           resistant == 8 ? HoleType.OBSIDIAN : (unbreakable == 8 ? HoleType.BEDROCK : HoleType.OBSIDIAN_BEDROCK),
                           pos1,
                           pos2,
                           pos3,
                           pos4,
                           pos5
                        );
                        quadHole.addHoleOffsets(quad);
                        return quadHole;
                     }
                  }
               } else if (air3 && BlockUtil.isBlockAccessible(pos3)) {
                  BlockPos[] doubleZ = new BlockPos[]{pos.offset(-1, 0, 1), pos.offset(0, 0, 2), pos.offset(1, 0, 1), pos.offset(1, 0, 0)};

                  for (BlockPos px : doubleZ) {
                     if (BlastResistantBlocks.isBlastResistant(px)) {
                        resistant++;
                     } else if (BlastResistantBlocks.isUnbreakable(px)) {
                        unbreakable++;
                     }
                  }

                  if (resistant != 6 && unbreakable != 6 && resistant + unbreakable != 6) {
                     return null;
                  } else {
                     Hole doubleZHole = new Hole(
                        pos, resistant == 6 ? HoleType.OBSIDIAN : (unbreakable == 6 ? HoleType.BEDROCK : HoleType.OBSIDIAN_BEDROCK), pos1, pos2, pos3
                     );
                     doubleZHole.addHoleOffsets(doubleZ);
                     return doubleZHole;
                  }
               } else if (air4 && BlockUtil.isBlockAccessible(pos4)) {
                  BlockPos[] doubleX = new BlockPos[]{pos.offset(0, 0, 1), pos.offset(1, 0, 1), pos.offset(2, 0, 0), pos.offset(1, 0, -1)};

                  for (BlockPos pxx : doubleX) {
                     if (BlastResistantBlocks.isBlastResistant(pxx)) {
                        resistant++;
                     } else if (BlastResistantBlocks.isUnbreakable(pxx)) {
                        unbreakable++;
                     }
                  }

                  if (resistant != 6 && unbreakable != 6 && resistant + unbreakable != 6) {
                     return null;
                  } else {
                     Hole doubleXHole = new Hole(
                        pos, resistant == 6 ? HoleType.OBSIDIAN : (unbreakable == 6 ? HoleType.BEDROCK : HoleType.OBSIDIAN_BEDROCK), pos1, pos2, pos4
                     );
                     doubleXHole.addHoleOffsets(doubleX);
                     return doubleXHole;
                  }
               } else {
                  if (BlastResistantBlocks.isBlastResistant(pos3)) {
                     resistant++;
                  } else if (BlastResistantBlocks.isUnbreakable(pos3)) {
                     unbreakable++;
                  }

                  if (BlastResistantBlocks.isBlastResistant(pos4)) {
                     resistant++;
                  } else if (BlastResistantBlocks.isUnbreakable(pos4)) {
                     unbreakable++;
                  }

                  return resistant != 4 && unbreakable != 4 && resistant + unbreakable != 4
                     ? null
                     : new Hole(
                        pos, resistant == 4 ? HoleType.OBSIDIAN : (unbreakable == 4 ? HoleType.BEDROCK : HoleType.OBSIDIAN_BEDROCK), pos1, pos2, pos3, pos4
                     );
               }
            }
         } else {
            return null;
         }
      }
   }

   public Set<Hole> getHoles() {
      if (this.result != null) {
         try {
            this.holes = this.result.get();
         } catch (InterruptedException | ExecutionException var2) {
         }
      }

      return this.holes;
   }

   public class HoleTask implements Callable<Set<Hole>> {
      private final List<BlockPos> blocks;

      public HoleTask(List<BlockPos> blocks) {
         this.blocks = blocks;
      }

      public Set<Hole> call() throws Exception {
         Set<Hole> holes1 = new HashSet<>();

         for (BlockPos blockPos : this.blocks) {
            Hole hole = HoleManager.this.checkHole(blockPos);
            if (hole != null) {
               holes1.add(hole);
            }
         }

         return holes1;
      }
   }
}
