package net.shoreline.client.impl.module.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.shoreline.client.util.network.InteractType;
import net.shoreline.eventbus.annotation.EventListener;

public class AutoMountModule extends ToggleModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to mount entities", 0.1F, 4.0F, 6.0F));
   Config<Boolean> forceConfig = this.register(new BooleanConfig("ForceMount", "Forces the mounting without packets", false));
   Config<Boolean> horseConfig = this.register(new BooleanConfig("Horse", "Mounts horses", true));
   Config<Boolean> donkeyConfig = this.register(new BooleanConfig("Donkey", "Mounts donkeys", true));
   Config<Boolean> muleConfig = this.register(new BooleanConfig("Mule", "Mounts mules", true));
   Config<Boolean> llamaConfig = this.register(new BooleanConfig("Llama", "Mounts llamas", false));

   public AutoMountModule() {
      super("AutoMount", "Mounts nearby entities", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!mc.player.isHandsBusy()) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            double dist = mc.player.getEyePosition().distanceTo(entity.position());
            if (!(dist > this.rangeConfig.getValue().floatValue()) && this.checkMount(entity)) {
               mc.gameMode.interact(mc.player, entity, InteractionHand.MAIN_HAND);
               return;
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof IPlayerInteractEntityC2SPacket packet
         && this.forceConfig.getValue()
         && packet.getType() == InteractType.INTERACT_AT
         && this.checkMount(packet.getEntity())) {
         event.cancel();
      }
   }

   private boolean checkMount(Entity entity) {
      return this.horseConfig.getValue() && (entity instanceof Horse horseEntity && !horseEntity.isBaby() || entity instanceof SkeletonHorse)
         || this.donkeyConfig.getValue() && entity instanceof Donkey
         || this.muleConfig.getValue() && entity instanceof Mule
         || this.llamaConfig.getValue() && entity instanceof Llama llamaEntity && !llamaEntity.isBaby();
   }
}
