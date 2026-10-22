package net.shoreline.client.util.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.shoreline.client.util.Globals;

public class FakePlayerEntity extends RemotePlayer implements Globals {
   public static final AtomicInteger CURRENT_ID = new AtomicInteger(1000000);
   private final Player player;

   public FakePlayerEntity(Player player, String name) {
      super(Minecraft.getInstance().level, new GameProfile(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), name));
      this.player = player;
      this.copyPosition(player);
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.yHeadRot = player.yHeadRot;
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = player.yBodyRot;
      this.yBodyRotO = this.yBodyRot;
      this.walkAnimation.position = player.walkAnimation.position();
      this.walkAnimation.setSpeed(player.walkAnimation.speed());
      Byte playerModel = player.getEntityData().get(Player.DATA_PLAYER_MODE_CUSTOMISATION);
      this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
      this.getAttributes().assignAllValues(player.getAttributes());
      this.setShiftKeyDown(player.isShiftKeyDown());
      this.setSwimming(player.isSwimming());
      this.setPose(player.getPose());
      this.setHealth(player.getHealth());
      this.setAbsorptionAmount(player.getAbsorptionAmount());
      this.getInventory().replaceWith(player.getInventory());
      this.setId(CURRENT_ID.incrementAndGet());
      this.tickCount = 100;
   }

   public FakePlayerEntity(Player player) {
      this(player, player.getName().getString());
   }

   public void spawnPlayer() {
      if (mc.level != null) {
         this.unsetRemoved();
         mc.level.addEntity(this);
      }
   }

   public void despawnPlayer() {
      if (mc.level != null) {
         mc.level.removeEntity(this.getId(), RemovalReason.DISCARDED);
         this.setRemoved(RemovalReason.DISCARDED);
      }
   }

   @Override
   public boolean isDeadOrDying() {
      return false;
   }

   public Player getPlayer() {
      return this.player;
   }
}
