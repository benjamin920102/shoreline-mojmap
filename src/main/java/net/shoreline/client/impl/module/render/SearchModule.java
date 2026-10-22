package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.math.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BlockListConfig;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.world.LoadChunkBlockEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.impl.event.world.SetBlockStateEvent;
import net.shoreline.client.impl.event.world.UnloadChunkBlocksEvent;
import net.shoreline.client.mixin.accessor.AccessorCamera;
import net.shoreline.client.util.render.RenderUtil;
import net.shoreline.client.util.world.BlockUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author linus
 * @since 1.0
 */
public class SearchModule extends ToggleModule
{
    Config<List<Block>> blocksConfig = register(new BlockListConfig<>("Blocks", "Valid search blocks",
            Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.DISPENSER,
            Blocks.DROPPER, Blocks.SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.SPAWNER, Blocks.END_PORTAL_FRAME));
    Config<Boolean> tracersConfig = register(new BooleanConfig("Tracers", "Draws tracers to highlighted blocks", false));
    Config<Float> widthConfig = register(new NumberConfig<>("Width", "The line width of the tracer", 1.0f, 1.0f, 5.0f, () -> tracersConfig.getValue()));
    Config<Boolean> fillConfig = register(new BooleanConfig("Fill", "Fills the render", true));
    Config<Boolean> softReloadConfig = register(new BooleanConfig("SoftReload", "Reloads without clearing the renders", false));

    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();
    private boolean warnedLag;

    public SearchModule()
    {
        super("Search", "Highlights specified blocks in the world", ModuleCategory.RENDER);
    }

    @Override
    public void onEnable()
    {
        if (mc.level == null || mc.getConnection() == null)
        {
            return;
        }
        // Big hack
        for (LevelChunk chunk : BlockUtil.loadedChunks())
        {
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();

            for (int y = chunk.getMinBuildHeight(); y < chunk.getHeight(); y++)
            {
                for (int x1 = startX; x1 < startX + 16; x1++)
                {
                    for (int z1 = startZ; z1 < startZ + 16; z1++)
                    {
                        BlockPos pos = new BlockPos(x1, y, z1);
                        BlockState blockState = chunk.getBlockState(pos);
                        if (!blockState.isAir() && isSearchBlock(blockState) && !blocks.containsKey(pos))
                        {
                            blocks.put(pos, blockState);
                        }
                    }
                }
            }
        }
        RenderUtil.reloadRenders(softReloadConfig.getValue());
        if (!warnedLag)
        {
            sendModuleMessage(ChatFormatting.RED + "This module may cause lag when loading new chunks");
            warnedLag = true;
        }
    }

    @Override
    public void onDisable()
    {
        blocks.clear();
    }

    @EventListener
    public void onGameJoin(GameJoinEvent event)
    {
        blocks.clear();
    }

    @EventListener
    public void onChangeDimension(LoadWorldEvent event)
    {
        blocks.clear();
    }

    @EventListener
    public void onLoadChunk(LoadChunkBlockEvent event)
    {
        BlockState blockState = event.getState();
        if (!blockState.isAir() && isSearchBlock(blockState) && !blocks.containsKey(event.getPos()))
        {
            blocks.put(event.getPos(), blockState);
        }
    }

    @EventListener
    public void onUnloadChunkBlocks(UnloadChunkBlocksEvent event)
    {
        blocks.entrySet().removeIf(e -> event.contains(e.getKey()));
    }

    @EventListener
    public void onSetBlockState(SetBlockStateEvent event)
    {
        if (isSearchBlock(event.getState()))
        {
            if (!blocks.containsKey(event.getPos()))
            {
                blocks.put(event.getPos(), event.getState());
            }
        }
        else
        {
            if (blocks.containsKey(event.getPos()))
            {
                blocks.remove(event.getPos());
            }
        }
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event)
    {
        if (event.getConfig() == blocksConfig && event.getStage() == StageEvent.EventStage.POST)
        {
            disable();
            enable();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (!(mc.getCameraEntity() instanceof Player playerEntity))
        {
            return;
        }
        if (tracersConfig.getValue())
        {
            PoseStack matrixStack = new PoseStack();
            double d = mc.options.fov().get();
            matrixStack.mulPose(mc.gameRenderer.getProjectionMatrix(d));
            Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
            RenderSystem.setProjectionMatrix(matrixStack.last().pose(), VertexSorting.DISTANCE_TO_ORIGIN);
            RenderBuffers.preRender();
            Vec3 playerPos = Interpolation.getRenderPosition(playerEntity, event.getTickDelta());
            // interp on camera y pos
            Camera camera = mc.gameRenderer.getMainCamera();
            double eyeHeight = Mth.lerp(event.getTickDelta(), ((AccessorCamera) camera).getLastCameraY(), ((AccessorCamera) camera).getCameraY());
            double x1 = playerEntity.getX() - playerPos.x();
            double y1 = playerEntity.getY() - playerPos.y() + eyeHeight;
            double z1 = playerEntity.getZ() - playerPos.z();
            float pitch = playerEntity.getXRot();
            float yaw = playerEntity.getYRot();
            if (FreecamModule.getInstance().isEnabled())
            {
                Vec3 pos1 = FreecamModule.getInstance().getCameraPosition();
                Vec3 pos2 = Interpolation.getRenderPosition(pos1, FreecamModule.getInstance().getLastCameraPosition(), event.getTickDelta());
                float rotations[] = FreecamModule.getInstance().getCameraRotations();
                x1 = pos1.x - pos2.x;
                y1 = pos1.y - pos2.y;
                z1 = pos1.z - pos2.z;
                yaw = rotations[0];
                pitch = rotations[1];
            }
            Vec3 pos = new Vec3(0.0, 0.0, 1.0)
                    .xRot(-(float) Math.toRadians(pitch))
                    .yRot(-(float) Math.toRadians(yaw))
                    .add(new Vec3(x1, y1, z1));
            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet())
            {
                BlockPos pos1 = entry.getKey();
                RenderManager.renderLine(event.getMatrices(), pos, pos1.getCenter(), widthConfig.getValue(), getColor(pos1, entry.getValue(), 255));
            }
            RenderBuffers.postRender();
            RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        }
        RenderBuffers.preRender();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet())
        {
            BlockPos pos1 = entry.getKey();
            VoxelShape outlineShape = entry.getValue().getShape(mc.level, pos1);
            if (outlineShape.isEmpty())
            {
                continue;
            }
            AABB render1 = outlineShape.bounds();
            AABB render = new AABB(pos1.getX() + render1.minX, pos1.getY() + render1.minY,
                    pos1.getZ() + render1.minZ, pos1.getX() + render1.maxX,
                    pos1.getY() + render1.maxY, pos1.getZ() + render1.maxZ);
            if (fillConfig.getValue())
            {
                RenderManager.renderBox(event.getMatrices(), render, getColor(pos1, entry.getValue(), 40));
            }
            RenderManager.renderBoundingBox(event.getMatrices(),
                    render, 1.5f, getColor(pos1, entry.getValue(), 145));
        }
        RenderBuffers.postRender();
    }

    private int getColor(BlockPos pos, BlockState state, int alpha)
    {
        int color = state.getMapColor(mc.level, pos).col;
        Block block = state.getBlock();
        if (block == Blocks.NETHER_PORTAL)
        {
            color = 0xff9307ff;
        }
        int r = FastColor.ARGB32.red(color);
        int g = FastColor.ARGB32.green(color);
        int b = FastColor.ARGB32.blue(color);
        return new Color(r, g, b, alpha).getRGB();
    }

    private boolean isSearchBlock(BlockState state)
    {
        return ((BlockListConfig) blocksConfig).contains(state.getBlock());
    }
}
