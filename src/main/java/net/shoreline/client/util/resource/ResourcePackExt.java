package net.shoreline.client.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public final class ResourcePackExt implements PackResources, ModResourcePack {
   public static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();
   private final ModNioResourcePack parent;

   public ResourcePackExt(ModNioResourcePack parent) {
      this.parent = parent;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... segments) {
      return this.parent.getRootResource(segments);
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
      String formattedName = String.format("assets/shoreline/%s", id.getPath());
      InputStream is = (InputStream)this.getResourceInternal(formattedName);
      return is != null ? () -> is : this.parent.getResource(type, id);
   }

   private Object getResourceInternal(String name) {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream(name);
      if (is == null) {
         if (name.startsWith("assets/")) {
            is = this.getClass().getClassLoader().getResourceAsStream(name.substring(7));
         }

         if (is == null && !name.startsWith("/")) {
            is = this.getClass().getClassLoader().getResourceAsStream("/" + name);
         }
      }

      return is;
   }

   @Override
   public void listResources(PackType type, String namespace, String prefix, ResourceOutput consumer) {
      if (prefix.equals("sounds")) {
         for (String soundFile : REGISTERED_SOUND_FILES) {
            String formattedName = String.format("assets/shoreline/sounds/%s", soundFile);
            InputStream is = (InputStream)this.getResourceInternal(formattedName);
            if (is != null) {
               ResourceLocation id = ResourceLocation.fromNamespaceAndPath("shoreline", String.format("sounds/%s", soundFile));
               consumer.accept(id, () -> is);
            }
         }
      }

      this.parent.listResources(type, namespace, prefix, consumer);
   }

   @Override
   public Set<String> getNamespaces(PackType type) {
      return this.parent.getNamespaces(type);
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException {
      return (T)this.parent.getMetadataSection(metaReader);
   }

   @Override
   public PackLocationInfo location() {
      return this.parent.location();
   }

   @Override
   public ModMetadata getFabricModMetadata() {
      return this.parent.getFabricModMetadata();
   }

   @Override
   public ModResourcePack createOverlay(String overlay) {
      return this.parent.createOverlay(overlay);
   }

   @Override
   public void close() {
      this.parent.close();
   }
}
