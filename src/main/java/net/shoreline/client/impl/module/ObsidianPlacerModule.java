package net.shoreline.client.impl.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.impl.module.combat.SurroundModule;

import java.util.*;

/**
 * @author linus
 * @see SurroundModule
 * @since 1.0
 */
public class ObsidianPlacerModule extends BlockPlacerModule
{
    protected static final BlockState DEFAULT_OBSIDIAN_STATE = Blocks.OBSIDIAN.defaultBlockState();
    // Blocks that can prevent explosion damage
    private static final List<Block> RESISTANT_BLOCKS = new LinkedList<>()
    {{
        add(Blocks.OBSIDIAN);
        add(Blocks.CRYING_OBSIDIAN);
        add(Blocks.ENDER_CHEST);
    }};

    public ObsidianPlacerModule(String name, String desc, ModuleCategory category)
    {
        super(name, desc, category);
    }

    public ObsidianPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority)
    {
        super(name, desc, category, rotationPriority);
    }

    /**
     * @return
     */
    protected net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot getResistantBlockItem()
    {
        final Set<net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot> blockSlots = new HashSet<>();
        for (final Block type : RESISTANT_BLOCKS)
        {
            final int slot = getBlockItemSlot(type);
            if (slot != -1)
            {
                blockSlots.add(new net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot(type, slot));
            }
        }

        // Prioritize
        net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot slot = blockSlots.stream().filter(b -> b.block() == Blocks.OBSIDIAN).findFirst().orElse(null);
        if (slot != null)
        {
            return slot;
        }
        net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot slot1 = blockSlots.stream().filter(b -> b.block() == Blocks.CRYING_OBSIDIAN).findFirst().orElse(null);
        if (slot1 != null)
        {
            return slot1;
        }
        net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot slot2 = blockSlots.stream().filter(b -> b.block() == Blocks.ENDER_CHEST).findFirst().orElse(null);
        if (slot2 != null)
        {
            return slot2;
        }
        return null;
    }

    public record BlockSlot(Block block, int slot)
    {
        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof net.shoreline.client.impl.module.ObsidianPlacerModule.BlockSlot b && b.block() == block;
        }
    }
}