package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.CommonColors;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.api.render.layers.RenderLayersClient;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.render.entity.RenderLabelEvent;
import net.shoreline.client.impl.event.world.PlaySoundEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.client.FontModule;
import net.shoreline.client.impl.module.client.SocialsModule;
import net.shoreline.client.init.Fonts;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorItemRenderer;
import net.shoreline.client.mixin.accessor.AccessorTextRenderer;
import net.shoreline.client.util.entity.FakePlayerEntity;
import net.shoreline.client.util.render.ColorUtil;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author linus
 * @since 1.0
 */
public class NametagsModule extends ToggleModule
{
    private static NametagsModule INSTANCE;

    Config<Boolean> armorConfig = register(new BooleanConfig("Armor", "Displays the player's armor", true));
    Config<Boolean> itemsConfig = register(new BooleanConfig("Items", "Displays the player's held items", true));
    Config<Boolean> enchantmentsConfig = register(new BooleanConfig("Enchantments", "Displays a list of the item's enchantments", true));
    Config<Boolean> durabilityConfig = register(new BooleanConfig("Durability", "Displays item durability", true));
    Config<Boolean> itemNameConfig = register(new BooleanConfig("ItemName", "Displays the player's current held item name", false));
    Config<Boolean> entityIdConfig = register(new BooleanConfig("EntityId", "Displays the player's entity id", false));
    Config<Boolean> gamemodeConfig = register(new BooleanConfig("Gamemode", "Displays the player's gamemode", false));
    // Pooron check
    Config<Boolean> pingConfig = register(new BooleanConfig("Ping", "Displays the player's server connection ping", true));
    Config<Boolean> healthConfig = register(new BooleanConfig("Health", "Displays the player's current health", true));
    Config<Boolean> totemsConfig = register(new BooleanConfig("Totems", "Displays the player's popped totem count", false));
    Config<Float> scalingConfig = register(new NumberConfig<>("Scaling", "The nametag label scale", 0.001f, 0.003f, 0.01f));
    Config<Boolean> invisiblesConfig = register(new BooleanConfig("Invisibles", "Renders nametags on invisible players", true));
    Config<Boolean> backgroundConfig = register(new BooleanConfig("Background", "Renders a background behind the nametag", true));
    Config<Boolean> borderedConfig = register(new BooleanConfig("Border", "Renders a border around the nametag", false));
    Config<Boolean> tamedConfig = register(new BooleanConfig("MobOwner", "Renders nametags on tamed mobs", false));
    Config<Boolean> pearlsConfig = register(new BooleanConfig("Pearls", "Renders nametags on thrown ender pearls", false));
    Config<Boolean> droppedItemsConfig = register(new BooleanConfig("DroppedItems", "Renders nametags on dropped items", false));
    Config<Boolean> soundsConfig = register(new BooleanConfig("Sounds", "Renders nametags on sounds", false));

    private final Map<SoundRender, Long> sounds = new HashMap<>();

    public NametagsModule()
    {
        super("Nametags", "Renders info on player nametags", ModuleCategory.RENDER);
        INSTANCE = this;
    }

    public static NametagsModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        sounds.clear();
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (mc.gameRenderer == null || mc.getCameraEntity() == null)
        {
            return;
        }
        RenderBuffers.preRender();
        Vec3 interpolate = Interpolation.getRenderPosition(mc.getCameraEntity(), mc.getTimer().getGameTimeDeltaPartialTick(true));
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = camera.getPosition();
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (!RenderManager.isFrustumVisible(entity.getBoundingBox()))
            {
                continue;
            }

            if (entity instanceof Player player)
            {
                if (player == mc.player && !FreecamModule.getInstance().isEnabled())
                {
                    continue;
                }
                if (!player.isAlive() || !invisiblesConfig.getValue() && player.isInvisible())
                {
                    continue;
                }
                String info = getNametagInfo(player);
                Vec3 pinterpolate = Interpolation.getRenderPosition(player, mc.getTimer().getGameTimeDeltaPartialTick(true));
                double rx = player.getX() - pinterpolate.x();
                double ry = player.getY() - pinterpolate.y();
                double rz = player.getZ() - pinterpolate.z();
                float w1 = FontModule.getInstance().isEnabled() ? Fonts.CLIENT_UNSCALED.getStringWidth(info) : mc.font.width(info);
                int width = (int)(w1);
                float hwidth = width / 2.0f;
                double dx = (pos.x() - interpolate.x()) - rx;
                double dy = (pos.y() - interpolate.y()) - ry;
                double dz = (pos.z() - interpolate.z()) - rz;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 4096.0)
                {
                    continue;
                }
                float scaling = 0.0018f + scalingConfig.getValue() * (float) dist;
                if (dist <= 8.0)
                {
                    scaling = 0.0245f;
                }
                renderInfo(info, hwidth, player, rx, ry, rz, camera, scaling);
            }
            else if (entity instanceof AbstractHorse tameable && tameable.getOwnerUUID() != null && tamedConfig.getValue())
            {
                String lookup = Managers.LOOKUP.getNameFromUUID(tameable.getOwnerUUID());
                if (lookup != null)
                {
                    Vec3 tamePos = Interpolation.getRenderPosition(entity, mc.getTimer().getGameTimeDeltaPartialTick(true));
                    double rx = entity.getX() - tamePos.x();
                    double ry = (entity.getY() + entity.getBbHeight() + 0.43f) - tamePos.y();
                    double rz = entity.getZ() - tamePos.z();
                    RenderManager.renderSign("Owner: " + lookup, rx, ry, rz, -1);
                }
            }
            else if (entity instanceof TamableAnimal tameable && tameable.getOwnerUUID() != null && tamedConfig.getValue())
            {
                String lookup = Managers.LOOKUP.getNameFromUUID(tameable.getOwnerUUID());
                if (lookup != null)
                {
                    Vec3 tamePos = Interpolation.getRenderPosition(entity, mc.getTimer().getGameTimeDeltaPartialTick(true));
                    double rx = entity.getX() - tamePos.x();
                    double ry = (entity.getY() + entity.getBbHeight() + 0.43f) - tamePos.y();
                    double rz = entity.getZ() - tamePos.z();
                    RenderManager.renderSign("Owner: " + lookup, rx, ry, rz, -1);
                }
            }
            else if (entity instanceof ItemEntity itemEntity && droppedItemsConfig.getValue())
            {
                Vec3 itemPos = Interpolation.getRenderPosition(itemEntity, mc.getTimer().getGameTimeDeltaPartialTick(true));
                double rx = itemEntity.getX() - itemPos.x();
                double ry = itemEntity.getY() - itemPos.y();
                double rz = itemEntity.getZ() - itemPos.z();
                ItemStack stack = itemEntity.getItem();
                String stackNametag = stack.getHoverName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
                RenderManager.renderSign(stackNametag, rx, ry, rz, -1);
            }
            else if (entity instanceof ThrownEnderpearl pearlEntity && pearlsConfig.getValue())
            {
                if (pearlEntity.getOwner() == null)
                {
                    continue;
                }
                Vec3 itemPos = Interpolation.getRenderPosition(pearlEntity, mc.getTimer().getGameTimeDeltaPartialTick(true));
                double rx = pearlEntity.getX() - itemPos.x();
                double ry = pearlEntity.getY() - itemPos.y();
                double rz = pearlEntity.getZ() - itemPos.z();
                Entity thrower = pearlEntity.getOwner();
                if (thrower instanceof Player player)
                {
                    RenderManager.renderSign(player.getName().getString(), rx, ry, rz, getNametagColor(player));
                }
            }
        }
        if (soundsConfig.getValue()) {
            sounds.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 1000);
            for (Map.Entry<SoundRender, Long> entry : sounds.entrySet()) {
                SoundEvent soundEvent = entry.getKey().soundEvent();
                Vec3 renderPos = entry.getKey().pos();
                String soundName = soundEvent.getLocation().getPath()
                        .replace('_', ' ')
                        .replace('.', ' ');
                soundName = Arrays.stream(soundName.split(" "))
                        .filter(word -> !word.isEmpty())
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));

                RenderManager.renderSign(soundName, renderPos.x, renderPos.y, renderPos.z, -1);
            }
        }

        RenderBuffers.postRender();
    }

    @EventListener
    public void onRenderLabel(RenderLabelEvent event)
    {
        if (event.getEntity() instanceof Player && event.getEntity() != mc.player)
        {
            event.cancel();
        }
    }

    @EventListener
    public void onPlaySound(PlaySoundEvent event)
    {
        sounds.put(new SoundRender(event.getPos(), event.getSoundEvent()), System.currentTimeMillis());
    }

    private record SoundRender(Vec3 pos, SoundEvent soundEvent) {}

    private void renderInfo(String info, float width, Player entity, double x, double y, double z, Camera camera, float scaling) {
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        final Vec3 pos = camera.getPosition();
        PoseStack matrices = new PoseStack();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));
        matrices.translate(x - pos.x(),
                y + (double) entity.getBbHeight() + (entity.isShiftKeyDown() ? 0.4f : 0.43f) - pos.y(),
                z - pos.z());
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.scale(-scaling, -scaling, 1.0f);

        if (backgroundConfig.getValue())
        {
            RenderManager.rect(matrices, -width - 1.0f, -1.0f, width * 2.0f + 2.5f,
                    mc.font.lineHeight + 1.0f, 0.0, 0x55000400);
        }

        if (borderedConfig.getValue())
        {
            RenderManager.borderedRectLine(matrices, -width - 1.0f, -1.0f, width * 2.0f + 2.5f,
                    mc.font.lineHeight + 1.0f, entity != mc.player && Managers.SOCIAL.isFriend(entity.getDisplayName().getString()) ? SocialsModule.getInstance().getFriendRGB() : ColorsModule.getInstance().getRGB());
        }

        int color = getNametagColor(entity);
        renderItems(matrices, entity);

        drawText(matrices, info, -width, 0.0f, color);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private void drawText(PoseStack matrices, String text, float x, float y, int color)
    {
        if (FontModule.getInstance().isEnabled())
        {
            Fonts.CLIENT_UNSCALED.drawStringWithShadow(matrices, text, x, y + 1.0f, color);
        }
        else
        {
            MultiBufferSource.BufferSource vertexConsumers = mc.renderBuffers().bufferSource();
            ((AccessorTextRenderer) mc.font).hookDrawLayer(text, x, y, Font.adjustColor(color), true,
                    matrices.last().pose(), vertexConsumers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.endBatch();

            ((AccessorTextRenderer) mc.font).hookDrawLayer(text, x, y, Font.adjustColor(color), false,
                    matrices.last().pose(), vertexConsumers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.endBatch();
        }
    }

    private void renderItems(PoseStack matrixStack, Player player)
    {
        List<ItemStack> displayItems = new CopyOnWriteArrayList<>();
        if (!player.getOffhandItem().isEmpty())
        {
            displayItems.add(player.getOffhandItem());
        }
        player.getInventory().armor.forEach(armorStack ->
        {
            if (!armorStack.isEmpty())
            {
                displayItems.add(armorStack);
            }
        });
        if (!player.getMainHandItem().isEmpty())
        {
            displayItems.add(player.getMainHandItem());
        }
        Collections.reverse(displayItems);
        float n10 = 0;
        int n11 = 0;
        boolean gapple = true;
        for (ItemStack stack : displayItems)
        {
            n10 -= gapple ? 9 : 8;
            gapple = false;
            if (stack.getEnchantments().keySet().size() > n11)
            {
                n11 = stack.getEnchantments().keySet().size();
            }
        }
        float m2 = enchantOffset(n11);
        for (ItemStack stack : displayItems)
        {
            boolean armor = stack.getItem() instanceof ArmorItem;
            if (armorConfig.getValue() && armor || itemsConfig.getValue() && !armor)
            {
                float y = !armor && !enchantmentsConfig.getValue() ? -18.5f : m2;
                matrixStack.pushPose();
                matrixStack.translate(n10, y, 0.0f);
                matrixStack.translate(8.0f, 8.0f, 0.0f);
                matrixStack.scale(16.0f, 16.0f, 0.0f);
                matrixStack.mulPose(new Matrix4f().scaling(1.0f, -1.0f, 0.0001f));
                renderItem(stack, ItemDisplayContext.GUI, 0xf, OverlayTexture.NO_OVERLAY,
                        matrixStack, mc.renderBuffers().bufferSource(), mc.level, 0);
                mc.renderBuffers().bufferSource().endBatch();
                matrixStack.popPose();
                renderItemOverlay(matrixStack, stack, (int) n10, (int) y);

                matrixStack.scale(0.5f, 0.5f, 0.5f);
                if (stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)
                {
                    float y2 = (m2 + (armorConfig.getValue() ? 1.0f : -13.5f)) * 2;
                    drawText(matrixStack, "God", (n10 + 2.0f) * 2, y2, 0xffc34e41);
                }
                else if (enchantmentsConfig.getValue())
                {
                    renderEnchants(matrixStack, stack, n10 + 2.0f, m2);
                }
                matrixStack.scale(2.0f, 2.0f, 2.0f);
            }
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            if (durabilityConfig.getValue())
            {
                renderDurability(matrixStack, stack, n10 + 2.0f, m2 - 4.5f);
            }
            matrixStack.scale(2.0f, 2.0f, 2.0f);
            n10 += 16;
            // int n4 = (n11 > 4) ? ((n11 - 4) * 8 / 2) : 0;
            // mc.getItemRenderer().renderInGui(matrixStack, mc.textRenderer, stack, n10, m2);
        }
        //
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty())
        {
            return;
        }
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        if (itemNameConfig.getValue())
        {
            renderItemName(matrixStack, heldItem, 0, durabilityConfig.getValue() ? m2 - 10.0f : m2 - 5.5f);
        }
        matrixStack.scale(2.0f, 2.0f, 2.0f);
    }

    private void renderItem(ItemStack stack, ItemDisplayContext renderMode, int light, int overlay, PoseStack matrices,
                            MultiBufferSource vertexConsumers, Level world, int seed)
    {
        BakedModel bakedModel = mc.getItemRenderer().getModel(stack, world, null, seed);
        if (stack.isEmpty())
        {
            return;
        }
        boolean bl = renderMode == ItemDisplayContext.GUI || renderMode == ItemDisplayContext.GROUND || renderMode == ItemDisplayContext.FIXED;
        if (bl)
        {
            if (stack.is(Items.TRIDENT))
            {
                bakedModel = mc.getItemRenderer().getItemModelShaper().getModelManager().getModel(ModelResourceLocation.vanilla("trident", "inventory"));
            }
            else if (stack.is(Items.SPYGLASS))
            {
                bakedModel = mc.getItemRenderer().getItemModelShaper().getModelManager().getModel(ModelResourceLocation.vanilla("spyglass", "inventory"));
            }
        }
        bakedModel.getTransforms().getTransform(renderMode).apply(false, matrices);
        matrices.translate(-0.5f, -0.5f, 0.0f);
        if (bakedModel.isCustomRenderer() || stack.is(Items.TRIDENT) && !bl)
        {
            ((AccessorItemRenderer) mc.getItemRenderer()).hookGetBuiltinModelItemRenderer().renderByItem(
                    stack, renderMode, matrices, vertexConsumers, light, overlay);
        }
        else
        {
            renderBakedItemModel(bakedModel, stack, light, overlay, matrices,
                    getItemGlintConsumer(vertexConsumers, RenderLayersClient.ENTITY_TRANSLUCENT_CULL.apply(TextureAtlas.LOCATION_BLOCKS), stack.hasFoil()));
        }
    }

    private void renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, PoseStack matrices, VertexConsumer vertices)
    {
        RandomSource random = RandomSource.create();
        long l = 42L;
        for (Direction direction : Direction.values())
        {
            random.setSeed(42L);
            renderBakedItemQuads(matrices, vertices, model.getQuads(null, direction, random), stack, light, overlay);
        }
        random.setSeed(42L);
        renderBakedItemQuads(matrices, vertices, model.getQuads(null, null, random), stack, light, overlay);
    }

    private void renderBakedItemQuads(PoseStack matrices, VertexConsumer vertices, List<BakedQuad> quads, ItemStack stack, int light, int overlay)
    {
        boolean bl = !stack.isEmpty();
        PoseStack.Pose entry = matrices.last();
        for (BakedQuad bakedQuad : quads)
        {
            int i = -1;
            if (bl && bakedQuad.isTinted())
            {
                i = ((AccessorItemRenderer) mc.getItemRenderer()).hookGetItemColors().getColor(stack, bakedQuad.getTintIndex());
            }
            float f = (float)(i >> 16 & 0xFF) / 255.0f;
            float g = (float)(i >> 8 & 0xFF) / 255.0f;
            float h = (float)(i & 0xFF) / 255.0f;
            quad(vertices, entry, bakedQuad, f, g, h, light, overlay);
        }
    }

    public void quad(VertexConsumer vertexConsumer, PoseStack.Pose matrixEntry, BakedQuad quad, float red, float green, float blue, int light, int overlay)
    {
        float[] fs = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
        int[] is = new int[] {light, light, light, light};
        int[] js = quad.getVertices();
        Matrix4f matrix4f = matrixEntry.pose();
        // Vec3i vec3i = quad.getFace().getVector();
        // Vector3f vector3f = matrixEntry.getNormalMatrix().transform(new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ()));
        int i = 8;
        int j = js.length / 8;
        try (MemoryStack memoryStack = MemoryStack.stackPush())
        {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            for (int k = 0; k < j; ++k)
            {
                float q;
                float p;
                float o;
                float n;
                float m;
                intBuffer.clear();
                intBuffer.put(js, k * 8, 8);
                float f = byteBuffer.getFloat(0);
                float g = byteBuffer.getFloat(4);
                float h = byteBuffer.getFloat(8);
                o = fs[k] * red;
                p = fs[k] * green;
                q = fs[k] * blue;
                int r = is[k];
                m = byteBuffer.getFloat(16);
                n = byteBuffer.getFloat(20);
                Vector4f vector4f = matrix4f.transform(new Vector4f(f, g, h, 1.0f));
                vertexConsumer.addVertex(vector4f.x(), vector4f.y(), vector4f.z());
                vertexConsumer.setColor(new Color(o, p, q).getRGB());
                vertexConsumer.setUv(m, n);
                vertexConsumer.setOverlay(overlay);
                vertexConsumer.setLight(r);
                vertexConsumer.setNormal(1.0f, 1.0f, 1.0f);
            }
        }
    }

    public static VertexConsumer getItemGlintConsumer(MultiBufferSource vertexConsumers,
                                                      RenderType layer, boolean glint)
    {
        if (glint)
        {
            return VertexMultiConsumer.create(vertexConsumers.getBuffer(RenderLayersClient.GLINT), vertexConsumers.getBuffer(layer));
        }
        return vertexConsumers.getBuffer(layer);
    }

    private void renderItemOverlay(PoseStack matrixStack, ItemStack stack, int x, int y)
    {
        matrixStack.pushPose();
        if (stack.getCount() != 1)
        {
            String string = String.valueOf(stack.getCount());
            // this.matrices.translate(0.0f, 0.0f, 200.0f);
            MultiBufferSource.BufferSource vertexConsumers = mc.renderBuffers().bufferSource();
            ((AccessorTextRenderer) mc.font).hookDrawLayer(string, x + 17 - mc.font.width(string), y + 9.0f, Font.adjustColor(-1), true,
                    matrixStack.last().pose(), vertexConsumers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.endBatch();

            ((AccessorTextRenderer) mc.font).hookDrawLayer(string, x + 17 - mc.font.width(string), y + 9.0f, Font.adjustColor(-1), false,
                    matrixStack.last().pose(), vertexConsumers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            vertexConsumers.endBatch();
        }
        if (stack.isBarVisible())
        {
            int i = (int) Math.clamp(stack.getBarWidth() * 0.923076923, 0, 12);
            int j = stack.getBarColor();
            int k = x + 3;
            int l = y + 13;
            RenderManager.rect(matrixStack, k, l, 12, 1, CommonColors.BLACK);
            RenderManager.rect(matrixStack, k, l, i, 1, j | CommonColors.BLACK);
        }
        matrixStack.popPose();
    }

    private void renderDurability(PoseStack matrixStack, ItemStack itemStack, float x, float y)
    {
        if (!itemStack.isDamageableItem())
        {
            return;
        }
        int n = itemStack.getMaxDamage();
        int n2 = itemStack.getDamageValue();
        int durability = (int) ((n - n2) / ((float) n) * 100.0f);
        drawText(matrixStack, durability + "%", x * 2, y * 2,
                ColorUtil.hslToColor((float) (n - n2) / (float) n * 120.0f, 100.0f, 50.0f, 1.0f).getRGB());
    }

    private void renderEnchants(PoseStack matrixStack, ItemStack itemStack, float x, float y)
    {

        if (!itemStack.isEnchanted())
        {
            return;
        }
        Set<Object2IntMap.Entry<Holder<Enchantment>>> enchants = EnchantmentHelper.getEnchantmentsForCrafting(itemStack).entrySet();

        float n2 = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchants)
        {
            int lvl = e.getIntValue();
            StringBuilder enchantString = new StringBuilder();
            String translatedName = Enchantment.getFullname(e.getKey(), lvl).getString();
            if (translatedName.contains("Vanish"))
            {
                enchantString.append("Van");
            }
            else if (translatedName.contains("Bind"))
            {
                enchantString.append("Bind");
            }
            else
            {
                int maxLen = lvl > 1 ? 2 : 3;
                if (translatedName.length() > maxLen)
                {
                    translatedName = translatedName.substring(0, maxLen);
                }
                enchantString.append(translatedName);
                enchantString.append(lvl);
            }
            drawText(matrixStack, enchantString.toString(), x * 2, (y + n2) * 2, -1);
            n2 += 4.5f;
        }
    }

    private float enchantOffset(final int n)
    {
        if (!enchantmentsConfig.getValue() || n <= 3)
        {
            return armorConfig.getValue() ? -18.0f : -3.5f;
        }
        float n2 = -14.0f;
        n2 -= (n - 3) * 4.5f;
        return n2;
    }

    private void renderItemName(PoseStack matrixStack, ItemStack itemStack, float x, float y)
    {
        String itemName = itemStack.getHoverName().getString();
        float width = mc.font.width(itemName) / 4.0f;
        drawText(matrixStack, itemName, (x - width) * 2, y * 2, -1);
    }

    private String getNametagInfo(Player player)
    {
        final StringBuilder info = new StringBuilder(player.getName().getString());
        info.append(" ");
        if (entityIdConfig.getValue())
        {
            info.append("ID: ");
            info.append(player.getId());
            info.append(" ");
        }
        if (gamemodeConfig.getValue())
        {
            if (player.isCreative())
            {
                info.append("[C] ");
            }
            else if (player.isSpectator())
            {
                info.append("[I] ");
            }
            else
            {
                info.append("[S] ");
            }
        }
        if (pingConfig.getValue() && mc.getConnection() != null)
        {
            PlayerInfo playerEntry = mc.getConnection().getPlayerInfo(player.getGameProfile().getId());
            if (playerEntry != null)
            {
                info.append(playerEntry.getLatency());
                info.append("ms ");
            }
        }
        if (healthConfig.getValue())
        {
            double health = player.getHealth() + player.getAbsorptionAmount();

            ChatFormatting hcolor;
            if (health > 18)
            {
                hcolor = ChatFormatting.GREEN;
            }
            else if (health > 16)
            {
                hcolor = ChatFormatting.DARK_GREEN;
            }
            else if (health > 12)
            {
                hcolor = ChatFormatting.YELLOW;
            }
            else if (health > 8)
            {
                hcolor = ChatFormatting.GOLD;
            }
            else if (health > 4)
            {
                hcolor = ChatFormatting.RED;
            }
            else
            {
                hcolor = ChatFormatting.DARK_RED;
            }
            BigDecimal bigDecimal = new BigDecimal(health);
            bigDecimal = bigDecimal.setScale(1, RoundingMode.HALF_UP);
            info.append(hcolor);
            info.append(bigDecimal.doubleValue());
            info.append(" ");
        }
        if (totemsConfig.getValue() && player != mc.player)
        {
            int totems = Managers.TOTEM.getTotems(player);
            if (totems > 0)
            {
                ChatFormatting pcolor = ChatFormatting.GREEN;

                if (totems > 1)
                {
                    pcolor = ChatFormatting.DARK_GREEN;
                }
                if (totems > 2)
                {
                    pcolor = ChatFormatting.YELLOW;
                }
                if (totems > 3)
                {
                    pcolor = ChatFormatting.GOLD;
                }
                if (totems > 4)
                {
                    pcolor = ChatFormatting.RED;
                }
                if (totems > 5)
                {
                    pcolor = ChatFormatting.DARK_RED;
                }
                info.append(pcolor);
                info.append(-totems);
                info.append(" ");
            }
        }
        return info.toString().trim();
    }

    private int getNametagColor(Player player)
    {
        if (player == mc.player)
        {
            return ColorsModule.getInstance().getRGB(255);
        }
        if (Managers.SOCIAL.isFriend(player.getName()))
        {
            return SocialsModule.getInstance().getFriendRGB();
        }
        if (player.isInvisible())
        {
            return 0xffff2500;
        }
        // fakeplayer
        if (player instanceof FakePlayerEntity)
        {
            return 0xffef0147;
        }
        if (player.isShiftKeyDown())
        {
            return 0xffff9900;
        }
        return 0xffffffff;
    }

    public float getScaling()
    {
        return scalingConfig.getValue();
    }
}
