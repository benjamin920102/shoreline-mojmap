package net.shoreline.client.impl.manager.client.cape;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.net.URL;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.impl.event.network.LoadCapeEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

public class CapeManager implements Globals {
   public CapeManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onLoadCape(LoadCapeEvent event) {
      this.loadPlayerCape(event.getGameProfile(), event.getTexture());
   }

   public void loadPlayerCape(GameProfile profile, CapeManager.CapeTexture texture) {
      Util.backgroundExecutor().execute(() -> {
         String uuid = profile.getId().toString();
         String url = String.format("http://s.optifine.net/capes/%s.png", profile.getName());

         try {
            URL optifineUrl = new URL(url);
            InputStream stream = optifineUrl.openStream();
            NativeImage cape = NativeImage.read(stream);
            NativeImage nativeImage = this.imageFromStream(cape);
            DynamicTexture t = new DynamicTexture(nativeImage);
            ResourceLocation identifier = mc.getTextureManager().register("of-capes-" + uuid, t);
            texture.callback(identifier);
            stream.close();
         } catch (Exception var11) {
         }
      });
   }

   private NativeImage imageFromStream(NativeImage image) {
      int imageWidth = 64;
      int imageHeight = 32;
      int imageSrcWidth = image.getWidth();
      int srcHeight = image.getHeight();

      for (int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
         imageWidth *= 2;
      }

      NativeImage img = new NativeImage(imageWidth, imageHeight, true);

      for (int x = 0; x < imageSrcWidth; x++) {
         for (int y = 0; y < srcHeight; y++) {
            img.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
         }
      }

      image.close();
      return img;
   }

   public interface CapeTexture {
      void callback(ResourceLocation var1);
   }
}
