package net.shoreline.client.impl.module.render;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.ColorConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author linus
 * @since 1.0
 */
public class BreakHighlightModule extends ToggleModule
{
    Config<Float> rangeConfig = register(new NumberConfig<>("Range", "The range to render breaking blocks", 5.0f, 10.0f, 50.0f));
    Config<Color> colorConfig = register(new ColorConfig("Color", "The break highlight color", new Color(255, 0, 0), false, true));
    //
    private final Map<ClientboundBlockDestructionPacket, Long> breakingProgress = new ConcurrentHashMap<>();

    public BreakHighlightModule()
    {
        super("BreakHighlight", "Highlights blocks that are being broken", ModuleCategory.RENDER);
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet
                && !BlastResistantBlocks.isUnbreakable(packet.getPos()))
        {
            ClientboundBlockDestructionPacket p = getPacketFromPos(packet.getPos());
            if (p != null)
            {
                breakingProgress.replace(p, System.currentTimeMillis());
            }
            else
            {
                breakingProgress.put(packet, System.currentTimeMillis());
            }
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        RenderBuffers.preRender();
        for (Map.Entry<ClientboundBlockDestructionPacket, Long> mine : breakingProgress.entrySet())
        {
            BlockPos mining = mine.getKey().getPos();
            long elapsedTime = System.currentTimeMillis() - mine.getValue();
            long count = breakingProgress.keySet().stream().filter(p -> p.getId() == mine.getKey().getId()).count();
            while (count > 2)
            {
                breakingProgress.entrySet().stream().filter(p -> p.getKey().getId() == mine.getKey().getId())
                        .min(Comparator.comparingLong(Map.Entry::getValue)).ifPresent(min -> breakingProgress.remove(min.getKey(), min.getValue()));
                count--;
            }
            if (mc.level.isEmptyBlock(mining) || elapsedTime > 2500)
            {
                breakingProgress.remove(mine.getKey(), mine.getValue());
                continue;
            }
            double dist = mc.player.distanceToSqr(mining.getCenter());
            if (dist > ((NumberConfig) rangeConfig).getValueSq())
            {
                continue;
            }
            VoxelShape outlineShape = mc.level.getBlockState(mining).getShape(mc.level, mining);
            outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
            AABB render1 = outlineShape.bounds();
            AABB render = new AABB(mining.getX() + render1.minX, mining.getY() + render1.minY,
                    mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                    mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3 center = render.getCenter();
            float scale = Mth.clamp(elapsedTime / 2500.0f, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
            RenderManager.renderBox(event.getMatrices(), scaled, ((ColorConfig) colorConfig).getValue(40).getRGB());
            RenderManager.renderBoundingBox(event.getMatrices(), scaled, 1.5f, ((ColorConfig) colorConfig).getValue(100).getRGB());
        }
        RenderBuffers.postRender();
    }

    private ClientboundBlockDestructionPacket getPacketFromPos(BlockPos pos)
    {
        return breakingProgress.keySet().stream().filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
    }
}
