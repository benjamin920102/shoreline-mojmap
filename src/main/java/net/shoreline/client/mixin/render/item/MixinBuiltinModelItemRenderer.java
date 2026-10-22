package net.shoreline.client.mixin.render.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock.Type;
import net.minecraft.world.level.block.SkullBlock.Types;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BlockEntityWithoutLevelRenderer.class})
public class MixinBuiltinModelItemRenderer {
   @Unique
   private static final Map<Type, ResourceLocation> TEXTURES = Util.make(Maps.newHashMap(), map -> {
      map.put(Types.SKELETON, ResourceLocation.parse("textures/entity/skeleton/skeleton.png"));
      map.put(Types.WITHER_SKELETON, ResourceLocation.parse("textures/entity/skeleton/wither_skeleton.png"));
      map.put(Types.ZOMBIE, ResourceLocation.parse("textures/entity/zombie/zombie.png"));
      map.put(Types.CREEPER, ResourceLocation.parse("textures/entity/creeper/creeper.png"));
      map.put(Types.DRAGON, ResourceLocation.parse("textures/entity/enderdragon/dragon.png"));
      map.put(Types.PIGLIN, ResourceLocation.parse("textures/entity/piglin/piglin.png"));
      map.put(Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
   });

   @Redirect(
      method = {"renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;getRenderType(Lnet/minecraft/world/level/block/SkullBlock$Type;Lnet/minecraft/world/item/component/ResolvableProfile;)Lnet/minecraft/client/renderer/RenderType;"
      )
   )
   public RenderType hookGetRenderLayer(Type type, ResolvableProfile profile) {
      ResourceLocation identifier = TEXTURES.get(type);
      if (type == Types.PLAYER && profile != null) {
         SkinManager playerSkinProvider = Minecraft.getInstance().getSkinManager();
         return RenderLayersClient.ENTITY_TRANSLUCENT.apply(playerSkinProvider.getInsecureSkin(profile.gameProfile()).texture(), true);
      } else {
         return RenderLayersClient.ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(identifier, true);
      }
   }
}
