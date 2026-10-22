package net.shoreline.client.api.render.layers;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.shoreline.client.impl.event.render.LightmapInitEvent;
import net.shoreline.client.impl.event.render.LightmapTickEvent;
import net.shoreline.client.impl.event.render.LightmapUpdateEvent;
import net.shoreline.client.util.Globals;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Vector3f;

public class LightmapManager implements AutoCloseable, Globals {
   private DynamicTexture texture;
   private NativeImage image;
   private ResourceLocation textureIdentifier;
   private boolean dirty;
   private float flickerIntensity;
   private GameRenderer renderer;

   public LightmapManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onLightmapInit(LightmapInitEvent event) {
      this.renderer = mc.gameRenderer;
      this.texture = new DynamicTexture(16, 16, false);
      this.textureIdentifier = mc.getTextureManager().register("shoreline_light_map", this.texture);
      this.image = this.texture.getPixels();

      for (int i = 0; i < 16; i++) {
         for (int j = 0; j < 16; j++) {
            this.image.setPixelRGBA(j, i, -1);
         }
      }

      this.texture.upload();
   }

   @EventListener
   public void onLightmapTick(LightmapTickEvent event) {
      this.flickerIntensity = this.flickerIntensity + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
      this.flickerIntensity *= 0.9F;
      this.dirty = true;
   }

   @EventListener
   public void onLightmapUpdate(LightmapUpdateEvent event) {
      if (this.renderer != null && this.texture != null && this.image != null) {
         if (this.dirty) {
            this.dirty = false;
            ClientLevel clientWorld = mc.level;
            if (clientWorld != null) {
               float f = clientWorld.getSkyDarken(1.0F);
               float g;
               if (clientWorld.getSkyFlashTime() > 0) {
                  g = 1.0F;
               } else {
                  g = f * 0.95F + 0.05F;
               }

               float h = mc.options.darknessEffectScale().get().floatValue();
               float i = this.getDarknessFactor(event.getTickDelta()) * h;
               float j = this.getDarkness(mc.player, i, event.getTickDelta()) * h;
               float k = mc.player.getWaterVision();
               float l;
               if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                  l = GameRenderer.getNightVisionScale(mc.player, event.getTickDelta());
               } else if (k > 0.0F && mc.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                  l = k;
               } else {
                  l = 0.0F;
               }

               Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
               float m = this.flickerIntensity + 1.5F;
               Vector3f vector3f2 = new Vector3f();

               for (int n = 0; n < 16; n++) {
                  for (int o = 0; o < 16; o++) {
                     float p = getBrightness(clientWorld.dimensionType(), n) * g;
                     float q = getBrightness(clientWorld.dimensionType(), o) * m;
                     float s = q * ((q * 0.6F + 0.4F) * 0.6F + 0.4F);
                     float t = q * (q * q * 0.6F + 0.4F);
                     vector3f2.set(q, s, t);
                     boolean bl = clientWorld.effects().forceBrightLightmap();
                     if (bl) {
                        vector3f2.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
                        clamp(vector3f2);
                     } else {
                        Vector3f vector3f3 = new Vector3f(vector3f).mul(p);
                        vector3f2.add(vector3f3);
                        vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                        if (this.renderer.getDarkenWorldAmount(event.getTickDelta()) > 0.0F) {
                           float u = this.renderer.getDarkenWorldAmount(event.getTickDelta());
                           Vector3f vector3f4 = new Vector3f(vector3f2).mul(0.7F, 0.6F, 0.6F);
                           vector3f2.lerp(vector3f4, u);
                        }
                     }

                     if (l > 0.0F) {
                        float v = Math.max(vector3f2.x(), Math.max(vector3f2.y(), vector3f2.z()));
                        if (v < 1.0F) {
                           float u = 1.0F / v;
                           Vector3f vector3f4 = new Vector3f(vector3f2).mul(u);
                           vector3f2.lerp(vector3f4, l);
                        }
                     }

                     if (!bl) {
                        if (j > 0.0F) {
                           vector3f2.add(-j, -j, -j);
                        }

                        clamp(vector3f2);
                     }

                     float v = mc.options.gamma().get().floatValue();
                     Vector3f vector3f5 = new Vector3f(this.easeOutQuart(vector3f2.x), this.easeOutQuart(vector3f2.y), this.easeOutQuart(vector3f2.z));
                     vector3f2.lerp(vector3f5, Math.max(0.0F, v - i));
                     vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                     clamp(vector3f2);
                     vector3f2.mul(255.0F);
                     int x = (int)vector3f2.x();
                     int y = (int)vector3f2.y();
                     int z = (int)vector3f2.z();
                     this.image.setPixelRGBA(o, n, 0xFF000000 | z << 16 | y << 8 | x);
                  }
               }

               this.texture.upload();
            }
         }
      }
   }

   @Override
   public void close() {
      this.texture.close();
   }

   public void disable() {
      RenderSystem.setShaderTexture(2, 0);
   }

   public void enable() {
      if (this.textureIdentifier != null) {
         RenderSystem.setShaderTexture(2, this.textureIdentifier);
         mc.getTextureManager().bindForSetup(this.textureIdentifier);
         RenderSystem.texParameter(3553, 10241, 9729);
         RenderSystem.texParameter(3553, 10240, 9729);
      }
   }

   private float getDarknessFactor(float delta) {
      MobEffectInstance statusEffectInstance = mc.player.getEffect(MobEffects.DARKNESS);
      return statusEffectInstance != null ? statusEffectInstance.getBlendFactor(mc.player, delta) : 0.0F;
   }

   private float getDarkness(LivingEntity entity, float factor, float delta) {
      float f = 0.45F * factor;
      return Math.max(0.0F, Mth.cos((entity.tickCount - delta) * (float) Math.PI * 0.025F) * f);
   }

   private static void clamp(Vector3f vec) {
      vec.set(Mth.clamp(vec.x, 0.0F, 1.0F), Mth.clamp(vec.y, 0.0F, 1.0F), Mth.clamp(vec.z, 0.0F, 1.0F));
   }

   private float easeOutQuart(float x) {
      float f = 1.0F - x;
      return 1.0F - f * f * f * f;
   }

   public static float getBrightness(DimensionType type, int lightLevel) {
      float f = lightLevel / 15.0F;
      float g = f / (4.0F - 3.0F * f);
      return Mth.lerp(type.ambientLight(), g, 1.0F);
   }

   public static int pack(int block, int sky) {
      return block << 4 | sky << 20;
   }
}
