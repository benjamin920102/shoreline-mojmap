package net.shoreline.client.impl.module.render;

import java.awt.Color;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.particle.BlockBreakParticleEvent;
import net.shoreline.client.impl.event.particle.EmitParticleEvent;
import net.shoreline.client.impl.event.particle.ParticleEvent;
import net.shoreline.client.impl.event.particle.TotemParticleEvent;
import net.shoreline.eventbus.annotation.EventListener;

public class ParticlesModule extends ToggleModule {
   Config<ParticlesModule.TotemParticle> totemConfig = this.register(
      new EnumConfig<>("Totem", "Modify totem particle rendering", ParticlesModule.TotemParticle.OFF, ParticlesModule.TotemParticle.values())
   );
   Config<Color> totemColorConfig = this.register(
      new ColorConfig(
         "TotemColor",
         "Color of the totem particles",
         new Color(25, 120, 0),
         false,
         false,
         () -> this.totemConfig.getValue() == ParticlesModule.TotemParticle.COLOR
      )
   );
   Config<Integer> totemParticlesConfig = this.register(
      new NumberConfig<>("TotemParticles", "Max totem particles", 3, 16, 16, () -> this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE)
   );
   Config<Integer> totemParticleTicksConfig = this.register(
      new NumberConfig<>("TotemParticleTicks", "Max totem particle ticks", 5, 30, 30, () -> this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE)
   );
   Config<Boolean> explosionsConfig = this.register(new BooleanConfig("Explosions", "Prevents explosion particles from rendering", true));
   Config<Boolean> fireworkConfig = this.register(new BooleanConfig("Firework", "Prevents rendering of firework particles", false));
   Config<Boolean> potionConfig = this.register(new BooleanConfig("Effects", "Prevents rendering of potion effect particles", true));
   Config<Boolean> itemsConfig = this.register(new BooleanConfig("Items", "Prevents rendering of eating particles", false));
   Config<Boolean> bottleConfig = this.register(new BooleanConfig("BottleSplash", "Prevents rendering of bottle splash particles", true));
   Config<Boolean> portalConfig = this.register(new BooleanConfig("Portal", "Prevents rendering of portal particles", true));
   Config<Boolean> blockConfig = this.register(new BooleanConfig("Block", "Prevents block particles from rendering", false));
   Config<Boolean> blockBreakConfig = this.register(new BooleanConfig("BlockBreak", "Prevents block break particles from rendering", false));
   Config<Boolean> campfiresConfig = this.register(new BooleanConfig("Campfires", "Prevents campfire particles from rendering", false));
   Config<Boolean> obsidianTearConfig = this.register(new BooleanConfig("CryingObsidian", "Prevents obsidian tear particles from rendering", false));

   public ParticlesModule() {
      super("Particles", "Change the rendering of particles", ModuleCategory.RENDER);
   }

   @EventListener
   public void onParticle(ParticleEvent event) {
      if (this.potionConfig.getValue() && event.getParticleType() == ParticleTypes.ENTITY_EFFECT
         || this.explosionsConfig.getValue()
            && (event.getParticleType() == ParticleTypes.EXPLOSION || event.getParticleType() == ParticleTypes.EXPLOSION_EMITTER)
         || this.fireworkConfig.getValue() && event.getParticleType() == ParticleTypes.FIREWORK
         || this.itemsConfig.getValue() && event.getParticleType() == ParticleTypes.ITEM
         || this.bottleConfig.getValue() && (event.getParticleType() == ParticleTypes.EFFECT || event.getParticleType() == ParticleTypes.INSTANT_EFFECT)
         || this.portalConfig.getValue() && event.getParticleType() == ParticleTypes.PORTAL
         || this.blockConfig.getValue() && event.getParticleType() == ParticleTypes.BLOCK
         || this.campfiresConfig.getValue() && event.getParticleType() == ParticleTypes.CAMPFIRE_COSY_SMOKE
         || this.obsidianTearConfig.getValue()
            && (
               event.getParticleType() == ParticleTypes.FALLING_OBSIDIAN_TEAR
                  || event.getParticleType() == ParticleTypes.DRIPPING_OBSIDIAN_TEAR
                  || event.getParticleType() == ParticleTypes.LANDING_OBSIDIAN_TEAR
            )) {
         event.cancel();
      }
   }

   @EventListener
   public void onParticleEmitter(ParticleEvent.Emitter event) {
      if (this.totemConfig.getValue() == ParticlesModule.TotemParticle.REMOVE && event.getParticleType() == ParticleTypes.TOTEM_OF_UNDYING) {
         event.cancel();
      }
   }

   @EventListener
   public void onEmitParticle(EmitParticleEvent event) {
      if (this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE && event.getParticleType() == ParticleTypes.TOTEM_OF_UNDYING) {
         event.cancel();
         event.setParticleCount(this.totemParticlesConfig.getValue());
         event.setParticleTime(this.totemParticleTicksConfig.getValue());
      }
   }

   @EventListener
   public void onTotemParticle(TotemParticleEvent event) {
      if (this.totemConfig.getValue() == ParticlesModule.TotemParticle.COLOR) {
         event.cancel();
         Color color = this.totemColorConfig.getValue();
         float r = color.getRed() / 255.0F;
         float g = color.getGreen() / 255.0F;
         float b = color.getBlue() / 255.0F;
         if (RANDOM.nextInt(4) == 0) {
            float r2 = color.getRed() * 0.7F / 255.0F;
            float g2 = color.getGreen() * 0.7F / 255.0F;
            float b2 = color.getBlue() * 0.7F / 255.0F;
            event.setColor(new Color(Mth.clamp(r2, 0.0F, 1.0F), Mth.clamp(g2, 0.0F, 1.0F), Mth.clamp(b2, 0.0F, 1.0F)));
         } else {
            event.setColor(
               new Color(
                  Mth.clamp(r + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F),
                  Mth.clamp(g + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F),
                  Mth.clamp(b + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F)
               )
            );
         }
      }
   }

   @EventListener
   public void onBlockBreakParticle(BlockBreakParticleEvent event) {
      if (this.blockBreakConfig.getValue()) {
         event.cancel();
      }
   }

   private static enum TotemParticle {
      OFF,
      REMOVE,
      COLOR;
   }
}
