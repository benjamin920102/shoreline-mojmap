package net.shoreline.client.impl.module.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.NumberDisplay;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.ItemUseEvent;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.module.client.ColorsModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorMinecraftClient;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author xgraza, linus
 * @since 1.0
 */
public final class AirPlaceModule extends ToggleModule
{
    private static AirPlaceModule INSTANCE;

    Config<Boolean> manualConfig = register(new BooleanConfig("Click", "Allow manual air place", true));
    Config<Boolean> grimConfig = register(new BooleanConfig("Grim", "Place on air on grim", false));
    Config<Float> rangeConfig = register(new NumberConfig<>("Range", "The range to air place", 1.0f, 4.0f, 6.0f, NumberDisplay.DEFAULT));
    Config<Boolean> fluidsConfig = register(new BooleanConfig("Fluids", "Place against fluids", false));

    private int airPlaceTicks;

    public AirPlaceModule()
    {
        super("AirPlace", "Allows you to place blocks in the air", ModuleCategory.WORLD);
        INSTANCE = this;
    }

    public static AirPlaceModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onEnable()
    {
        airPlaceTicks = 0;
    }

    @EventListener
    public void onPlayerTick(final TickEvent event)
    {
        if (airPlaceTicks > 0)
        {
            airPlaceTicks--;
        }

        if (mc.player == null || mc.gameMode == null || !manualConfig.getValue()
                || event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }

        if (mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos()))
        {
            return;
        }

        final ItemStack stack = mc.player.getMainHandItem();
        if ((stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) || !mc.options.keyUse.isDown())
        {
            return;
        }
        final HitResult result = mc.player.pick(rangeConfig.getValue(), 1.0f, fluidsConfig.getValue());
        if (((AccessorMinecraftClient) mc).hookGetItemUseCooldown() == 0 && airPlaceTicks == 0 && !mc.player.isUsingItem()
                && result instanceof BlockHitResult blockHitResult)
        {
            final BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
            if (!mc.level.isEmptyBlock(blockPos) || isEntityInBlockPos(blockPos))
            {
                return;
            }
            ((AccessorMinecraftClient) mc).hookSetItemUseCooldown(4);
            airPlaceTicks = 4;
            if (grimConfig.getValue())
            {
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, blockHitResult);
                mc.player.swing(InteractionHand.MAIN_HAND, false);
                Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            }
            else
            {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @EventListener
    public void onItemUse(final ItemUseEvent event)
    {
        if (airPlaceTicks > 0 && manualConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onRenderWorld(final RenderWorldEvent event)
    {
        if (mc.player == null || !manualConfig.getValue())
        {
            return;
        }

        if (mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos()))
        {
            return;
        }

        final ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
        {
            return;
        }
        final HitResult result = mc.player.pick(rangeConfig.getValue(), 1.0f, fluidsConfig.getValue());
        if (!(result instanceof BlockHitResult blockHitResult))
        {
            return;
        }
        final BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
        if (!mc.level.isEmptyBlock(blockPos) || isEntityInBlockPos(blockPos))
        {
            return;
        }
        RenderBuffers.preRender();
        RenderManager.renderBoundingBox(event.getMatrices(), blockPos, 1.5f, ColorsModule.getInstance().getRGB(145));
        RenderBuffers.postRender();
    }

    private boolean isEntityInBlockPos(final BlockPos blockPos)
    {
        for (Entity entity : mc.level.entitiesForRendering())
        {
            if (entity.getBoundingBox().intersects(new AABB(blockPos)))
            {
                return true;
            }
        }
        return false;
    }
}
