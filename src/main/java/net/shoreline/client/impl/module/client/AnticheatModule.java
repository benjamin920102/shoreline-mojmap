package net.shoreline.client.impl.module.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ConcurrentModule;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.shoreline.client.util.math.position.DirectionUtil;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

public class AnticheatModule extends ConcurrentModule {
   private static AnticheatModule INSTANCE;
   Config<AnticheatModule.Anticheats> modeConfig = this.register(
      new EnumConfig<>("Mode", "Applies anticheat optimizations", AnticheatModule.Anticheats.VANILLA, AnticheatModule.Anticheats.values())
   );
   Config<Float> entityPlaceConfig = this.register(
      new NumberConfig<>(
         "PlaceThreshold", "The max ticks to place on entities", 0.0F, 2.0F, 25.0F, () -> this.modeConfig.getValue() != AnticheatModule.Anticheats.VANILLA
      )
   );
   Config<Boolean> miningFixConfig = this.register(
      new BooleanConfig("MiningFix", "Fixes vanilla mining on GrimV3", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM)
   );
   Config<Boolean> webJumpFixConfig = this.register(
      new BooleanConfig("WebJumpFix", "Fixes sprint jumping in webs on grim", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM)
   );
   Config<Boolean> noScreenCloseConfig = this.register(
      new BooleanConfig(
         "NoScreenClose", "Prevents the server from closing your inventory screen", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM
      )
   );
   Config<Boolean> raytraceSpoofConfig = this.register(
      new BooleanConfig("RaytraceFix", "Allows you to spoof your raytrace", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P)
   );
   private final Timer raytraceTimer = new CacheTimer();
   private float pitch = Float.NaN;

   public AnticheatModule() {
      super("Anticheat", "Settings for anticheat configs", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static AnticheatModule getInstance() {
      return INSTANCE;
   }

   @EventListener(
      priority = 10000
   )
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.level != null) {
         if (this.isNCP()
            && this.raytraceSpoofConfig.getValue()
            && event.getPacket() instanceof ServerboundUseItemOnPacket packet
            && this.raytraceTimer.passed(250)) {
            BlockHitResult packetResult = packet.getHitResult();
            BlockPos pos = packetResult.getBlockPos();
            BlockHitResult result = mc.level
               .clip(
                  new ClipContext(
                     mc.player.getEyePosition(), DirectionUtil.getDirectionOffsetPos(pos, packetResult.getDirection()), Block.COLLIDER, Fluid.NONE, mc.player
                  )
               );
            if (mc.level.noCollision(mc.player.getBoundingBox().expandTowards(0.0, 0.15, 0.0))
               && result != null
               && result.getType() == Type.BLOCK
               && !result.getBlockPos().equals(pos)) {
               this.pitch = -75.0F;
               this.raytraceTimer.reset();
            }
         }

         if (event.getPacket() instanceof ServerboundMovePlayerPacket packetx && packetx.hasRotation() && !Float.isNaN(this.pitch)) {
            ((AccessorPlayerMoveC2SPacket)packetx).hookSetPitch(this.pitch);
            this.pitch = Float.NaN;
         }

         if (this.isGrim()
            && this.miningFixConfig.getValue()
            && event.getPacket() instanceof ServerboundPlayerActionPacket packetx
            && (
               packetx.getAction() == Action.START_DESTROY_BLOCK
                  || packetx.getAction() == Action.STOP_DESTROY_BLOCK
                  || packetx.getAction() == Action.ABORT_DESTROY_BLOCK
            )) {
            if (BlastResistantBlocks.isUnbreakable(packetx.getPos())) {
               event.cancel();
               return;
            }

            if (packetx.getAction() == Action.START_DESTROY_BLOCK) {
               Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               event.cancel();
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.level != null) {
         if (this.isGrim()
            && this.noScreenCloseConfig.getValue()
            && event.getPacket() instanceof ClientboundContainerClosePacket packet
            && packet.getContainerId() == 0) {
            event.cancel();
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.raytraceSpoofConfig && !this.raytraceSpoofConfig.getValue()) {
         this.pitch = Float.NaN;
      }
   }

   public boolean isGrim() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM;
   }

   public boolean isNCP() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P;
   }

   public boolean getMiningFix() {
      return this.miningFixConfig.getValue();
   }

   public boolean getWebJumpFix() {
      return this.webJumpFixConfig.getValue();
   }

   public int getEntityPlaceThreshold() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.VANILLA ? 0 : Math.round(this.entityPlaceConfig.getValue() * 10.0F);
   }

   private static enum Anticheats {
      GRIM,
      N_C_P,
      VANILLA;
   }
}
