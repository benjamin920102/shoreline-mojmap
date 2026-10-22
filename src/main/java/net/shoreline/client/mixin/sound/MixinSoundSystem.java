package net.shoreline.client.mixin.sound;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({SoundEngine.class})
public class MixinSoundSystem {
   @Shadow
   private boolean loaded;
   @Shadow
   private int tickCount;
   @Shadow
   @Final
   private Map<SoundInstance, Integer> soundDeleteTime;
   @Shadow
   @Final
   private Map<SoundInstance, ChannelHandle> instanceToChannel;
}
