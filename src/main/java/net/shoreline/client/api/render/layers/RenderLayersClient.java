package net.shoreline.client.api.render.layers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.LightmapStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorRenderPhase;
import net.shoreline.client.util.Globals;
import org.lwjgl.opengl.GL11;

public class RenderLayersClient implements Globals {
   public static final ResourceLocation SHULKER_BOXES_ATLAS_TEXTURE = ResourceLocation.parse("textures/atlas/shulker_boxes.png");
   public static final ResourceLocation CHEST_ATLAS_TEXTURE = ResourceLocation.parse("textures/atlas/chest.png");
   public static final RenderType GLINT = RenderType.create(
      "glint",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      256,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
         .setTextureState(new TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(new RenderLayersClient.DepthTest())
         .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
         .setTexturingState(RenderStateShard.GLINT_TEXTURING)
         .createCompositeState(false)
   );
   public static final Function<ResourceLocation, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_NO_OUTLINE_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .createCompositeState(false);
         return RenderType.create("entity_no_outline", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, true, multiPhaseParameters);
      }
   );
   public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
      (texture, affectsOutline) -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(affectsOutline);
         return RenderType.create("entity_translucent", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, true, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_SOLID_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("entity_solid", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, false, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL_2 = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderType.ITEM_ENTITY_TARGET)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
            .createCompositeState(true);
         return RenderType.create("item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, true, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, true, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("entity_cutout", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, false, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
      texture -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .createCompositeState(true);
         return RenderType.create("entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, false, multiPhaseParameters);
      }
   );
   public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
      (texture, affectsOutline) -> {
         CompositeState multiPhaseParameters = CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.NO_TRANSPARENCY)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setOverlayState(RenderType.OVERLAY)
            .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(affectsOutline);
         return RenderType.create("entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, true, false, multiPhaseParameters);
      }
   );
   public static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
      texture -> RenderType.create(
         "text",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         786432,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .createCompositeState(false)
      )
   );
   public static final RenderType TEXT_BACKGROUND = RenderType.create(
      "text_background",
      DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
      Mode.QUADS,
      1536,
      false,
      true,
      CompositeState.builder()
         .setShaderState(RenderType.RENDERTYPE_TEXT_BACKGROUND_SHADER)
         .setTextureState(RenderType.NO_TEXTURE)
         .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
         .setLightmapState(new RenderLayersClient.Lightmap())
         .createCompositeState(false)
   );
   public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(
      texture -> RenderType.create(
         "text_intensity",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         786432,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .createCompositeState(false)
      )
   );
   public static final Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
      texture -> RenderType.create(
         "text_polygon_offset",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         1536,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setLayeringState(RenderType.POLYGON_OFFSET_LAYERING)
            .createCompositeState(false)
      )
   );
   public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
      texture -> RenderType.create(
         "text_intensity_polygon_offset",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         1536,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setLayeringState(RenderType.POLYGON_OFFSET_LAYERING)
            .createCompositeState(false)
      )
   );
   public static final Function<ResourceLocation, RenderType> TEXT_SEE_THROUGH = Util.memoize(
      texture -> RenderType.create(
         "text_see_through",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         1536,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .createCompositeState(false)
      )
   );
   public static final RenderType TEXT_BACKGROUND_SEE_THROUGH = RenderType.create(
      "text_background_see_through",
      DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
      Mode.QUADS,
      1536,
      false,
      true,
      CompositeState.builder()
         .setShaderState(RenderType.RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER)
         .setTextureState(RenderType.NO_TEXTURE)
         .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
         .setLightmapState(new RenderLayersClient.Lightmap())
         .setDepthTestState(RenderType.NO_DEPTH_TEST)
         .setWriteMaskState(RenderType.COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
      texture -> RenderType.create(
         "text_intensity_see_through",
         DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
         Mode.QUADS,
         1536,
         false,
         true,
         CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(new RenderLayersClient.Lightmap())
            .setDepthTestState(RenderType.NO_DEPTH_TEST)
            .setWriteMaskState(RenderType.COLOR_WRITE)
            .createCompositeState(false)
      )
   );

   protected static class DepthTest extends DepthTestStateShard {
      public DepthTest() {
         super("depth_test", 519);
      }

      @Override
      public void setupRenderState() {
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDepthFunc(514);
      }

      @Override
      public void clearRenderState() {
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glDepthFunc(515);
         GL11.glDepthFunc(519);
      }
   }

   protected static class Lightmap extends LightmapStateShard {
      public Lightmap() {
         super(false);
         ((AccessorRenderPhase)this).hookSetBeginAction(() -> Managers.LIGHT_MAP.enable());
         ((AccessorRenderPhase)this).hookSetEndAction(() -> Managers.LIGHT_MAP.disable());
      }
   }
}
