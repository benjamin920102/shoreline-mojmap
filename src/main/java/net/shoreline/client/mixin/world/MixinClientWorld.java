package net.shoreline.client.mixin.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.world.AddEntityEvent;
import net.shoreline.client.impl.event.world.PlaySoundEvent;
import net.shoreline.client.impl.event.world.RemoveEntityEvent;
import net.shoreline.client.impl.event.world.SetBlockStateEvent;
import net.shoreline.client.impl.event.world.SkyboxEvent;
import net.shoreline.client.impl.event.world.UnloadChunkBlocksEvent;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientLevel.class})
public abstract class MixinClientWorld {
   @Shadow
   protected abstract LevelEntityGetter<Entity> getEntities();

   @Inject(
      method = {"addEntity(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")}
   )
   private void hookAddEntity(Entity entity, CallbackInfo ci) {
      AddEntityEvent addEntityEvent = new AddEntityEvent(entity);
      EventBus.INSTANCE.dispatch(addEntityEvent);
   }

   @Inject(
      method = {"removeEntity(ILnet/minecraft/world/entity/Entity$RemovalReason;)V"},
      at = {@At("HEAD")}
   )
   private void hookRemoveEntity(int entityId, RemovalReason removalReason, CallbackInfo ci) {
      Entity entity = this.getEntities().get(entityId);
      if (entity != null) {
         RemoveEntityEvent removeEntityEvent = new RemoveEntityEvent(entity, removalReason);
         EventBus.INSTANCE.dispatch(removeEntityEvent);
      }
   }

   @Inject(
      method = {"getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Vec3> cir) {
      SkyboxEvent.Sky skyboxEvent = new SkyboxEvent.Sky();
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(skyboxEvent.getColorVec());
      }
   }

   @Inject(
      method = {"getCloudColor(F)Lnet/minecraft/world/phys/Vec3;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
      SkyboxEvent.Cloud skyboxEvent = new SkyboxEvent.Cloud();
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(skyboxEvent.getColorVec());
      }
   }

   @Inject(
      method = {"playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookPlaySound(
      double x, double y, double z, SoundEvent event, SoundSource category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci
   ) {
      PlaySoundEvent playSoundEvent = new PlaySoundEvent(new Vec3(x, y, z), event, category);
      EventBus.INSTANCE.dispatch(playSoundEvent);
      if (playSoundEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
      SetBlockStateEvent setBlockStateEvent = new SetBlockStateEvent(flags, pos, state);
      EventBus.INSTANCE.dispatch(setBlockStateEvent);
      if (setBlockStateEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V"},
      at = {@At("HEAD")}
   )
   private void hookUnloadBlockEntities(LevelChunk chunk, CallbackInfo ci) {
      UnloadChunkBlocksEvent unloadChunkBlocksEvent = new UnloadChunkBlocksEvent(chunk);
      EventBus.INSTANCE.dispatch(unloadChunkBlocksEvent);
   }
}
