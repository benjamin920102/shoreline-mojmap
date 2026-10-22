package net.shoreline.client.mixin.sound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;

@Mixin(SoundEngine.class)
public class MixinSoundSystem
{
    @Shadow
    private boolean loaded;
    //
    @Shadow
    private int tickCount;
    //
    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundDeleteTime;
    //
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

//    /**
//     * @param sound
//     * @param cir
//     */
//    @Inject(method = "isActive", at = @At(value = "HEAD"), cancellable = true)
//    public void isPlaying(SoundInstance sound, CallbackInfoReturnable<Boolean> cir) {
//        // Fixes Soundsystem tick crash
//        cir.cancel();
//        Integer i = soundDeleteTime.get(sound);
//        if (!loaded) {
//            cir.setReturnValue(false);
//        }
//        if (i != null && i <= tickCount) {
//            cir.setReturnValue(true);
//        }
//        cir.setReturnValue(instanceToChannel.containsKey(sound));
//    }
}
