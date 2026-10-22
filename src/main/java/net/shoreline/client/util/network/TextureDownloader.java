package net.shoreline.client.util.network;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.Shoreline;
import net.shoreline.client.util.Globals;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public final class TextureDownloader implements Globals {
   private final CloseableHttpClient client = HttpClients.createDefault();
   private final Map<String, ResourceLocation> cache = new ConcurrentHashMap<>();
   private final Set<String> downloading = new HashSet<>();

   public void downloadTexture(String id, String url, boolean force) {
      if (this.downloading.add(id) && !this.cache.containsKey(id)) {
         Shoreline.EXECUTOR.execute(() -> {
            HttpGet request = new HttpGet(url);

            try {
               CloseableHttpResponse response = this.client.execute(request);

               try {
                  InputStream stream = response.getEntity().getContent();
                  NativeImage image = NativeImage.read(stream);
                  ResourceLocation textureIdentifier = mc.getTextureManager().register(id, new DynamicTexture(image));
                  this.cache.put(id, textureIdentifier);
               } catch (Throwable var10) {
                  if (response != null) {
                     try {
                        response.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (response != null) {
                  response.close();
               }
            } catch (IOException var11) {
               var11.printStackTrace();
               if (force) {
                  this.downloading.remove(id);
               }
            }
         });
      }
   }

   public void removeTexture(String id) {
      ResourceLocation identifier = this.cache.get(id);
      if (identifier != null) {
         mc.getTextureManager().release(identifier);
         this.cache.remove(id);
      }
   }

   public ResourceLocation get(String id) {
      return this.cache.get(id);
   }

   public boolean exists(String id) {
      return this.cache.containsKey(id);
   }

   public boolean isDownloading(String id) {
      return this.downloading.contains(id);
   }
}
