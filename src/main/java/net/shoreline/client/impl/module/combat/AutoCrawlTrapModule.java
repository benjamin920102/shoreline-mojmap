package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.entity.EntityUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.*;

public class AutoCrawlTrapModule extends ObsidianPlacerModule
{
    private static AutoCrawlTrapModule INSTANCE;

    Config<Float> rangeConfig = register(new NumberConfig<>("PlaceRange", "The range to trap enemies", 0.1f, 4.0f, 6.0f));
    Config<Float> enemyRangeConfig = register(new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1f, 10.0f, 15.0f));
    Config<Boolean> downConfig = register(new BooleanConfig("PreventDownwards", "Prevents digging downwards", true));
    Config<Boolean> serverHitboxConfig = register(new BooleanConfig("HitboxSync", "Places on serverside crawling hitboxes", false));
    Config<Boolean> mineIgnoreConfig = register(new BooleanConfig("PreventMine", "Prevents enemies from mining the trap", false));
    Config<Integer> shiftTicksConfig = register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
    Config<Float> shiftDelayConfig = register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0f, 1.0f, 5.0f));
    Config<Integer> extrapolateTicksConfig = register(new NumberConfig<>("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Renders trap placements", false));
    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));

    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private Player target;
    private int blocksPlaced;

    public AutoCrawlTrapModule()
    {
        super("AutoCrawlTrap", "Automatically places blocks to keep enemies in crawl", ModuleCategory.COMBAT);
        INSTANCE = this;
    }

    public static AutoCrawlTrapModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        surround.clear();
        placements.clear();
        fadeList.clear();
        target = null;
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        blocksPlaced = 0;

        if (!multitaskConfig.getValue() && checkMultitask())
        {
            surround.clear();
            placements.clear();
            return;
        }

        net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot blockSlot = getResistantBlockItem();
        if (blockSlot == null) {
            return;
        }
        
        int slot = blockSlot.slot();
        if (slot == -1)
        {
            surround.clear();
            placements.clear();
            return;
        }
        target = getClosestPlayer(enemyRangeConfig.getValue());
        if (target == null)
        {
            surround.clear();
            placements.clear();
            return;
        }

        BlockPos targetPos = EntityUtil.getRoundedBlockPos(target);
        surround = getCrawlTrap(target, targetPos);
        if (!canCrawlTrap(target, targetPos) || surround.isEmpty())
        {
            return;
        }

        placements = getPlacementsFromTrap(surround);
        if (placements.isEmpty())
        {
            return;
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));
        while (blocksPlaced < shiftTicksConfig.getValue())
        {
            if (blocksPlaced >= placements.size())
            {
                break;
            }
            BlockPos targetPlacePos = placements.get(blocksPlaced);
            // All rotations for shift ticks must send extra packet
            // This may not work on all servers
            placeBlock(targetPlacePos, slot);
        }

        if (rotateConfig.getValue())
        {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        if (event.getPacket() instanceof ClientboundBundlePacket packet)
        {
            for (Packet<?> packet1 : packet.subPackets())
            {
                handlePackets(packet1);
            }
        }
        else
        {
            handlePackets(event.getPacket());
        }
    }

    private void handlePackets(Packet<?> serverPacket)
    {
        if (serverPacket instanceof ClientboundBlockUpdatePacket packet)
        {
            final BlockState blockState = packet.getBlockState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos))
            {
                if (blockState.canBeReplaced() && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, targetPos, CollisionContext.empty()))
                {
                     net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot blockSlot = getResistantBlockItem();
        if (blockSlot == null) {
            return;
        }
        
        int slot = blockSlot.slot();
                    if (slot == -1)
                    {
                        return;
                    }
                    placeBlock(targetPos, slot);
                }
                else if (BlastResistantBlocks.isBlastResistant(blockState))
                {
                    packets.remove(targetPos);
                }
            }
        }
    }

    private void placeBlock(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirectionConfig.getValue(), false, true, (state, angles) ->
        {
            if (rotateConfig.getValue() && state)
            {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
        blocksPlaced++;
    }

    public List<BlockPos> getPlacementsFromTrap(List<BlockPos> surround)
    {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround)
        {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.getValue() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.getValue() * 50.0f)
            {
                continue;
            }

            final AABB surroundBox = new AABB(surroundPos);
            List<Entity> invalid = mc.level.getEntities(null, surroundBox).stream().filter(e -> invalidEntity(e)).toList();
            boolean serverCrawling = invalid.stream().allMatch(e -> Managers.HITBOX.isServerCrawling(e)
                    && Managers.HITBOX.getCrawlingBoundingBox(e).intersects(surroundBox));

            if (!mc.level.getBlockState(surroundPos).canBeReplaced()
                    && !(serverCrawling && serverHitboxConfig.getValue())
                    && !(Managers.BLOCK.isPassed(surroundPos, 0.7f) && mineIgnoreConfig.getValue()))
            {
                continue;
            }
            double dist = mc.player.distanceToSqr(surroundPos.getCenter());
            if (dist > ((NumberConfig) rangeConfig).getValueSq())
            {
                continue;
            }

            if (mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, surroundPos, CollisionContext.empty()))
            {
                placements.add(surroundPos);
            }
        }

        return placements;
    }

    public List<BlockPos> getCrawlTrap(Player entity, BlockPos playerPos)
    {
        final List<BlockPos> crawlTrap = new ArrayList<>();
        crawlTrap.add(playerPos.above());
        if (downConfig.getValue())
        {
            crawlTrap.add(playerPos.below());
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        int ticks = 0;
        while (ticks <= extrapolateTicksConfig.getValue())
        {
            double ox = (x - entity.xo) * ticks;
            double oz = (z - entity.zo) * ticks;
            BlockPos blockPos = BlockPos.containing(x + ox, y, z + oz);
            if (!crawlTrap.contains(blockPos.above()))
            {
                crawlTrap.add(blockPos.above());
            }
            if (downConfig.getValue() && !crawlTrap.contains(blockPos.below()))
            {
                crawlTrap.add(blockPos.below());
            }
            ticks++;
        }
        return crawlTrap;
    }

    public boolean invalidEntity(Entity entity)
    {
        return !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof Arrow);
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (renderConfig.getValue())
        {
            RenderBuffers.preRender();
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
                Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
                RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
                RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5f, lineColor.getRGB());
            }
            RenderBuffers.postRender();

            if (placements.isEmpty())
            {
                return;
            }

            for (BlockPos pos : placements)
            {
                Animation animation = new Animation(true, fadeTimeConfig.getValue());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
                e.getValue().getFactor() == 0.0);
    }

    private boolean canCrawlTrap(Player player, BlockPos playerPos)
    {
        return player.onGround() || !mc.level.getBlockState(playerPos.above()).canBeReplaced() || !mc.level.getBlockState(playerPos.above(2)).canBeReplaced();
    }

    public boolean isPlacing()
    {
        return !placements.isEmpty();
    }
}
