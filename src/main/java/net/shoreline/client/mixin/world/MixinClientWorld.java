package net.shoreline.client.mixin.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.impl.event.world.*;
import net.shoreline.eventbus.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author linus
 * @since 1.0
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientWorld
{

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    /**
     * @param entity
     * @param ci
     */
    @Inject(method = "addEntity", at = @At(value = "HEAD"))
    private void hookAddEntity(Entity entity, CallbackInfo ci)
    {
        AddEntityEvent addEntityEvent = new AddEntityEvent(entity);
        EventBus.INSTANCE.dispatch(addEntityEvent);
    }

    /**
     * @param entityId
     * @param removalReason
     * @param ci
     */
    @Inject(method = "removeEntity", at = @At(value = "HEAD"))
    private void hookRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci)
    {
        Entity entity = getEntities().get(entityId);
        if (entity == null)
        {
            return;
        }
        RemoveEntityEvent removeEntityEvent = new RemoveEntityEvent(entity, removalReason);
        EventBus.INSTANCE.dispatch(removeEntityEvent);
    }

    /**
     * @param cameraPos
     * @param tickDelta
     * @param cir
     */
    @Inject(method = "getSkyColor", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetSkyColor(Vec3 cameraPos, float tickDelta,
                                 CallbackInfoReturnable<Vec3> cir)
    {
        SkyboxEvent.Sky skyboxEvent = new SkyboxEvent.Sky();
        EventBus.INSTANCE.dispatch(skyboxEvent);
        if (skyboxEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(skyboxEvent.getColorVec());
        }
    }

    /**
     * @param tickDelta
     * @param cir
     */
    @Inject(method = "getCloudColor", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetCloudsColor(float tickDelta,
                                    CallbackInfoReturnable<Vec3> cir)
    {
        SkyboxEvent.Cloud skyboxEvent = new SkyboxEvent.Cloud();
        EventBus.INSTANCE.dispatch(skyboxEvent);
        if (skyboxEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(skyboxEvent.getColorVec());
        }
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookPlaySound(double x, double y, double z, SoundEvent event, SoundSource category,
                               float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci)
    {
        PlaySoundEvent playSoundEvent = new PlaySoundEvent(new Vec3(x, y, z), event, category);
        EventBus.INSTANCE.dispatch(playSoundEvent);
        if (playSoundEvent.isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "setBlockAndUpdate", at = @At(value = "HEAD"), cancellable = true)
    private void hookSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir)
    {
        SetBlockStateEvent setBlockStateEvent = new SetBlockStateEvent(flags, pos, state);
        EventBus.INSTANCE.dispatch(setBlockStateEvent);
        if (setBlockStateEvent.isCanceled())
        {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "unload", at = @At(value = "HEAD"))
    private void hookUnloadBlockEntities(LevelChunk chunk, CallbackInfo ci)
    {
        UnloadChunkBlocksEvent unloadChunkBlocksEvent = new UnloadChunkBlocksEvent(chunk);
        EventBus.INSTANCE.dispatch(unloadChunkBlocksEvent);
    }
}
