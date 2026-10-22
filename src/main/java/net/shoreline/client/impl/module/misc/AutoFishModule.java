package net.shoreline.client.impl.module.misc;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.imixin.IMinecraftClient;
import net.shoreline.eventbus.annotation.EventListener;
import net.shoreline.eventbus.event.StageEvent;

/**
 * @author linus
 * @since 1.0
 */
public class AutoFishModule extends ToggleModule
{
    //
    Config<Boolean> openInventoryConfig = register(new BooleanConfig("OpenInventory", "Allows you to fish while in the inventory", true));
    Config<Integer> castDelayConfig = register(new NumberConfig<>("CastingDelay", "The delay between fishing rod casts", 10, 15, 25));
    Config<Float> maxSoundDistConfig = register(new NumberConfig<>("MaxSoundDist", "The maximum distance from the splash sound", 0.0f, 2.0f, 5.0f));
    //
    private boolean autoReel;
    private int autoReelTicks;
    //
    private int autoCastTicks;

    /**
     *
     */
    public AutoFishModule()
    {
        super("AutoFish", "Automatically casts and reels fishing rods",
                ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event)
    {
        if (mc.player == null)
        {
            return;
        }
        if (event.getPacket() instanceof ClientboundSoundPacket packet
                && packet.getSound().value() == SoundEvents.FISHING_BOBBER_SPLASH
                && mc.player.getMainHandItem().getItem() == Items.FISHING_ROD)
        {
            FishingHook fishHook = mc.player.fishing;
            if (fishHook == null || fishHook.getPlayerOwner() != mc.player)
            {
                return;
            }
            double dist = fishHook.distanceToSqr(packet.getX(),
                    packet.getY(), packet.getZ());
            if (dist <= maxSoundDistConfig.getValue())
            {
                autoReel = true;
                autoReelTicks = 4;
            }
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        if (event.getStage() != StageEvent.EventStage.PRE)
        {
            return;
        }
        if (mc.screen == null || mc.screen instanceof ChatScreen
                || openInventoryConfig.getValue())
        {
            if (mc.player.getMainHandItem().getItem() != Items.FISHING_ROD)
            {
                return;
            }
            FishingHook fishHook = mc.player.fishing;
            if ((fishHook == null || fishHook.getHookedIn() != null)
                    && autoCastTicks <= 0)
            {
                ((IMinecraftClient) mc).rightClick();
                autoCastTicks = castDelayConfig.getValue();
                return;
            }
            if (autoReel)
            {
                if (autoReelTicks <= 0)
                {
                    ((IMinecraftClient) mc).rightClick();
                    autoReel = false;
                    return;
                }
                autoReelTicks--;
            }
        }
        autoCastTicks--;
    }
}
