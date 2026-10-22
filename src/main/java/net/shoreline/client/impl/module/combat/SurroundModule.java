package net.shoreline.client.impl.module.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.event.network.PlayerTickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.ObsidianPlacerModule;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.impl.module.world.AirPlaceModule;
import net.shoreline.client.impl.module.world.AutoMineModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.client.util.render.animation.Animation;
import net.shoreline.client.util.world.BlastResistantBlocks;
import net.shoreline.eventbus.annotation.EventListener;

import java.awt.*;
import java.util.*;

/**
 * @author linus
 * @since 1.0
 */
public class SurroundModule extends ObsidianPlacerModule
{
    private static SurroundModule INSTANCE;

    Config<Timing> timingConfig = register(new EnumConfig<>("Timing", "Timing for replacing blocks", Timing.VANILLA, Timing.values()));
    Config<Boolean> prePlaceExplosionConfig = register(new BooleanConfig("PrePlace-Explosions", "Pre places before explosions", false, () -> timingConfig.getValue() == Timing.SEQUENTIAL));
    Config<Boolean> prePlaceTickConfig = register(new BooleanConfig("PrePlace-Tick", "Pre places before ticks", false, () -> timingConfig.getValue() == Timing.SEQUENTIAL));
    Config<Float> placeRangeConfig = register(new NumberConfig<>("PlaceRange", "The placement range for surround", 0.0f, 4.0f, 6.0f));
    Config<Boolean> attackConfig = register(new BooleanConfig("Attack", "Attacks crystals in the way of surround", true));
    Config<Boolean> extendConfig = register(new BooleanConfig("Extend", "Extends surround if the player is not in the center of a block", true));
    Config<Boolean> headConfig = register(new BooleanConfig("CoverHead", "Place a block at your head", false));
    Config<Boolean> mineExtendConfig = register(new BooleanConfig("MineExtend", "Extends surround if the block is being mined", false));
    Config<Boolean> supportConfig = register(new BooleanConfig("Support", "Creates a floor for the surround if there is none", false));
    Config<Integer> shiftTicksConfig = register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
    Config<Float> shiftDelayConfig = register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0f, 1.0f, 5.0f));
    Config<Boolean> jumpDisableConfig = register(new BooleanConfig("AutoDisable", "Disables after moving out of the hole", true));
    Config<Boolean> renderConfig = register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
    Config<Integer> fadeTimeConfig = register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));

    private int blocksPlaced;
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private double prevY;

    public SurroundModule()
    {
        super("Surround", "Surrounds feet with obsidian", ModuleCategory.COMBAT, 950);
        INSTANCE = this;
    }

    public static SurroundModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onEnable()
    {
        if (mc.player == null)
        {
            return;
        }
        prevY = mc.player.getY();
    }

    @Override
    public void onDisable()
    {
        surround.clear();
        placements.clear();
        packets.clear();
        fadeList.clear();
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        blocksPlaced = 0;
        if (SelfTrapModule.getInstance().isEnabled())
        {
            return;
        }

        if (jumpDisableConfig.getValue() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f))
        {
            disable();
            return;
        }

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

        surround = getSurround(mc.player);
        if (surround.isEmpty())
        {
            return;
        }

        if (attackConfig.getValue())
        {
            attackBlockingCrystals(surround);
        }
        placements = getPlacementsFromSurround(surround);
        if (placements.isEmpty())
        {
            return;
        }

        if (supportConfig.getValue())
        {
            for (BlockPos block : new ArrayList<>(placements))
            {
                if (block.getY() > mc.player.getBlockY() + 1.0)
                {
                    continue;
                }
                Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, strictDirectionConfig.getValue());
                if (direction == null)
                {
                    placements.add(block.below());
                }
            }
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));
        while (blocksPlaced < shiftTicksConfig.getValue())
        {
            if (blocksPlaced >= placements.size())
            {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            // All rotations for shift ticks must send extra packet
            // This may not work on all servers
            placeBlock(targetPos, slot);
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
        if (timingConfig.getValue() != Timing.SEQUENTIAL)
        {
            return;
        }

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

        if (blocksPlaced > shiftTicksConfig.getValue() * 2) // Give some leniency if we are getting place on
        {
            return;
        }

        if (serverPacket instanceof ClientboundExplodePacket packet && prePlaceExplosionConfig.getValue())
        {
            BlockPos pos = BlockPos.containing(packet.getX(), packet.getY(), packet.getZ());
            if (surround.contains(pos))
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
                placeBlock(pos, slot);
            }
        }

        if (serverPacket instanceof ClientboundAddEntityPacket packet
                && packet.getType().equals(EntityType.END_CRYSTAL) && prePlaceTickConfig.getValue())
        {
            for (BlockPos pos : surround)
            {
                if (!pos.equals(BlockPos.containing(packet.getX(), packet.getY(), packet.getZ())))
                {
                    continue;
                }

                 net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot blockSlot = getResistantBlockItem();
        if (blockSlot == null) {
            return;
        }
        
        int slot = blockSlot.slot();
                if (slot == -1)
                {
                    return;
                }
                placeBlock(pos, slot);
                break;
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

    public void attackBlockingCrystals(List<BlockPos> posList)
    {
        for (BlockPos pos : posList)
        {
            Entity crystalEntity = mc.level.getEntities(null, new AABB(pos)).stream()
                    .filter(e -> e instanceof EndCrystal).findFirst().orElse(null);
            if (crystalEntity == null)
            {
                continue;
            }
            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(crystalEntity, mc.player.isShiftKeyDown()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            return;
        }
    }

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround)
    {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround)
        {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.getValue() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.getValue() * 50.0f)
            {
                continue;
            }
            if (!mc.level.getBlockState(surroundPos).canBeReplaced())
            {
                continue;
            }
            double dist = mc.player.distanceToSqr(surroundPos.getCenter());
            if (dist > ((NumberConfig) placeRangeConfig).getValueSq())
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

    public List<BlockPos> getSurround(Player player)
    {
        List<BlockPos> surroundBlocks = getSurroundNoDown(player);
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos playerPos : playerBlocks)
        {
            if (playerPos.equals(player.blockPosition()))
            {
                continue;
            }
            surroundBlocks.add(playerPos.below());
        }
        if (mineExtendConfig.getValue())
        {
            for (BlockPos surroundPos : new ArrayList<>(surroundBlocks))
            {
                if (!Managers.BLOCK.isPassed(surroundPos, 0.7f))
                {
                    continue;
                }
                for (Direction direction : Direction.values())
                {
                    if (direction == Direction.DOWN)
                    {
                        continue;
                    }
                    BlockPos blockerPos = surroundPos.relative(direction);
                    if (playerBlocks.contains(blockerPos)
                            || AutoMineModule.getInstance().getMiningBlock() == blockerPos) // Dont want to help our opponent surround
                    {
                        continue;
                    }
                    surroundBlocks.add(blockerPos);
                }
            }
        }

        if (AirPlaceModule.getInstance().isEnabled() && headConfig.getValue())
        {
            surroundBlocks.add(mc.player.blockPosition().above(2));
        }
        return surroundBlocks;
    }

    public List<BlockPos> getSurroundNoDown(Player player)
    {
        return getSurroundNoDown(player, 0.0f);
    }

    public List<BlockPos> getSurroundNoDown(Player player, float range)
    {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos pos : playerBlocks)
        {
            if (range > 0.0f && mc.player.getEyePosition().distanceToSqr(pos.getCenter()) > range * range)
            {
                continue;
            }
            for (Direction dir : Direction.values())
            {
                if (!dir.getAxis().isHorizontal())
                {
                    continue;
                }
                BlockPos pos1 = pos.relative(dir);
                if (surroundBlocks.contains(pos1) || playerBlocks.contains(pos1))
                {
                    continue;
                }
                surroundBlocks.add(pos1);
            }
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(Player entity)
    {
        BlockPos playerPos = PositionUtil.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extendConfig.getValue())
        {
            playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
        }
        else
        {
            playerBlocks.add(playerPos);
        }
        return playerBlocks;
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event)
    {
        if (SelfTrapModule.getInstance().isEnabled())
        {
            return;
        }
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

    public boolean isPlacing()
    {
        return !placements.isEmpty();
    }

    public enum Timing
    {
        VANILLA,
        SEQUENTIAL
    }
}
