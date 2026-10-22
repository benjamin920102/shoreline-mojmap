package net.shoreline.client.impl.module.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.entity.LookDirectionEvent;
import net.shoreline.client.impl.event.network.PlayerUpdateEvent;
import net.shoreline.client.impl.module.RotationModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class BowAimModule extends RotationModule {
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Aims bow at players", true));
   Config<Boolean> monstersConfig = this.register(new BooleanConfig("Monsters", "Aims bow at monsters", false));
   Config<Boolean> neutralsConfig = this.register(new BooleanConfig("Neutrals", "Aims bow at neutrals", false));
   Config<Boolean> animalsConfig = this.register(new BooleanConfig("Animals", "Aims bow at animals", false));
   Config<Boolean> invisiblesConfig = this.register(new BooleanConfig("Invisibles", "Aims bow at invisible entities", false));
   private Entity aimTarget;

   public BowAimModule() {
      super("BowAim", "Automatically aims charged bow at nearby entities", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.aimTarget = null;
         if (mc.player.getMainHandItem().getItem() instanceof BowItem && mc.player.getTicksUsingItem() >= 3) {
            double minDist = Double.MAX_VALUE;

            for (Entity entity : mc.level.entitiesForRendering()) {
               if (entity != null && entity != mc.player && entity.isAlive() && this.isValidAimTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
                  double dist = mc.player.distanceTo(entity);
                  if (dist < minDist) {
                     minDist = dist;
                     this.aimTarget = entity;
                  }
               }
            }

            if (this.aimTarget instanceof LivingEntity target) {
               float[] rotations = this.getBowRotationsTo(target);
               this.setRotationClient(rotations[0], rotations[1]);
            }
         }
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.aimTarget != null) {
         event.cancel();
      }
   }

   private float[] getBowRotationsTo(Entity entity) {
      float duration = (mc.player.getUseItem().getUseDuration(mc.player) - mc.player.getTicksUsingItem()) / 20.0F;
      duration = (duration * duration + duration * 2.0F) / 3.0F;
      if (duration >= 1.0F) {
         duration = 1.0F;
      }

      double duration1 = duration * 3.0F;
      double coeff = 0.05F;
      float pitch = (float)(-Math.toDegrees(this.calculateArc(entity, duration1, coeff)));
      double ix = entity.getX() - entity.xo;
      double iz = entity.getZ() - entity.zo;
      double d = mc.player.distanceTo(entity);
      d -= d % 2.0;
      ix = d / 2.0 * ix * (mc.player.isSprinting() ? 1.3 : 1.1);
      iz = d / 2.0 * iz * (mc.player.isSprinting() ? 1.3 : 1.1);
      float yaw = (float)Math.toDegrees(Math.atan2(entity.getZ() + iz - mc.player.getZ(), entity.getX() + ix - mc.player.getX())) - 90.0F;
      return new float[]{yaw, pitch};
   }

   private float calculateArc(Entity target, double duration, double coeff) {
      double yArc = target.getY() + target.getEyeHeight() / 2.0F - (mc.player.getY() + mc.player.getEyeHeight());
      double dX = target.getX() - mc.player.getX();
      double dZ = target.getZ() - mc.player.getZ();
      double dirRoot = Math.sqrt(dX * dX + dZ * dZ);
      return this.calculateArc(duration, coeff, dirRoot, yArc);
   }

   private float calculateArc(double duration, double coeff, double root, double yArc) {
      double dirCoeff = coeff * (root * root);
      yArc = 2.0 * yArc * (duration * duration);
      yArc = coeff * (dirCoeff + yArc);
      yArc = Math.sqrt(duration * duration * duration * duration - yArc);
      duration = duration * duration - yArc;
      yArc = Math.atan2(duration * duration + yArc, coeff * root);
      duration = Math.atan2(duration, coeff * root);
      return (float)Math.min(yArc, duration);
   }

   private boolean isValidAimTarget(Entity entity) {
      return entity.isInvisible() && !this.invisiblesConfig.getValue()
         ? false
         : entity instanceof Player && this.playersConfig.getValue()
            || EntityUtil.isMonster(entity) && this.monstersConfig.getValue()
            || EntityUtil.isNeutral(entity) && this.neutralsConfig.getValue()
            || EntityUtil.isPassive(entity) && this.animalsConfig.getValue();
   }
}
