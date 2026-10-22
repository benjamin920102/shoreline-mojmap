package net.shoreline.client.impl.manager.world.sound;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.shoreline.client.util.Globals;

public class SoundManager implements Globals {
   private static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();
   public static final SoundEvent GUI_CLICK = registerSound("gui_click");
   public static final SoundEvent TWITTER = registerSound("twitter");
   public static final SoundEvent IOS = registerSound("ios");
   public static final SoundEvent DISCORD = registerSound("discord");
   public static final SoundEvent STEAM = registerSound("steam");

   private static SoundEvent registerSound(String name) {
      registerSoundFile(name + ".ogg");
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("shoreline", name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   private static void registerSoundFile(String soundFile) {
      REGISTERED_SOUND_FILES.add(soundFile);
   }

   private static InputStream loadResource(String resourcePath) {
      try {
         return SoundManager.class.getClassLoader().getResourceAsStream(resourcePath);
      } catch (Exception var2) {
         return null;
      }
   }

   public void playSound(SoundEvent sound) {
      this.playSound(sound, 1.2F, 0.75F);
   }

   public void playSound(SoundEvent sound, float volume, float pitch) {
      if (mc.player != null) {
         mc.executeIfPossible(() -> mc.player.playSound(sound, volume, pitch));
      }
   }

   public static Set<String> getRegisteredSoundFiles() {
      return new HashSet<>(REGISTERED_SOUND_FILES);
   }
}
