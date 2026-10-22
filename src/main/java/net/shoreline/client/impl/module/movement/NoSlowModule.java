package net.shoreline.client.impl.module.movement;

import net.minecraft.block.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.block.BlockSlipperinessEvent;
import net.shoreline.client.impl.event.block.SteppedOnSlimeBlockEvent;
import net.shoreline.client.impl.event.entity.SlowMovementEvent;
import net.shoreline.client.impl.event.entity.VelocityMultiplierEvent;
import net.shoreline.client.impl.event.network.*;
import net.shoreline.client.impl.module.exploit.DisablerModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorKeyBinding;
import net.shoreline.client.util.math.position.PositionUtil;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linus
 * @since 1.0
 */
public class NoSlowModule extends ToggleModule
{
    public static NoSlowModule INSTANCE;

    //
    Config<Boolean> strictConfig = register(new BooleanConfig("Strict", "Strict NCP bypass for ground slowdowns", false));
    Config<Boolean> airStrictConfig = register(new BooleanConfig("AirStrict", "Strict NCP bypass for air slowdowns", false));
    Config<Boolean> grimConfig = register(new BooleanConfig("Grim", "Strict Grim bypass for slowdown", false));
    Config<Boolean> grimNewConfig = register(new BooleanConfig("GrimV3", "Strict GrimV3 bypass for slowdown", false));
    Config<Boolean> strafeFixConfig = register(new BooleanConfig("StrafeFix", "Old NCP bypass for strafe", false));
    Config<Boolean> inventoryMoveConfig = register(new BooleanConfig("InventoryMove", "Allows the player to move while in inventories or screens", true));
    Config<Boolean> arrowMoveConfig = register(new BooleanConfig("ArrowMove", "Allows the player to look while in inventories or screens by using the arrow keys", false));
    Config<Boolean> itemsConfig = register(new BooleanConfig("Items", "Removes the slowdown effect caused by using items", true));
    Config<Boolean> sneakConfig = register(new BooleanConfig("Sneak", "Removes sneak slowdown", false));
    Config<Boolean> crawlConfig = register(new BooleanConfig("Crawl", "Removes crawl slowdown", false));
    Config<Boolean> shieldsConfig = register(new BooleanConfig("Shields", "Removes the slowdown effect caused by shields", true));
    Config<Boolean> websConfig = register(new BooleanConfig("Webs", "Removes the slowdown caused when moving through webs", false));
    Config<Boolean> berryBushConfig = register(new BooleanConfig("BerryBush", "Removes the slowdown caused when moving through webs", false));
    Config<Float> webSpeedConfig = register(new NumberConfig<>("WebMultiplier", "Speed to fall through webs", 0.00f, 1.00f, 1.00f, () -> websConfig.getValue() || berryBushConfig.getValue()));
    Config<Boolean> soulsandConfig = register(new BooleanConfig("SoulSand", "Removes the slowdown effect caused by walking over SoulSand blocks", false));
    Config<Boolean> honeyblockConfig = register(new BooleanConfig("HoneyBlock", "Removes the slowdown effect caused by walking over Honey blocks", false));
    Config<Boolean> slimeblockConfig = register(new BooleanConfig("SlimeBlock", "Removes the slowdown effect caused by walking over Slime blocks", false));
    //
    private boolean sneaking;
    //

    /**
     *
     */
    public NoSlowModule()
    {
        super("NoSlow", "Prevents items from slowing down player", ModuleCategory.MOVEMENT);
        INSTANCE = this;
    }

    public static NoSlowModule getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable()
    {
        if (airStrictConfig.getValue() && sneaking)
        {
            Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                    ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        }
        sneaking = false;
        Managers.TICK.setClientTick(1.0f);
    }

    @EventListener
    public void onSetCurrentHand(SetCurrentHandEvent event)
    {
        if (airStrictConfig.getValue() && !sneaking && checkSlowed())
        {
            sneaking = true;
            Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                    ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        }
    }

    @EventListener
    public void onPlayerUpdate(PlayerUpdateEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE && grimConfig.getValue()
                && mc.player.isUsingItem() && !mc.player.isShiftKeyDown() && itemsConfig.getValue())
        {

            // Grim focuses on other hand noslow checks
            if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND && checkStack(mc.player.getMainHandItem()))
            {
                Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            }
            else if (checkStack(mc.player.getOffhandItem()))
            {
                Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            }
        }
    }

    private boolean checkStack(ItemStack stack)
    {
        return !stack.getComponents().has(DataComponents.FOOD) && stack.getItem() != Items.BOW && stack.getItem() != Items.CROSSBOW && stack.getItem() != Items.SHIELD;
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() == StageEvent.EventStage.PRE)
        {
            if (airStrictConfig.getValue() && !mc.player.isUsingItem())
            {
                sneaking = false;
                Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                        ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
            }
            if (strafeFixConfig.getValue() && checkSlowed())
            {
                // Old NCP
//              Managers.NETWORK.sendSequencedPacket(id ->
//                      new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
//                              new BlockHitResult(mc.player.getPos(), Direction.UP, mc.player.getBlockPos(), false), id));
            }
            if (inventoryMoveConfig.getValue() && checkScreen())
            {
                final long handle = mc.getWindow().getWindow();
                KeyMapping[] keys = new KeyMapping[]{mc.options.keyJump, mc.options.keyUp, mc.options.keyDown, mc.options.keyRight, mc.options.keyLeft};
                for (KeyMapping binding : keys)
                {
                    binding.setDown(InputConstants.isKeyDown(handle, ((AccessorKeyBinding) binding).getBoundKey().getValue()));
                }
                if (arrowMoveConfig.getValue())
                {
                    float yaw = mc.player.getYRot();
                    float pitch = mc.player.getXRot();
                    if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_UP))
                    {
                        pitch -= 3.0f;
                    }
                    else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_DOWN))
                    {
                        pitch += 3.0f;
                    }
                    else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT))
                    {
                        yaw -= 3.0f;
                    }
                    else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT))
                    {
                        yaw += 3.0f;
                    }
                    mc.player.setYRot(yaw);
                    mc.player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
                }
            }

            if ((grimConfig.getValue() || grimNewConfig.getValue()) && websConfig.getValue())
            {
                AABB bb = grimConfig.getValue() ? mc.player.getBoundingBox().inflate(1.0) : mc.player.getBoundingBox();
                for (BlockPos pos : getIntersectingWebs(bb))
                {
                    Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
                    // Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                    //        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
                }
            }
        }
    }

    @EventListener
    public void onStrafeFix(StrafeFixEvent event)
    {
        if (strafeFixConfig.getValue())
        {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating())
            {
                yaw = Managers.ROTATION.getRotationYaw();
                pitch = Managers.ROTATION.getRotationPitch();
            }
            event.cancel();
            event.setYaw(yaw);
            event.setPitch(pitch);
        }
    }

    @EventListener
    public void onSlowMovement(SlowMovementEvent event)
    {
        Block block = event.getState().getBlock();
        if (block instanceof WebBlock && websConfig.getValue()
                || block instanceof SweetBerryBushBlock && berryBushConfig.getValue())
        {
            float multiplier = webSpeedConfig.getValue();
            if (webSpeedConfig.getValue() == 1.0f)
            {
                multiplier = 0.0f;
            }
            event.cancel();
            event.setMultiplier(multiplier);
        }
    }

    @EventListener
    public void onMovementSlowdown(MovementSlowdownEvent event)
    {
        if (sneakConfig.getValue() && mc.player.isShiftKeyDown() || crawlConfig.getValue() && mc.player.isVisuallyCrawling())
        {
            float f = 1.0f / (float) mc.player.getAttributeValue(Attributes.SNEAKING_SPEED);
            event.input.forwardImpulse *= f;
            event.input.leftImpulse *= f;
        }

        if (checkSlowed())
        {
            event.input.forwardImpulse *= 5.0f;
            event.input.leftImpulse *= 5.0f;
        }
    }

    @EventListener
    public void onVelocityMultiplier(VelocityMultiplierEvent event)
    {
        if (event.getBlock() == Blocks.SOUL_SAND && soulsandConfig.getValue()
                || event.getBlock() == Blocks.HONEY_BLOCK && honeyblockConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event)
    {
        if (slimeblockConfig.getValue())
        {
            event.cancel();
        }
    }

    @EventListener
    public void onBlockSlipperiness(BlockSlipperinessEvent event)
    {
        if (event.getBlock() == Blocks.SLIME_BLOCK
                && slimeblockConfig.getValue())
        {
            event.cancel();
            event.setSlipperiness(0.6f);
        }
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Outbound event)
    {
        if (mc.player == null || mc.level == null || mc.isLocalServer())
        {
            return;
        }
        else if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && packet.hasPosition()
                && strictConfig.getValue() && checkSlowed())
        {
            // Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(0));
            // Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id));
            Managers.INVENTORY.setSlotForced(mc.player.getInventory().selected);
        }
        else if (event.getPacket() instanceof ServerboundContainerClickPacket && strictConfig.getValue())
        {
            if (mc.player.isUsingItem())
            {
                mc.player.releaseUsingItem();
            }
            if (sneaking || Managers.POSITION.isSneaking())
            {
                Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                        ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
            }
            if (Managers.POSITION.isSprinting())
            {
                Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                        ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
        }
    }

    private boolean checkGrimNew()
    {
        return !mc.player.isShiftKeyDown() && !mc.player.isVisuallyCrawling() && !mc.player.isHandsBusy() &&
                mc.player.getUseItemRemainingTicks() < 5 || ((mc.player.getTicksUsingItem() > 1) && mc.player.getTicksUsingItem() % 2 != 0);
    }

    public boolean checkSlowed()
    {
//        ItemStack offHandStack = mc.player.getOffHandStack();
//        if ((offHandStack.isFood() || offHandStack.getItem() == Items.BOW || offHandStack.getItem() == Items.CROSSBOW || offHandStack.getItem() == Items.SHIELD) && grimConfig.getValue()) {
//            return false;
//        }
        if (DisablerModule.getInstance().grimFireworkCheck2())
        {
            return true;
        }
        if (!grimNewConfig.getValue() || checkGrimNew())
        {
            return !mc.player.isHandsBusy() && !mc.player.isShiftKeyDown() && (mc.player.isUsingItem() && itemsConfig.getValue()
                    || mc.player.isBlocking() && shieldsConfig.getValue() && !grimNewConfig.getValue() && !grimConfig.getValue());
        }
        return false;
    }

    public boolean checkScreen()
    {
        return mc.screen != null && !(mc.screen instanceof ChatScreen
                || mc.screen instanceof SignEditScreen || mc.screen instanceof DeathScreen);
    }

    public List<BlockPos> getIntersectingWebs(AABB boundingBox)
    {
        final List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos blockPos : PositionUtil.getAllInBox(boundingBox))
        {
            BlockState state = mc.level.getBlockState(blockPos);
            if (state.getBlock() instanceof WebBlock)
            {
                blocks.add(blockPos);
            }
        }
        return blocks;
    }

    public boolean getStrafeFix()
    {
        return strafeFixConfig.getValue();
    }
}
